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

package net.adamcin.oakpal.toolslib;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonCollectors;
import javax.json.stream.JsonGenerator;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.checks.Rule;

/**
 * Exports {@link ForcedRoot}s from a JCR session to assist with project checklist management.
 */
public final class ForcedRootExporter {

    /**
     * Builder for a {@link ForcedRootExporter}, which is otherwise immutable.
     */
    public static class Builder {

        private List<Op> operations = new ArrayList<>();
        private List<Rule> pathScopes = new ArrayList<>();
        private List<Rule> nodeTypeScopes = new ArrayList<>();

        /**
         * Add an operation to export a set of roots by individual paths. If a specified path does not exist in the
         * repository, a root will not be exported for it.
         *
         * @param paths the list of paths to export as forced roots
         * @return this builder
         */
        public Builder byPath(final String... paths) {
            this.operations.add(new Op(OpType.PATH, paths));
            return this;
        }

        /**
         * Add an operation to export a set of roots returned by the specified JCR query. This query can be in either
         * JCR-SQL2 or XPath language.
         *
         * @param statement the JCR query statement
         * @return this builder
         */
        public Builder byQuery(final String statement) {
            this.operations.add(new Op(OpType.QUERY, statement));
            return this;
        }

        /**
         * Add an operation to export a set of roots returned by a nodetype query. By default, only nodes which are
         * _explicitly_ typed with one of the specified node types are returned. If you also want to include nodes which
         * are either _explicitly_ or _implicitly_ typed with a particular node type or one of its subtypes, you must
         * prefix the type name with a "+", e.g. "+sling:Folder" instead of "sling:Folder".
         *
         * @param nodeTypes the list of JCR node types to query for
         * @return this builder
         */
        public Builder byNodeType(final String... nodeTypes) {
            this.operations.add(new Op(OpType.NODETYPE, nodeTypes));
            return this;
        }

        /**
         * Limit the scope of exported forced root paths using a list of {@link Rule} patterns. Paths which last-match
         * an "INCLUDE" pattern are considered to be "in scope", which means they can be returned by the specified
         * operations and/or replaced in existing checklists. Use EXCLUDE patterns to protect existing forced roots from
         * being overwritten by a particular instance of {@link ForcedRootExporter}.
         *
         * @param pathScopes the list of include/exclude patterns
         * @return this builder
         * @see Rule#lastMatch(List, String)
         */
        public Builder withPathScopes(final List<Rule> pathScopes) {
            this.pathScopes = Optional.ofNullable(pathScopes).orElse(Collections.emptyList());
            return this;
        }

        /**
         * In situations where a checklist is only concerned with a subset of node types, use this method to
         * exclude irrelevant types from the output. This is useful when trying to avoid introducing a dependency on an
         * JCR namespace or CND that exists in a template repository for other purposes. For example, an EXCLUDE rule
         * for "cq:.*" would exclude any types in the cq namespace.
         *
         * @param nodeTypeScopes the list of include/exclude patterns
         * @return this builder
         * @see Rule#lastMatch(List, String)
         */
        public Builder withNodeTypeScopes(final List<Rule> nodeTypeScopes) {
            this.nodeTypeScopes = Optional.ofNullable(nodeTypeScopes).orElse(Collections.emptyList());
            return this;
        }

        /**
         * Create the new exporter instance.
         *
         * @return the new {@link ForcedRootExporter} instance
         */
        public ForcedRootExporter build() {
            return new ForcedRootExporter(this.operations, this.pathScopes, this.nodeTypeScopes);
        }
    }

    private enum OpType {
        QUERY, PATH, NODETYPE
    }

    /**
     * Represents an atomic export operation.
     */
    private static class Op {
        private final OpType opType;
        private final List<String> args;

        private Op(final OpType opType, final String... args) {
            if (args == null) {
                throw new IllegalArgumentException("args cannot be null");
            }
            this.opType = opType;
            this.args = Arrays.asList(args);
        }
    }

    private final List<Op> operations;
    private final List<Rule> pathScopes;
    private final List<Rule> nodeTypeScopes;

    private ForcedRootExporter(final List<Op> operations, final List<Rule> pathScopes, final List<Rule> nodeTypeScopes) {
        this.operations = operations;
        this.pathScopes = pathScopes;
        this.nodeTypeScopes = nodeTypeScopes;
    }

    public enum ChecklistUpdatePolicy {

        /**
         * Remove all existing forced roots from the checklist before exporting new ones.
         */
        TRUNCATE,

        /**
         * Remove existing forced roots that are included by the pathScopes before exporting new ones.
         */
        REPLACE,

        /**
         * Existing forced roots will not be removed. If exported root paths match existing root paths, the existing
         * entries will be overwritten, such that the primaryType and mixinTypes may be changed by the export.
         */
        MERGE
    }

    public static final ChecklistUpdatePolicy DEFAULT_UPDATE_POLICY = ChecklistUpdatePolicy.REPLACE;

    /**
     * Update a checklist (or start a new one) with the forced roots exported from the provided session. Tidied JSON
     * output will be written to the provided writer.
     *
     * @param writer       the writer to write the JSON output to
     * @param session      the JCR session to export roots from
     * @param checklist    the checklist to update, or null to start from scratch
     * @param updatePolicy specify behavior for retaining existing forced roots
     * @throws RepositoryException if an error occurs when exporting the new forced roots
     */
    public void updateChecklist(final Writer writer,
                                final Session session,
                                final JsonObject checklist,
                                final ChecklistUpdatePolicy updatePolicy)
            throws RepositoryException {

        // try to find the roots. If any error occurs there, we want to fail fast before committing to other
        // potentially expensive, destructive, or error-prone logic.
        final List<ForcedRoot> newRoots = findRoots(session);

        // construct a stream filter for retaining existing forced roots
        Predicate<ForcedRoot> retainFilter;
        switch (updatePolicy != null ? updatePolicy : DEFAULT_UPDATE_POLICY) {
            case TRUNCATE:
                // retain nothing
                retainFilter = root -> false;
                break;
            case REPLACE:
                // only retain roots excluded by the path filter
                retainFilter = root -> Rule.lastMatch(pathScopes, root.getPath()).isExclude();
                break;
            case MERGE:
            default:
                // retain everything
                retainFilter = root -> true;
                break;
        }

        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final Map<String, ForcedRoot> existing = new LinkedHashMap<>();
        if (checklist != null) {
            checklist.forEach(builder::add);
            if (checklist.containsKey(Checklist.KEY_FORCED_ROOTS)) {
                JavaxJson.mapArrayOfObjects(checklist.getJsonArray(Checklist.KEY_FORCED_ROOTS), ForcedRoot::fromJson).stream()
                        .filter(retainFilter)
                        .forEachOrdered(root -> existing.put(root.getPath(), root));
            }
        }

        newRoots.forEach(root -> existing.put(root.getPath(), root));

        final JsonArray forcedRoots = existing.values().stream()
                .map(ForcedRoot::toJson)
                .collect(JsonCollectors.toJsonArray());
        builder.add(Checklist.KEY_FORCED_ROOTS, forcedRoots);

        try (JsonWriter jsonWriter = Json
                .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(writer)) {
            jsonWriter.writeObject(builder.build());
        }
    }

    /**
     * Perform all retrieval operations against the provided session.
     *
     * @param session the session to retrieve nodes from
     * @return the result list of {@link ForcedRoot}s
     * @throws RepositoryException if an error occurs
     */
    public List<ForcedRoot> findRoots(final Session session) throws RepositoryException {
        List<ForcedRoot> roots = new ArrayList<>();
        for (Op op : this.operations) {
            switch (op.opType) {
                case PATH:
                    roots.addAll(traverse(session, op.args));
                    break;
                case NODETYPE:
                    roots.addAll(query(session, ntStatement(session, op.args)));
                    break;
                case QUERY:
                default:
                    roots.addAll(query(session, op.args.get(0)));
                    break;
            }
        }
        return roots;
    }

    /**
     * Construct a union SQL2 statement to find nodes of specified types. Prefix a node type name with a '+' to treat
     * that type as a supertype and find nodes which are of that type or any of its subtypes.
     *
     * @param session       the session which will be queried
     * @param nodeTypeNames the list of node type names to query from
     * @return the generated JCR-SQL2 query statement
     * @throws RepositoryException if an error occurs
     */
    String ntStatement(final Session session, final List<String> nodeTypeNames) throws RepositoryException {
        final List<String> subqueries = new ArrayList<>();
        final NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();

        final Predicate<String> covariantFilter = name -> name.startsWith("+");

        // covariants (find nodes of specified types or their subtypes)
        nodeTypeNames.stream()
                .filter(covariantFilter)
                .map(name -> name.substring(1))
                .filter(FunUtil.testOrDefault(ntManager::hasNodeType, false))
                .map(name -> String.format("SELECT [jcr:path] FROM [%s] AS a", name))
                .forEachOrdered(subqueries::add);

        // invariants (find only nodes which are of specified types)
        nodeTypeNames.stream()
                .filter(covariantFilter.negate())
                .filter(FunUtil.testOrDefault(ntManager::hasNodeType, false))
                .map(name -> String.format("SELECT [jcr:path] FROM [nt:base] AS a WHERE [a].[jcr:primaryType] = '%s' UNION SELECT [jcr:path] FROM [nt:base] AS a WHERE [a].[jcr:mixinTypes] = '%s'", name, name))
                .forEachOrdered(subqueries::add);

        return String.join(" UNION ", subqueries);
    }

    /**
     * Retrieve a list of {@link ForcedRoot}s by JCR query.
     *
     * @param session   the session to retrieve nodes from
     * @param statement the query statement
     * @return the result list of ForcedRoots
     * @throws RepositoryException when an error occurs
     */
    List<ForcedRoot> query(final Session session, final String statement) throws RepositoryException {
        final QueryManager qm = session.getWorkspace().getQueryManager();
        final String language =
                statement.toUpperCase().replaceFirst("^\\s*((MEASURE|EXPLAIN)\\s*)*", "")
                        .startsWith("SELECT") ? Query.JCR_SQL2 : Query.XPATH;
        final Query query = qm.createQuery(statement, language);
        final QueryResult result = query.execute();
        final Map<String, ForcedRoot> roots = new LinkedHashMap<>();
        for (NodeIterator nodes = result.getNodes(); nodes.hasNext(); ) {
            nodeToRoot(nodes.nextNode()).ifPresent(root -> roots.put(root.getPath(), root));
        }
        return new ArrayList<>(roots.values());
    }

    /**
     * Gets each node from the session, then adapts to a {@link ForcedRoot} within the current path and node type scopes.
     *
     * @param session the session to retrieve nodes from
     * @param paths   the list of node paths to retrieve
     * @return the result list of ForcedRoots
     * @throws RepositoryException when an error occurs
     */
    List<ForcedRoot> traverse(final Session session, final List<String> paths) throws RepositoryException {
        final List<ForcedRoot> roots = new ArrayList<>();
        for (String path : paths) {
            if (session.nodeExists(path)) {
                nodeToRoot(session.getNode(path)).ifPresent(roots::add);
            }
        }
        return roots;
    }

    /**
     * Construct a forced root object for the given node, within the context of path and type scopes.
     *
     * @param node the node to convert to a {@link ForcedRoot}
     * @return empty when the node is not within path scope
     * @throws RepositoryException when an error occurs
     */
    Optional<ForcedRoot> nodeToRoot(final Node node) throws RepositoryException {
        if (Rule.lastMatch(pathScopes, node.getPath()).isExclude()) {
            return Optional.empty();
        }

        ForcedRoot forcedRoot = new ForcedRoot();
        forcedRoot.setPath(node.getPath());
        final String primaryType = node.getPrimaryNodeType().getName();
        if (Rule.lastMatch(nodeTypeScopes, primaryType).isInclude()) {
            forcedRoot.setPrimaryType(primaryType);
        }
        final List<String> mixinTypes = Stream.of(node.getMixinNodeTypes())
                .map(NodeType::getName)
                .filter(name -> Rule.lastMatch(nodeTypeScopes, name).isInclude())
                .collect(Collectors.toList());
        forcedRoot.setMixinTypes(mixinTypes);
        return Optional.of(forcedRoot);
    }

}
