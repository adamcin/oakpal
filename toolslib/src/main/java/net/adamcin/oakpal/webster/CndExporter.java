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
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.InitialContent;
import org.apache.jackrabbit.oak.spi.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefWriter;

/**
 * Interface independent logic for exporting .cnd files from a JCR session.
 */
public final class CndExporter {

    private CndExporter() {
        // no construction
    }

    static final List<String> BUILTIN_NODETYPES = StreamSupport.stream(
            InitialContent.INITIAL_CONTENT
                    .getChildNode(JcrConstants.JCR_SYSTEM)
                    .getChildNode(NodeTypeConstants.JCR_NODE_TYPES)
                    .getChildNodeNames().spliterator(), false).collect(Collectors.toList());

    /**
     * Serialize the desired nodetypes as a CND to the provided stream writer.
     *
     * @param writer          the desired output stream.
     * @param session         the JCR session
     * @param onlyNames       the list of nodetype names to export
     * @param includeBuiltins false to exclude builtin nodetypes from the output (even when specified in onlyNames)
     * @throws RepositoryException for missing nodetypes or any other repository exception
     * @throws IOException         for I/O errors
     */
    public static void writeNodetypes(final Writer writer,
                                      final Session session,
                                      final List<String> onlyNames,
                                      final boolean includeBuiltins)
            throws RepositoryException, IOException {


        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final Function<String, Name> mapper = FunUtil.tryOrDefault(resolver::getQName, null);

        final Set<Name> builtinNodetypes = BUILTIN_NODETYPES.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Predicate<NodeType> builtinFilter =
                FunUtil.fundicate(mapper.compose(NodeType::getName),
                        includeBuiltins ? name -> true : ((Predicate<Name>) builtinNodetypes::contains).negate());

        final Map<Name, NodeType> allTypes = new LinkedHashMap<>();
        final NodeTypeIterator iter = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();

        while (iter.hasNext()) {
            NodeType type = iter.nextNodeType();
            allTypes.put(mapper.apply(type.getName()), type);
        }

        final Set<NodeType> types = new LinkedHashSet<>();
        if (onlyNames == null || onlyNames.isEmpty()) {
            types.addAll(allTypes.values().stream().filter(builtinFilter).collect(Collectors.toSet()));
        } else {
            for (String typeName : onlyNames) {
                final Name qName = mapper.apply(typeName);
                if (qName != null && allTypes.containsKey(qName)) {
                    final NodeType def = allTypes.get(qName);
                    addType(types, def, builtinFilter);
                } else {
                    throw new NoSuchNodeTypeException("Failed to find nodetype with name: " + typeName);
                }
            }
        }

        final CompactNodeTypeDefWriter cndWriter = new CompactNodeTypeDefWriter(writer, session, true);
        try {
            for (NodeTypeDefinition def : types) {
                cndWriter.write(def);
            }
        } finally {
            cndWriter.close();
        }
    }

    static <T> Stream<T> optStream(final T element) {
        return Optional.ofNullable(element).map(Stream::of).orElse(Stream.empty());
    }

    static void addType(final Set<NodeType> typeSet, final NodeType def, final Predicate<NodeType> builtinFilter) {
        if (typeSet.contains(def) || !builtinFilter.test(def)) {
            return;
        }
        final Consumer<NodeType> accum = (nt) -> addType(typeSet, nt, builtinFilter);
        // first add super types
        optStream(def.getDeclaredSupertypes()).flatMap(Stream::of).forEachOrdered(accum);
        // then add this type
        typeSet.add(def);
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
