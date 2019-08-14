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

package net.adamcin.oakpal.core;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefinitionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.adamcin.oakpal.core.Fun.onEntry;
import static net.adamcin.oakpal.core.Fun.uncheckVoid1;
import static net.adamcin.oakpal.core.OakMachine.NT_UNDECLARED;

/**
 * Encapsulation of JCR initialization parameters for multistage inits.
 */
public final class InitStage {
    private final List<URL> unorderedCndUrls;

    private final List<URL> orderedCndUrls;

    private final List<QNodeTypeDefinition> qNodeTypes;

    // uri to prefix !!
    private final Map<String, String> namespaces;

    private final Set<String> privileges;

    private final Map<String, ForcedRoot> forcedRoots;

    private InitStage(final List<URL> unorderedCndUrls,
                      final List<URL> orderedCndUrls,
                      final List<QNodeTypeDefinition> qNodeTypes,
                      final Map<String, String> namespaces,
                      final Set<String> privileges,
                      final Map<String, ForcedRoot> forcedRoots) {
        this.unorderedCndUrls = unorderedCndUrls;
        this.orderedCndUrls = orderedCndUrls;
        this.qNodeTypes = qNodeTypes;
        this.namespaces = namespaces;
        this.privileges = privileges;
        this.forcedRoots = forcedRoots;
    }

    /**
     * Use the builder to construct the {@link InitStage}.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private Map<String, String> namespaces = new LinkedHashMap<>();

        private Set<String> privileges = new LinkedHashSet<>();

        private Map<String, ForcedRoot> forcedRoots = new LinkedHashMap<>();

        private List<URL> unorderedCndUrls = new ArrayList<>();

        private List<URL> orderedCndUrls = new ArrayList<>();

        private List<QNodeTypeDefinition> qNodeTypes = new ArrayList<>();

        /**
         * Register an additional JCR namespace prior to the scan.
         *
         * @param prefix the registered prefix for the namespace
         * @param uri    the registered URI for the namespace
         * @return my builder self
         */
        public Builder withNs(String prefix, String uri) {
            this.namespaces.put(uri, prefix);
            return this;
        }

        /**
         * Register additional JCR namespaces prior to the scan.
         *
         * @param mappings list of mappings
         * @return my builder self
         */
        public Builder withNs(final @NotNull List<JcrNs> mappings) {
            mappings.forEach(jcrNs -> withNs(jcrNs.getPrefix(), jcrNs.getUri()));
            return this;
        }

        /**
         * Register an additional JCR privilege prior to the scan. If the privilege belongs to a custom namespace, be
         * sure to register that as well using {@link #withNs(String, String)}
         *
         * @param privilege the name of the privilege
         * @return my builder self
         */
        public Builder withPrivilege(final @NotNull String... privilege) {
            this.privileges.addAll(Arrays.asList(privilege));
            return this;
        }

        /**
         * Register an additional JCR privilege prior to the scan. If the privilege belongs to a custom namespace, be
         * sure to register that as well using {@link #withNs(String, String)}
         *
         * @param privileges the names of the privileges
         * @return my builder self
         */
        public Builder withPrivileges(final @Nullable Collection<String> privileges) {
            if (privileges != null) {
                this.privileges = new HashSet<>(privileges);
            } else {
                this.privileges = new HashSet<>();
            }
            return this;
        }

        /**
         * Force the creation of the described root path prior to the scan.
         *
         * @param path      the JCR path
         * @param nodeTypes the list of nodetypes. the first element is assumed
         *                  to be the primary type, with subsequent elements treated as mixin types.
         * @return my builder self
         */
        public Builder withForcedRoot(final @Nullable String path, final @NotNull String... nodeTypes) {
            String primaryType = null;
            List<String> mixinTypes = Collections.emptyList();
            if (nodeTypes.length > 0) {
                primaryType = nodeTypes[0];
                if (nodeTypes.length > 1) {
                    mixinTypes = Arrays.asList(Arrays.copyOfRange(nodeTypes, 1, nodeTypes.length));
                }
            }
            ForcedRoot forcedRoot = new ForcedRoot();
            forcedRoot.setPath(path);
            forcedRoot.setPrimaryType(primaryType);
            forcedRoot.setMixinTypes(mixinTypes);
            return this.withForcedRoot(forcedRoot);
        }

        /**
         * Force the creation of the described root path prior to the scan.
         *
         * @param forcedRoot the described root path
         * @return my builder self
         */
        public Builder withForcedRoot(final @NotNull ForcedRoot forcedRoot) {
            this.forcedRoots.put(forcedRoot.getPath(), forcedRoot);
            return this;
        }

        /**
         * Force the creation of the described root path prior to the scan.
         *
         * @param forcedRoots the described root path
         * @return my builder self
         */
        public Builder withForcedRoots(final @Nullable List<ForcedRoot> forcedRoots) {
            if (forcedRoots != null) {
                forcedRoots.forEach(root -> this.forcedRoots.put(root.getPath(), root));
            }
            return this;
        }

        /**
         * Provide a list of cnd resource names to install.
         *
         * @param unorderedCndUrls the list of cnd resources
         * @return my builder self
         */
        public Builder withUnorderedCndUrls(final @Nullable List<URL> unorderedCndUrls) {
            if (unorderedCndUrls != null) {
                this.unorderedCndUrls.addAll(unorderedCndUrls);
            }
            return this;
        }

        /**
         * Provide a list of cnd resources to install.
         *
         * @param unorderedCndUrl the list of cnd resource names
         * @return my builder self
         */
        public Builder withUnorderedCndUrl(final @NotNull URL... unorderedCndUrl) {
            return this.withUnorderedCndUrls(Arrays.asList(unorderedCndUrl));
        }

        /**
         * Provide a list of cnd resource URLs to install, in order.
         *
         * @param orderedCndUrls the list of cnd resource URLs.
         * @return my builder self
         */
        public Builder withOrderedCndUrls(final @Nullable List<URL> orderedCndUrls) {
            if (orderedCndUrls != null) {
                this.orderedCndUrls.addAll(orderedCndUrls);
            }
            return this;
        }

        /**
         * Provide a list of cnd resources to install, in order.
         *
         * @param orderedCndUrl the list of cnd resource URLs
         * @return my builder self
         */
        public Builder withOrderedCndUrl(final @NotNull URL... orderedCndUrl) {
            return this.withOrderedCndUrls(Arrays.asList(orderedCndUrl));
        }

        /**
         * Provide a list of {@link QNodeTypeDefinition}s to register.
         *
         * @param qNodeTypes qualified node types
         * @return my builder self
         */
        public Builder withQNodeTypes(final @Nullable List<QNodeTypeDefinition> qNodeTypes) {
            if (qNodeTypes != null) {
                this.qNodeTypes.addAll(qNodeTypes);
            }
            return this;
        }

        /**
         * Construct an {@link InitStage} from the {@link Builder} state.
         *
         * @return an {@link InitStage}
         */
        public InitStage build() {
            return new InitStage(unorderedCndUrls, orderedCndUrls, qNodeTypes, namespaces, privileges, forcedRoots);
        }
    }

    void initSession(final Session admin, final ErrorListener errorListener) throws RepositoryException {
        final CNDURLInstaller cndInstaller = new CNDURLInstaller(errorListener,
                this.unorderedCndUrls, this.orderedCndUrls);

        cndInstaller.register(admin);

        final NamespaceRegistry registry = admin.getWorkspace().getNamespaceRegistry();

        // uri to prefix !!
        namespaces.entrySet().stream().forEachOrdered(onEntry((uri, prefix) -> {
            try {
                if (Arrays.asList(registry.getURIs()).contains(uri)) {
                    admin.setNamespacePrefix(prefix, uri);
                } else {
                    registry.registerNamespace(prefix, uri);
                }
            } catch (final Exception e) {
                errorListener.onJcrNamespaceRegistrationError(e, prefix, uri);
            }
        }));

        if (!qNodeTypes.isEmpty()) {
            try {
                NodeTypeDefinitionFactory fac = new NodeTypeDefinitionFactory(admin);
                List<NodeTypeDefinition> nodeTypes = fac.create(qNodeTypes);

                admin.getWorkspace().getNodeTypeManager()
                        .registerNodeTypes(nodeTypes.toArray(new NodeTypeDefinition[0]), true);
            } catch (final RepositoryException e) {
                // TODO should an init stage also carry an optional source URL for this kind of thing?
                errorListener.onNodeTypeRegistrationError(e, null);
            }
        }

        if (!privileges.isEmpty()) {
            if (admin.getWorkspace() instanceof JackrabbitWorkspace) {
                PrivilegeManager pm = ((JackrabbitWorkspace) admin.getWorkspace()).getPrivilegeManager();
                privileges.forEach(privilege -> {
                    try {
                        pm.registerPrivilege(privilege, false, new String[0]);
                    } catch (final Exception e) {
                        errorListener.onJcrPrivilegeRegistrationError(e, privilege);
                    }
                });
            }
        }

        forcedRoots.values().stream()
                .filter(ForcedRoot::hasPath)
                .sorted(Comparator.comparing(root -> root.getPath().length()))
                .forEachOrdered(uncheckVoid1(root -> {
                    try {
                        final String primaryType = root.getPrimaryType() != null
                                ? root.getPrimaryType()
                                : NT_UNDECLARED;
                        final List<String> mixinTypes = root.getMixinTypes();
                        Node rootNode = JcrUtils.getOrCreateByPath(root.getPath(),
                                NT_UNDECLARED, primaryType, admin, false);
                        mixinTypes.forEach(uncheckVoid1(rootNode::addMixin));
                        admin.save();
                    } catch (final Exception e) {
                        errorListener.onForcedRootCreationError(e, root);
                        admin.refresh(false);
                    }
                }));
    }
}
