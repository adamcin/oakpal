/*
 * Copyright 2019 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.webster;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.api.Result;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.spi.CNDReader;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.apache.jackrabbit.vault.fs.spi.PrivilegeDefinitions;
import org.apache.jackrabbit.vault.fs.spi.ServiceProviderFactory;
import org.apache.jackrabbit.vault.util.DocViewNode;
import org.apache.jackrabbit.vault.util.DocViewProperty;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.RejectingEntityDefaultHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.jcr.NamespaceException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.api.Fun.uncheckVoid1;
import static net.adamcin.oakpal.core.JsonCnd.BUILTIN_MAPPINGS;

public final class FileVaultNameFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVaultNameFinder.class);

    private final Set<QName> references = new LinkedHashSet<>();
    private final Set<QName> definitions = new LinkedHashSet<>();

    public FileVaultNameFinder() {
        loadBuiltins();
    }

    void loadBuiltins() {
        JsonCnd.BUILTIN_PRIVILEGES.stream()
                .map(result1(jcrName -> QName.parseQName(BUILTIN_MAPPINGS, QName.Type.PRIVILEGE, jcrName)))
                .flatMap(Result::stream)
                .forEachOrdered(this::addDefinition);

        JsonCnd.BUILTIN_NODETYPES.stream()
                .map(result1(jcrName -> QName.parseQName(BUILTIN_MAPPINGS, QName.Type.NODETYPE, jcrName)))
                .flatMap(Result::stream)
                .forEachOrdered(this::addDefinition);
    }

    void addReference(final QName qName) {
        LOGGER.trace("[QName#addReference] qName={}", qName);
        this.references.add(qName);
    }

    void addDefinition(final QName qName) {
        LOGGER.trace("[QName#addDefinition] qName={}", qName);
        this.definitions.add(qName);
    }

    void collectPrivilegeDefinition(final @NotNull NamespaceMapping mapping,
                                    final @NotNull PrivilegeDefinition def) {
        final Function<Name, Result<QName>> adapterFn =
                result1(name -> QName.adaptName(mapping, QName.Type.PRIVILEGE, name));
        adapterFn.apply(def.getName()).forEach(this::addDefinition);
        Name defName = def.getName();
        addDefinition(QName.adaptName(mapping, QName.Type.PRIVILEGE, defName));
        def.getDeclaredAggregateNames().stream()
                .map(adapterFn)
                .flatMap(Result::stream)
                .forEachOrdered(this::addReference);
    }

    void collectNodeTypeNames(final NamespaceMapping mapping, final QNodeTypeDefinition def) {
        final Function<Name, QName> typeNameTransform = name -> QName.adaptName(mapping, QName.Type.NODETYPE, def.getName());
        ((Function<QName, Optional<QName>>) Optional::ofNullable)
                .compose(name -> QName.adaptName(mapping, QName.Type.NODETYPE, def.getName()))
                .apply(def.getName()).ifPresent(this::addDefinition);

        def.getDependencies().stream().map(typeNameTransform).filter(Objects::nonNull).forEachOrdered(this::addReference);
    }

    public Set<QName> search(final Archive archive) throws Exception {
        archive.open(false);
        PrivilegeDefinitions defs = archive.getMetaInf().getPrivileges();
        if (defs != null && !defs.getDefinitions().isEmpty()) {
            NamespaceMapping mapping = defs.getNamespaceMapping();
            defs.getDefinitions().forEach(def -> collectPrivilegeDefinition(mapping, def));
        }

        Collection<NodeTypeSet> nodeTypeSets = archive.getMetaInf().getNodeTypes();
        for (NodeTypeSet nodeTypeSet : nodeTypeSets) {
            NamespaceMapping names = nodeTypeSet.getNamespaceMapping();
            nodeTypeSet.getNodeTypes().values().forEach(def -> collectNodeTypeNames(names, def));
        }

        Archive.Entry rootEntry = archive.getJcrRoot();

        if (rootEntry != null) {
            this.search(archive, rootEntry);
        }

        Set<QName> subtracted = new LinkedHashSet<>();

        references.stream()
                .filter(((Predicate<QName>) definitions::contains).negate())
                .forEachOrdered(subtracted::add);

        return subtracted;
    }

    void search(final Archive archive, final Archive.Entry entry)
            throws IOException {
        if (entry.isDirectory()) {
            for (Archive.Entry child : entry.getChildren()) {
                search(archive, child);
            }
        } else {
            String fileName = entry.getName();
            String repoName = PlatformNameFormat.getRepositoryName(fileName);
            String ext = "";
            int idx = repoName.lastIndexOf('.');
            if (idx > 0) {
                ext = repoName.substring(idx);
            }

            if (".xml".equals(ext)) {
                Optional.ofNullable(archive.getInputSource(entry))
                        .ifPresent(uncheckVoid1((this::handleDocView)));
            } else if (".cnd".equals(ext)) {
                Optional.ofNullable(archive.getInputSource(entry))
                        .ifPresent(uncheckVoid1(is -> {
                            try (InputStream input = is.getByteStream();
                                 Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                                CNDReader cndReader = ServiceProviderFactory.getProvider().getCNDReader();
                                // provide session namespaces
                                cndReader.read(reader, is.getSystemId(), null);
                                NamespaceMapping names = cndReader.getNamespaceMapping();
                                cndReader.getNodeTypes().values().forEach(def -> collectNodeTypeNames(names, def));
                            }
                        }));
            }
        }
    }

    void handleDocView(final @NotNull VaultInputSource source) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        parser.parse(source, handler);
    }

    static final class NsStack {

        /**
         * Next NameSpace mapping for this prefix on the stack.
         */
        final NsStack next;

        /**
         * Prefix of this NameSpace element.
         */
        final NamespaceMapping mapping;

        /**
         * Construct a namespace for placement on the
         * result tree namespace stack.
         *
         * @param mapping the namespace mapping associated with this level of the stack
         * @param next    the next element in the stack
         */
        NsStack(@NotNull NamespaceMapping mapping, @Nullable NsStack next) {
            this.mapping = mapping;
            this.next = next;
        }
    }

    final class Handler extends RejectingEntityDefaultHandler implements NamespaceResolver {
        boolean expectStartElement = false;
        boolean expectEndPrefixMapping = false;
        NamespaceMapping mapping = new NamespaceMapping();
        NsStack mappingStack = new NsStack(mapping, null);

        final NamePathResolver npResolver = new DefaultNamePathResolver(this);
        Boolean isDocView = null;

        @Override
        public void startPrefixMapping(final @NotNull String prefix, final @NotNull String uri) throws SAXException {
            if (!expectStartElement) {
                mapping = new NamespaceMapping(mapping);
                mappingStack = new NsStack(mapping, mappingStack);
                expectStartElement = true;
                expectEndPrefixMapping = true;
            }
            setMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(final @NotNull String prefix) throws SAXException {
            if (expectEndPrefixMapping) {
                if (mappingStack.next == null) {
                    throw new IllegalStateException("namespace mapping stack is out of sync");
                }
                mappingStack = mappingStack.next;
                mapping = mappingStack.mapping;
                expectEndPrefixMapping = false;
            }
        }

        final void setMapping(final String prefix, final String uri) throws SAXException {
            try {
                mapping.setMapping(prefix, uri);
            } catch (NamespaceException e) {
                throw new SAXException(e);
            }
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            expectStartElement = false;
            LOGGER.trace("[Handler#startElement] uri={} localName={} qName={} attributes={}", uri, localName, qName, attributes);
            if (isDocView == null) {
                isDocView = "jcr:root".equals(qName);
            }

            if (!isDocView) {
                return;
            }

            String label = ISO9075.decode(qName);
            String name = nameFromLabel(label);
            if (attributes.getLength() > 0) {
                DocViewNode ni = createNode(name, label, attributes);
                if (ni.primary != null) {
                    Fun.result0(() -> QName.parseQName(mapping, QName.Type.NODETYPE, ni.primary)).get()
                            .forEach(FileVaultNameFinder.this::addReference);
                }
                if (ni.mixins != null) {
                    Stream.of(ni.mixins).map(result1(type -> QName.parseQName(mapping, QName.Type.NODETYPE, type)))
                            .flatMap(Result::stream).forEachOrdered(FileVaultNameFinder.this::addReference);
                }
                if (ni.props.containsKey("rep:privileges")) {
                    DocViewProperty prop = ni.props.get("rep:privileges");
                    if (prop.values != null
                            && (prop.type == UNDEFINED || prop.type == STRING || prop.type == NAME)) {
                        Stream.of(prop.values).map(result1(type -> QName.parseQName(mapping, QName.Type.PRIVILEGE, type)))
                                .flatMap(Result::stream).forEachOrdered(FileVaultNameFinder.this::addReference);
                    }
                }
            }
        }

        String nameFromLabel(final @NotNull String label) {
            int idx = label.lastIndexOf('[');
            if (idx > 0) {
                return label.substring(0, idx);
            } else {
                return label;
            }
        }

        DocViewNode createNode(final String name, final String label, final Attributes attributes) throws SAXException {
            try {
                return new DocViewNode(name, label, attributes, npResolver);
            } catch (NamespaceException e) {
                throw new SAXException(e);
            }
        }

        @Override
        public String getURI(final String prefix) throws NamespaceException {
            return mapping.getURI(prefix);
        }

        @Override
        public String getPrefix(final String uri) throws NamespaceException {
            return mapping.getPrefix(uri);
        }
    }
}
