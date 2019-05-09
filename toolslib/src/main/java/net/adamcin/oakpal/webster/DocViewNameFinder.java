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
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.oak.spi.namespace.NamespaceConstants;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.vault.fs.api.SerializationType;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.impl.io.XmlAnalyzer;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.spi.CNDReader;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.apache.jackrabbit.vault.fs.spi.PrivilegeDefinitions;
import org.apache.jackrabbit.vault.fs.spi.ServiceProviderFactory;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.RejectingEntityDefaultHandler;
import org.xml.sax.SAXException;

public final class DocViewNameFinder {

    public enum NameType {
        PRIVILEGE, NODETYPE
    }

    public static class QName {
        private final NameType type;
        private final String prefix;
        private final String localName;
        private final String uri;

        public QName(final NameType type,
                     final String prefix,
                     final String localName,
                     final String uri) {
            this.type = type;
            this.prefix = prefix;
            this.localName = localName;
            this.uri = uri;
        }

        public NameType getType() {
            return type;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getLocalName() {
            return localName;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QName qName = (QName) o;
            return type == qName.type &&
                    localName.equals(qName.localName) &&
                    uri.equals(qName.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, localName, uri);
        }
    }

    private final NamespaceMapping builtinMappings = new NamespaceMapping();
    private final Set<QName> references = new LinkedHashSet<>();
    private final Set<QName> definitions = new LinkedHashSet<>();

    void loadBuiltins() throws Exception {
        builtinMappings.setMapping(NamespaceRegistry.PREFIX_EMPTY,
                NamespaceRegistry.NAMESPACE_EMPTY);
        builtinMappings.setMapping(NamespaceRegistry.PREFIX_JCR,
                NamespaceRegistry.NAMESPACE_JCR);
        builtinMappings.setMapping(NamespaceRegistry.PREFIX_NT,
                NamespaceRegistry.NAMESPACE_NT);
        builtinMappings.setMapping(NamespaceRegistry.PREFIX_XML,
                NamespaceRegistry.NAMESPACE_XML);
        builtinMappings.setMapping(NamespaceRegistry.PREFIX_MIX,
                NamespaceRegistry.NAMESPACE_MIX);
        builtinMappings.setMapping(NamespaceConstants.PREFIX_REP,
                NamespaceConstants.NAMESPACE_REP);
        builtinMappings.setMapping(NamespaceConstants.PREFIX_SV,
                NamespaceConstants.NAMESPACE_SV);
        builtinMappings.setMapping(NamespaceConstants.PREFIX_XMLNS,
                NamespaceConstants.NAMESPACE_XMLNS);
        builtinMappings.setMapping("oak", "http://jackrabbit.apache.org/oak/ns/1.0");

        for (String name : PrivilegeXmlExporter.BUILTIN_PRIVILEGES) {
            Optional.ofNullable(parseQName(builtinMappings, NameType.PRIVILEGE, name)).ifPresent(definitions::add);
        }

        for (String name : CndExporter.BUILTIN_NODETYPES) {
            Optional.ofNullable(parseQName(builtinMappings, NameType.NODETYPE, name)).ifPresent(definitions::add);
        }
    }

    QName parseQName(final NamespaceMapping mapping, final NameType type, final String qName) throws Exception {
        if (qName.contains(":")) {
            final String[] parts = qName.split(":", 2);
            final String prefix = parts[0];
            final String localName = parts[1];
            if (mapping.hasPrefix(prefix)) {
                return new QName(type, prefix, localName, mapping.getURI(prefix));
            }
        }
        return null;
    }

    QName adaptName(final NamespaceMapping mapping, final NameType type, final Name name) throws Exception {
        return new QName(type,
                mapping.getPrefix(name.getNamespaceURI()),
                name.getLocalName(),
                name.getNamespaceURI());
    }

    void collectPrivilegeDefinition(final NamespaceMapping mapping, final PrivilegeDefinition def) {
        try {
            Name defName = def.getName();
            definitions.add(adaptName(mapping, NameType.PRIVILEGE, defName));
            def.getDeclaredAggregateNames().stream()
                    .map(FunUtil.tryOrDefault(name -> adaptName(mapping, NameType.PRIVILEGE, name), null))
                    .filter(Objects::nonNull)
                    .forEachOrdered(references::add);
        } catch (final Exception e) {
            // do nothing
        }
    }

    void collectNodeTypeNames(final NamespaceMapping mapping, final QNodeTypeDefinition def) {
        final Function<Name, QName> typeNameTransform = FunUtil
                .tryOrDefault(name -> adaptName(mapping, NameType.NODETYPE, def.getName()), null);
        Optional<QName> qname = FunUtil
                .tryOrOptional(name -> adaptName(mapping, NameType.NODETYPE, def.getName()))
                .apply(def.getName());
        qname.ifPresent(definitions::add);

        def.getDependencies().stream().map(typeNameTransform).filter(Objects::nonNull).forEachOrdered(references::add);
    }

    public Set<QName> search(final Archive archive) throws IOException {
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
            String repoBase = repoName;
            String ext = "";
            int idx = repoName.lastIndexOf('.');
            if (idx > 0) {
                repoBase = repoName.substring(0, idx);
                ext = repoName.substring(idx);
            }

            if (".xml".equals(ext)) {
                VaultInputSource is = archive.getInputSource(entry);
                if (is != null && XmlAnalyzer.analyze(is) == SerializationType.XML_DOCVIEW) {

                }
            } else if (".cnd".equals(ext)) {
                VaultInputSource is = archive.getInputSource(entry);
                if (is != null) {
                    try (InputStream input = is.getByteStream();
                         Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {

                        CNDReader cndReader = ServiceProviderFactory.getProvider().getCNDReader();
                        // provide session namespaces
                        cndReader.read(reader, is.getSystemId(), null);
                        NamespaceMapping names = cndReader.getNamespaceMapping();
                        cndReader.getNodeTypes().values().forEach(def -> collectNodeTypeNames(names, def));
                    }
                }
            }
        }
    }

    class NsStack {

        /**
         * Next NameSpace element on the stack.
         */
        public NsStack next = null;

        /**
         * Prefix of this NameSpace element.
         */
        public String prefix;

        /**
         * Namespace URI of this NameSpace element.
         */
        public String uri;  // if null, then Element namespace is empty.

        /**
         * Construct a namespace for placement on the
         * result tree namespace stack.
         *
         * @param prefix Prefix of this element
         * @param uri    URI of  this element
         */
        public NsStack(String prefix, String uri) {
            this.prefix = prefix;
            this.uri = uri;
        }
    }

    class DocViewNameCheckHandler extends RejectingEntityDefaultHandler implements NamespaceResolver {

        final NamespaceMapping mapping = new NamespaceMapping();
        NsStack nsStack = null;

        public DocViewNameCheckHandler() {
        }

        @Override
        public String getURI(final String prefix) throws NamespaceException {
            return mapping.getURI(prefix);
        }

        @Override
        public String getPrefix(final String uri) throws NamespaceException {
            return mapping.getPrefix(uri);
        }

        @Override
        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
            NsStack ns = new NsStack(prefix, uri);
            // push on stack
            ns.next = nsStack;
            nsStack = ns;
            // check if uri is already registered
            String oldPrefix = null;
            try {
                oldPrefix = mapping.getPrefix(uri);
            } catch (NamespaceException e) {
                // assume uri never registered
                try {
                    mapping.setMapping(prefix, uri);
                    oldPrefix = prefix;
                } catch (NamespaceException e1) {
                    throw new SAXException(e);
                }
            }
            // update mapping
            if (!prefix.equals(oldPrefix)) {
                try {
                    mapping.setMapping(prefix, uri);
                } catch (NamespaceException e) {
                    throw new SAXException(e);
                }
            }
        }

        @Override
        public void endPrefixMapping(final String prefix) throws SAXException {
            NsStack ns = nsStack;
            NsStack prev = null;
            while (ns != null && !ns.prefix.equals(prefix)) {
                prev = ns;
                ns = ns.next;
            }
            if (ns == null) {
                throw new SAXException("Illegal state: prefix " + prefix + " never mapped.");
            }
            // remove from stack
            if (prev == null) {
                nsStack = ns.next;
            } else {
                prev.next = ns.next;
            }
            // find old prefix
            ns = ns.next;
            while (ns != null && !ns.prefix.equals(prefix)) {
                ns = ns.next;
            }
            // update mapping
            if (ns != null) {
                try {
                    mapping.setMapping(prefix, ns.uri);
                } catch (NamespaceException e) {
                    throw new SAXException(e);
                }
            }
        }
    }

    void handleDocView(final VaultInputSource source) throws ParserConfigurationException, SAXException, IOException {
        if (source == null) {
            return;
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        SAXParser parser = factory.newSAXParser();
        DocViewNameCheckHandler handler = new DocViewNameCheckHandler();
        parser.parse(source, handler);

    }


}
