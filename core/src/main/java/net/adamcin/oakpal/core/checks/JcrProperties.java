/*
 * Copyright 2020 Mark Adamcin
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

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.json.JsonObject;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings;

/**
 * A complex check for enforcing characteristics of JCR Properties of imported nodes and their descendants within the
 * scope of the workspace filter.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code scopePaths} ({@link Rule}{@code []})</dt>
 * <dd>A list of rules, with each pattern matched against an import path, and the {@code type}
 * ({@link Rule.RuleType}) of the last matching rule determines whether the matched
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
    @ProviderType
    public interface JsonKeys {
        String scopePaths();

        String denyNodeTypes();

        String scopeNodeTypes();

        String properties();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String scopePaths() {
            return "scopePaths";
        }

        @Override
        public String denyNodeTypes() {
            return "denyNodeTypes";
        }

        @Override
        public String scopeNodeTypes() {
            return "scopeNodeTypes";
        }

        @Override
        public String properties() {
            return "properties";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    @Deprecated
    public static final String CONFIG_SCOPE_PATHS = keys().scopePaths();
    @Deprecated
    public static final String CONFIG_DENY_NODE_TYPES = keys().denyNodeTypes();
    @Deprecated
    public static final String CONFIG_SCOPE_NODE_TYPES = keys().scopeNodeTypes();
    @Deprecated
    public static final String CONFIG_PROPERTIES = keys().properties();

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        List<Rule> pathScope = Rule.fromJsonArray(arrayOrEmpty(config, keys().scopePaths()));

        List<String> denyNodeTypes = mapArrayOfStrings(arrayOrEmpty(config, keys().denyNodeTypes()));
        List<String> nodeTypeScope = mapArrayOfStrings(arrayOrEmpty(config, keys().scopeNodeTypes()));
        final ResourceBundleHolder resourceBundleHolder = new ResourceBundleHolder();
        List<JcrPropertyConstraints> propertyChecks = JcrPropertyConstraints
                .fromJsonArray(resourceBundleHolder::getResourceBundle, arrayOrEmpty(config, keys().properties()));
        return new Check(pathScope, denyNodeTypes, nodeTypeScope, propertyChecks, resourceBundleHolder);
    }

    static final class ResourceBundleHolder {
        private ResourceBundle resourceBundle;

        public ResourceBundle getResourceBundle() {
            if (resourceBundle == null) {
                resourceBundle = ResourceBundle.getBundle(JcrProperties.class.getName());
            }
            return resourceBundle;
        }

        public void setResourceBundle(final ResourceBundle resourceBundle) {
            this.resourceBundle = resourceBundle;
        }
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<JcrProperties> {
        private final List<Rule> scopePaths;
        private final List<String> denyNodeTypes;
        private final List<String> scopeNodeTypes;
        private final List<JcrPropertyConstraints> propertyChecks;
        private final ResourceBundleHolder resourceBundleHolder;
        private WorkspaceFilter wspFilter;

        Check(final List<Rule> scopePaths,
              final List<String> denyNodeTypes,
              final List<String> scopeNodeTypes,
              final List<JcrPropertyConstraints> propertyChecks,
              final ResourceBundleHolder resourceBundleHolder) {
            super(JcrProperties.class);
            this.scopePaths = scopePaths;
            this.denyNodeTypes = denyNodeTypes;
            this.scopeNodeTypes = scopeNodeTypes;
            this.propertyChecks = propertyChecks;
            this.resourceBundleHolder = resourceBundleHolder;
        }

        @Override
        public void setResourceBundle(final ResourceBundle resourceBundle) {
            super.setResourceBundle(resourceBundle);
            resourceBundleHolder.setResourceBundle(resourceBundle);
        }

        @Override
        protected @NotNull ResourceBundle getResourceBundle() throws MissingResourceException {
            return super.getResourceBundle();
        }

        @Override
        public void beforeExtract(final PackageId packageId, final Session inspectSession,
                                  final PackageProperties packageProperties, final MetaInf metaInf,
                                  final List<PackageId> subpackages) throws RepositoryException {
            this.wspFilter = metaInf.getFilter();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node,
                                 final PathAction action) throws RepositoryException {
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
                    final Object[] arguments = new Object[]{
                            node.getPath(),
                            node.getPrimaryNodeType().getName(),
                            Stream.of(node.getMixinNodeTypes())
                                    .map(NodeTypeDefinition::getName)
                                    .collect(Collectors.toList()),
                            denyNodeType
                    };
                    reporting(violation -> violation
                            .withSeverity(Severity.MAJOR)
                            .withDescription("{0} (t: {1}, m: {2}): denied node type {3}")
                            .withArgument(arguments)
                            .withPackage(packageId));
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
