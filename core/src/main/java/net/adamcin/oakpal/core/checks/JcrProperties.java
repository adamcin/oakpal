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

import static net.adamcin.oakpal.core.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfStrings;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.json.JsonObject;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

/**
 * A complex check for enforcing characteristics of JCR Properties of imported nodes and their descendants within the
 * scope of the workspace filter.
 * <p>
 * {@code config} options:
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
public final class JcrProperties implements ProgressCheckFactory {
    public static final String CONFIG_SCOPE_PATHS = "scopePaths";
    public static final String CONFIG_DENY_NODE_TYPES = "denyNodeTypes";
    public static final String CONFIG_SCOPE_NODE_TYPES = "scopeNodeTypes";
    public static final String CONFIG_PROPERTIES = "properties";

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        List<Rule> pathScope = Rule.fromJsonArray(arrayOrEmpty(config, CONFIG_SCOPE_PATHS));

        List<String> denyNodeTypes = mapArrayOfStrings(arrayOrEmpty(config, CONFIG_DENY_NODE_TYPES));
        List<String> nodeTypeScope = mapArrayOfStrings(arrayOrEmpty(config, CONFIG_SCOPE_NODE_TYPES));
        List<JcrPropertyConstraints> propertyChecks = JcrPropertyConstraints
                .fromJsonArray(arrayOrEmpty(config, CONFIG_PROPERTIES));
        return new Check(pathScope, denyNodeTypes, nodeTypeScope, propertyChecks);
    }

    static final class Check extends SimpleProgressCheck {
        private final List<Rule> scopePaths;
        private final List<String> denyNodeTypes;
        private final List<String> scopeNodeTypes;
        private final List<JcrPropertyConstraints> propertyChecks;
        private WorkspaceFilter wspFilter;

        Check(final List<Rule> scopePaths,
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
            return JcrProperties.class.getSimpleName();
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

            final Rule lastMatch = Rule.lastMatch(scopePaths, path);
            if (lastMatch.isInclude()) {
                this.checkNode(packageId, node);
            }
        }

        void checkNode(final PackageId packageId, final Node node) throws RepositoryException {
            for (String denyNodeType : denyNodeTypes) {
                if (node.isNodeType(denyNodeType)) {
                    majorViolation(String.format("%s (t: %s, m: %s): denied node type %s",
                            node.getPath(),
                            node.getPrimaryNodeType().getName(),
                            Stream.of(node.getMixinNodeTypes())
                                    .map(NodeTypeDefinition::getName)
                                    .collect(Collectors.toList()),
                            denyNodeType),
                            packageId);
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
    }
}
