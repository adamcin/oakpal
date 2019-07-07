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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeTemplate;

import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface independent logic for exporting .cnd files from a JCR session.
 */
public final class CndExporter {

    public static final class Builder {
        private List<Rule> scopeExportNames;
        private List<Rule> scopeReplaceNames;
        private boolean includeBuiltins;

        public Builder withScopeExportNames(final List<Rule> scopeExportNames) {
            this.scopeExportNames = scopeExportNames;
            return this;
        }

        public Builder withScopeReplaceNames(final List<Rule> scopeReplaceNames) {
            this.scopeReplaceNames = scopeReplaceNames;
            return this;
        }

        /**
         * Determines whether to included Oak and JCR builtin nodetypes in the output.
         *
         * @param includeBuiltins false to exclude builtin nodetypes from the output (even when specified in onlyNames)
         * @return this builder
         */
        public Builder withIncludeBuiltins(final boolean includeBuiltins) {
            this.includeBuiltins = includeBuiltins;
            return this;
        }

        public CndExporter build() {
            return new CndExporter(scopeExportNames, scopeReplaceNames, includeBuiltins);
        }
    }

    private final List<Rule> scopeExportNames;
    private final List<Rule> scopeReplaceNames;
    private final boolean includeBuiltins;

    private CndExporter(final List<Rule> scopeExportNames,
                        final List<Rule> scopeReplaceNames,
                        final boolean includeBuiltins) {
        this.scopeExportNames = scopeExportNames != null ? scopeExportNames : Collections.emptyList();
        this.scopeReplaceNames = scopeReplaceNames != null ? scopeReplaceNames : Collections.emptyList();
        this.includeBuiltins = includeBuiltins;
    }

    public static final List<String> BUILTIN_NODETYPES = JsonCnd.BUILTIN_NODETYPES;

    /**
     * Function type that provides a Writer.
     */
    @FunctionalInterface
    public interface WriterOpener {
        @NotNull Writer open() throws IOException;
    }

    /**
     * Serialize the desired nodetypes as a CND exported from the provided Session to the provided cndFile.
     *
     * @param cndFile          a CND file to create or update with the desired JCR node type names.
     * @param session          the JCR session
     * @param desiredTypeNames the list of nodetype names to export, usually determined by scanning a FileVault archive for
     *                         jcr:primaryType and jcr:mixinTypes properties
     * @throws RepositoryException for missing nodetypes or any other repository exception
     * @throws IOException         for I/O errors
     * @throws ParseException      if the initialCnd is not a valid CND file.
     */
    public void writeNodetypes(final @NotNull File cndFile,
                               final @NotNull Session session,
                               final @NotNull List<String> desiredTypeNames)
            throws RepositoryException, IOException, ParseException {
        writeNodetypes(() -> new OutputStreamWriter(new FileOutputStream(cndFile), StandardCharsets.UTF_8),
                session, desiredTypeNames, cndFile);
    }

    /**
     * Serialize the desired nodetypes as a CND to the provided stream writer.
     *
     * @param writerOpener     an opener function that provides the desired output stream.
     * @param session          the JCR session
     * @param desiredTypeNames the list of nodetype names to export, usually determined by scanning a FileVault archive for
     *                         jcr:primaryType and jcr:mixinTypes properties
     * @param initialCnd       an optional file providing a CND with an initial list of nodetype definitions. This will be
     *                         read fully before the {@code writerOpener} is opened, so it is safe to read and write to the
     *                         same file, in order to update its contents according to the rules of this exporter.
     * @throws RepositoryException for missing nodetypes or any other repository exception
     * @throws IOException         for I/O errors
     * @throws ParseException      if the initialCnd is not a valid CND file.
     */
    public void writeNodetypes(final @NotNull WriterOpener writerOpener,
                               final @NotNull Session session,
                               final @NotNull List<String> desiredTypeNames,
                               final @Nullable File initialCnd)
            throws RepositoryException, IOException, ParseException {

        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final TemplateBuilderFactory defFactory = new TemplateBuilderFactory(session);

        final Function<String, Name> mapper = Fun.tryOrDefault1(resolver::getQName, null);
        final Function<Name, String> qualifier = Fun.tryOrDefault1(resolver::getJCRName, null);

        final Set<Name> exportTypeNames = new HashSet<>();
        desiredTypeNames.stream().map(mapper).forEachOrdered(exportTypeNames::add);

        final Map<Name, NodeTypeDefinition> writableTypes = new LinkedHashMap<>();
        if (initialCnd != null && initialCnd.isFile()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(initialCnd), StandardCharsets.UTF_8)) {
                CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry> ntReader
                        = new CompactNodeTypeDefReader<>(reader, initialCnd.toURI().toString(), defFactory);

                for (NodeTypeTemplate def : ntReader.getNodeTypeDefinitions()) {
                    Name defName = mapper.apply(def.getName());
                    writableTypes.put(defName, def);
                    ntDepStream(def).map(mapper).forEachOrdered(exportTypeNames::add);
                }
            }
        }

        final Set<Name> builtinNodetypes = BUILTIN_NODETYPES.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());


        final Function<String, String> normalizer = qualifier.compose(mapper);

        final Predicate<NodeTypeDefinition> scopeExportFilter = Fun
                .composeTest(normalizer.compose(NodeTypeDefinition::getName),
                        (name -> Rule.lastMatch(scopeExportNames, name).isInclude()));

        final Predicate<NodeTypeDefinition> scopeReplaceFilter = Fun
                .composeTest(normalizer.compose(NodeTypeDefinition::getName),
                        (name -> Rule.lastMatch(scopeReplaceNames, name).isInclude()));

        final Predicate<NodeTypeDefinition> addOrReplaceFilter = Fun
                .composeTest(mapper.compose(NodeTypeDefinition::getName),
                        writableTypes::containsKey)
                .negate().or(scopeReplaceFilter);

        final Predicate<NodeTypeDefinition> builtinFilter =
                Fun.composeTest(mapper.compose(NodeTypeDefinition::getName),
                        includeBuiltins ? name -> true : ((Predicate<Name>) builtinNodetypes::contains).negate());

        // export if name
        // 1. matches scopeExportNames
        // 2. includeBuiltins is true or name is not in builtinNodetypes
        // 3. not in writeableTypes already OR matches scopeReplaceNames
        final Predicate<NodeTypeDefinition> includeFilter = builtinFilter.and(scopeExportFilter).and(addOrReplaceFilter);

        final Map<Name, NodeTypeDefinition> exportedTypes = retrieveNodeTypes(session, exportTypeNames);

        exportedTypes.values().stream().filter(includeFilter).forEachOrdered(type ->
                writableTypes.put(mapper.apply(type.getName()), type));

        try (Writer writer = writerOpener.open()) {
            final CompactNodeTypeDefWriter cndWriter = new CompactNodeTypeDefWriter(writer, session, true);
            try {
                for (NodeTypeDefinition def : writableTypes.values()) {
                    cndWriter.write(def);
                }
            } finally {
                cndWriter.close();
            }
        }
    }

    /**
     * Simple method to recursively retrieve types from the provided session.
     *
     * @param session          the JCR session to retrieve node types from.
     * @param desiredTypeNames the qualified names of node types to retrieve
     * @return a map of qualified names to exported node types.
     * @throws RepositoryException when session throws
     */
    public static Map<Name, NodeTypeDefinition> retrieveNodeTypes(final @NotNull Session session,
                                                                  final @NotNull Collection<Name> desiredTypeNames)
            throws RepositoryException {
        return retrieveNodeTypes(session, desiredTypeNames, null);
    }

    /**
     * Simple method to recursively retrieve types from the provided session.
     *
     * @param session          the JCR session to retrieve node types from.
     * @param desiredTypeNames the qualified names of node types to retrieve
     * @param nodeTypeSelector an optional predicate that selects nodetypes for export in addition to the explicit list
     *                         of desiredTypeNames
     * @return a map of qualified names to exported node types.
     * @throws RepositoryException when session throws
     */
    public static Map<Name, NodeTypeDefinition>
    retrieveNodeTypes(final @NotNull Session session,
                      final @NotNull Collection<Name> desiredTypeNames,
                      final @Nullable BiPredicate<NamePathResolver, NodeType> nodeTypeSelector)
            throws RepositoryException {
        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final Map<Name, NodeType> allTypes = new LinkedHashMap<>();
        final Set<Name> exportableTypeNames = new LinkedHashSet<>(desiredTypeNames);
        final BiPredicate<NamePathResolver, NodeType> _selector = nodeTypeSelector != null
                ? nodeTypeSelector
                : (res, type) -> false;

        final NodeTypeIterator iter = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();

        while (iter.hasNext()) {
            NodeType type = iter.nextNodeType();
            final Name name = resolver.getQName(type.getName());
            allTypes.put(name, type);
            if (_selector.test(resolver, type)) {
                exportableTypeNames.add(name);
            }
        }

        final Map<Name, NodeTypeDefinition> exportedTypes = new LinkedHashMap<>();
        for (Name qName : exportableTypeNames) {
            if (allTypes.containsKey(qName)) {
                final NodeType def = allTypes.get(qName);
                addType(resolver, exportedTypes, def);
            } else {
                throw new NoSuchNodeTypeException("Failed to find nodetype with name: " + qName);
            }
        }

        return exportedTypes;
    }

    static <T> Stream<T> optStream(final T element) {
        return Optional.ofNullable(element).map(Stream::of).orElse(Stream.empty());
    }

    @SuppressWarnings("unchecked")
    static Stream<String> ntDepStream(final NodeTypeTemplate ntDef) {
        final Stream<String> superTypes = optStream(ntDef.getDeclaredSupertypeNames()).flatMap(Stream::of);
        final Stream<String> childTypes = ((Stream<NodeDefinitionTemplate>) ntDef.getNodeDefinitionTemplates().stream())
                .flatMap(nodeDef -> {
                    return Stream.concat(
                            optStream(nodeDef.getDefaultPrimaryTypeName()),
                            optStream(nodeDef.getRequiredPrimaryTypeNames()).flatMap(Stream::of));
                });
        return Stream.concat(superTypes, childTypes);
    }

    static void addType(final @NotNull NamePathResolver resolver,
                        final @NotNull Map<Name, NodeTypeDefinition> typeSet,
                        final @NotNull NodeType def)
            throws RepositoryException {
        final Name name = resolver.getQName(def.getName());
        if (typeSet.containsKey(name)) {
            return;
        }
        final Consumer<NodeType> accum = Fun.uncheckVoid1(nt -> addType(resolver, typeSet, nt));
        // first add super types
        optStream(def.getDeclaredSupertypes()).flatMap(Stream::of).forEachOrdered(accum);
        // then add this type
        typeSet.put(name, def);
        // then add types declared in node defs
        optStream(def.getDeclaredChildNodeDefinitions())
                .flatMap(Stream::of)
                .flatMap(childDef -> {
                    return Stream.concat(
                            optStream(childDef.getDefaultPrimaryType()),
                            optStream(childDef.getRequiredPrimaryTypes()).flatMap(Stream::of));
                })
                .forEachOrdered(accum);
    }

}
