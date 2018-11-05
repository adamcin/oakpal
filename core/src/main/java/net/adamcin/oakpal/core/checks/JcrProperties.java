/*
 * Copyright 2018 Mark Adamcin
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

package net.adamcin.oakpal.core.checks;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.util.TraversingItemVisitor;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.PackageCheckFactory;
import net.adamcin.oakpal.core.ReportCollector;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.json.JSONObject;

/**
 * A complex check for enforcing characteristics of JCR Properties of imported nodes and their descendants within the
 * scope of the workspace filter.
 * <p>
 * Configuration options:
 * <dl>
 * <dt>{@code scopePaths} ({@link Rule}{@code []})</dt>
 * <dd>A list of rules, with each pattern matched against an import path, and the {@code type}
 * ({@link net.adamcin.oakpal.core.checks.Rule.RuleType}) of the last matching rule determines whether the matched
 * path is in scope for checking the properties on the node and its descendants.</dd>
 * <dt>{@code denyNodeTypes}</dt>
 * <dd>A list of nodeType strings, which specify primary or mixin types that should be disallowed. If matched, the path is
 * marked as a violation, without descending further to children and without evaluating property checks.</dd>
 * <dt>{@code scopeNodeTypes}</dt>
 * <dd>A list of nodeType strings, which specify primary or mixin types that the list of property constraints should
 * apply to.</dd>
 * <dt>{@code properties}</dt>
 * <dd>A list of {@link JcrPropertyConstraints} definitions.</dd>
 * </dl>
 * <pre>
 *     "config": {
 *         "scopePaths": [
 *             {
 *                 "pattern": "/apps/.*",
 *                 "type": "allow"
 *             },
 *             {
 *                 "pattern": "/libs/.*",
 *                 "type": "allow"
 *             }
 *         ],
 *         "denyNodeTypes": ["sling:OsgiConfig"],
 *         "scopeNodeTypes": ["{http://www.day.com/jcr/cq/1.0}Component"],
 *         "properties": [
 *             {
 *                 "name": "{http://sling.apache.org/jcr/sling/1.0}resourceType",
 *                 "denyIfPresent": true
 *             },
 *             {
 *                 "name": "componentGroup",
 *                 "denyIfAbsent": true,
 *                 "denyIfMultivalued": true
 *             },
 *             {
 *                 "name": "sling:resourceSuperType",
 *                 "valueRules": [{
 *                     "pattern": "[^/].*",
 *                     "type": "deny"
 *                 }]
 *             }
 *         ]
 *     }
 * </pre>
 */
public class JcrProperties implements PackageCheckFactory {
    public static final String CONFIG_SCOPE_PATHS = "scopePaths";
    public static final String CONFIG_DENY_NODE_TYPES = "denyNodeTypes";
    public static final String CONFIG_SCOPE_NODE_TYPES = "scopeNodeTypes";
    public static final String CONFIG_PROPERTIES = "properties";

    class Check extends SimpleProgressCheck {
        private final List<Rule> scopePaths;
        private final List<String> denyNodeTypes;
        private final List<String> scopeNodeTypes;
        private final List<JcrPropertyConstraints> propertyChecks;
        private WorkspaceFilter wspFilter;

        public Check(final List<Rule> scopePaths,
                     final List<String> denyNodeTypes,
                     final List<String> scopeNodeTypes,
                     final List<JcrPropertyConstraints> propertyChecks) {
            this.scopePaths = scopePaths;
            this.denyNodeTypes = denyNodeTypes;
            this.scopeNodeTypes = scopeNodeTypes;
            this.propertyChecks = propertyChecks;
        }

        @Override
        public String getCheckName() {
            return JcrProperties.this.getClass().getSimpleName();
        }

        @Override
        public void beforeExtract(final PackageId packageId, final Session inspectSession,
                                  final PackageProperties packageProperties, final MetaInf metaInf,
                                  final List<PackageId> subpackages) throws RepositoryException {
            this.wspFilter = metaInf.getFilter();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node) throws RepositoryException {
            if (!wspFilter.contains(path)) {
                return;
            }

            Rule lastMatch = scopePaths.isEmpty() ? Rule.DEFAULT_ALLOW : Rule.DEFAULT_DENY;
            for (Rule rule : scopePaths) {
                if (rule.matches(path)) {
                    lastMatch = rule;
                }
            }
            if (lastMatch.isAllow()) {
                node.accept(new CheckItemVisitor(this.collector, packageId, this.denyNodeTypes,
                        this.scopeNodeTypes, this.propertyChecks));
            }
        }
    }


    class CheckItemVisitor extends TraversingItemVisitor.Default {
        private final ReportCollector collector;
        private final List<String> denyNodeTypes;
        private final List<String> scopeNodeTypes;
        private final List<JcrPropertyConstraints> propertyChecks;
        private final PackageId packageId;
        private Node deniedByType = null;

        public CheckItemVisitor(final ReportCollector collector,
                                final PackageId packageId,
                                final List<String> denyNodeTypes,
                                final List<String> scopeNodeTypes,
                                final List<JcrPropertyConstraints> propertyChecks) {
            super(false, -1);
            this.collector = collector;
            this.packageId = packageId;
            this.denyNodeTypes = denyNodeTypes;
            this.scopeNodeTypes = scopeNodeTypes;
            this.propertyChecks = propertyChecks;
        }

        @Override
        protected void entering(final Node node, final int level) throws RepositoryException {
            if (deniedByType != null) {
                return;
            }

            for (String denyNodeType : denyNodeTypes) {
                if (node.isNodeType(denyNodeType)) {
                    collector.reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("%s (t: %s, m: %s): denied node type %s",
                                    node.getPath(),
                                    node.getPrimaryNodeType().getName(),
                                    Stream.of(node.getMixinNodeTypes())
                                            .map(NodeTypeDefinition::getName)
                                            .collect(Collectors.toList()),
                                    denyNodeType),
                            packageId));
                    deniedByType = node;
                    return;
                }
            }

            boolean isInScope = scopeNodeTypes.isEmpty();
            for (String nodeType : scopeNodeTypes) {
                if (node.isNodeType(nodeType)) {
                    isInScope = true;
                }
            }
            if (isInScope) {
                for (JcrPropertyConstraints check : propertyChecks) {
                    check.evaluate(packageId, node).ifPresent(collector::reportViolation);
                }
            }
        }

        @Override
        protected void leaving(final Node node, final int level) throws RepositoryException {
            if (node == deniedByType) {
                deniedByType = null;
            }
        }
    }

    @Override
    public ProgressCheck newInstance(final JSONObject config) throws Exception {
        List<Rule> pathScope = Rule.fromJSON(config.optJSONArray(CONFIG_SCOPE_PATHS));
        List<String> denyNodeTypes = ofNullable(config.optJSONArray(CONFIG_DENY_NODE_TYPES))
                .map(array -> StreamSupport.stream(array.spliterator(), false)
                        .map(String::valueOf).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        List<String> nodeTypeScope = ofNullable(config.optJSONArray(CONFIG_SCOPE_NODE_TYPES))
                .map(array -> StreamSupport.stream(array.spliterator(), false)
                        .map(String::valueOf).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        List<JcrPropertyConstraints> propertyChecks = JcrPropertyConstraints
                .fromJSON(config.optJSONArray(CONFIG_PROPERTIES));
        return new Check(pathScope, denyNodeTypes, nodeTypeScope, propertyChecks);
    }
}
