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

import static net.adamcin.oakpal.core.OakMachine.NT_UNDECLARED;

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
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefinitionFactory;

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
        public Builder withNs(final List<JcrNs> mappings) {
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
        public Builder withPrivilege(String... privilege) {
            if (privilege != null) {
                this.privileges.addAll(Arrays.asList(privilege));
            }
            return this;
        }

        /**
         * Register an additional JCR privilege prior to the scan. If the privilege belongs to a custom namespace, be
         * sure to register that as well using {@link #withNs(String, String)}
         *
         * @param privileges the names of the privileges
         * @return my builder self
         */
        public Builder withPrivileges(Collection<String> privileges) {
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
        public Builder withForcedRoot(String path, String... nodeTypes) {
            String primaryType = null;
            List<String> mixinTypes = Collections.emptyList();
            if (nodeTypes != null) {
                if (nodeTypes.length > 0) {
                    primaryType = nodeTypes[0];
                    if (nodeTypes.length > 1) {
                        mixinTypes = Arrays.asList(Arrays.copyOfRange(nodeTypes, 1, nodeTypes.length));
                    }
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
        public Builder withForcedRoot(ForcedRoot forcedRoot) {
            this.forcedRoots.put(forcedRoot.getPath(), forcedRoot);
            return this;
        }

        /**
         * Force the creation of the described root path prior to the scan.
         *
         * @param forcedRoots the described root path
         * @return my builder self
         */
        public Builder withForcedRoots(List<ForcedRoot> forcedRoots) {
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
        public Builder withUnorderedCndUrls(List<URL> unorderedCndUrls) {
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
        public Builder withUnorderedCndUrls(URL... unorderedCndUrl) {
            if (unorderedCndUrl != null) {
                return this.withUnorderedCndUrls(Arrays.asList(unorderedCndUrl));
            }
            return this;
        }

        /**
         * Provide a list of cnd resource URLs to install, in order.
         *
         * @param orderedCndUrls the list of cnd resource URLs.
         * @return my builder self
         */
        public Builder withOrderedCndUrls(List<URL> orderedCndUrls) {
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
        public Builder withOrderedCndUrls(URL... orderedCndUrl) {
            if (orderedCndUrl != null) {
                return this.withOrderedCndUrls(Arrays.asList(orderedCndUrl));
            }
            return this;
        }

        /**
         * Provide a list of {@link QNodeTypeDefinition}s to register.
         *
         * @param qNodeTypes qualified node types
         * @return my builder self
         */
        public Builder withQNodeTypes(final List<QNodeTypeDefinition> qNodeTypes) {
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

        // uri to prefix !!
        if (!namespaces.isEmpty()) {
            for (Map.Entry<String, String> nsEntry : namespaces.entrySet()) {
                final String uri = nsEntry.getKey();
                final String prefix = nsEntry.getValue();
                try {
                    try {
                        if (!prefix.equals(admin.getNamespacePrefix(uri))) {
                            admin.setNamespacePrefix(prefix, uri);
                        }
                    } catch (NamespaceException ex) {
                        admin.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
                    }
                } catch (final Throwable e) {
                    errorListener.onJcrNamespaceRegistrationError(e, prefix, uri);
                }
            }
        }

        if (!qNodeTypes.isEmpty()) {
            NodeTypeDefinitionFactory fac = new NodeTypeDefinitionFactory(admin);
            List<NodeTypeDefinition> nodeTypes = fac.create(qNodeTypes);

            admin.getWorkspace().getNodeTypeManager()
                    .registerNodeTypes(nodeTypes.toArray(new NodeTypeDefinition[0]), true);
        }

        if (!privileges.isEmpty()) {
            if (admin.getWorkspace() instanceof JackrabbitWorkspace) {
                PrivilegeManager pm = ((JackrabbitWorkspace) admin.getWorkspace()).getPrivilegeManager();
                for (String privilege : privileges) {
                    try {
                        pm.registerPrivilege(privilege, false, new String[0]);
                    } catch (final Throwable e) {
                        errorListener.onJcrPrivilegeRegistrationError(e, privilege);
                    }
                }
            }
        }

        if (!forcedRoots.isEmpty()) {
            List<ForcedRoot> roots = new ArrayList<>(forcedRoots.values());
            roots.sort(Comparator.comparing(root -> root.getPath().length()));
            for (ForcedRoot root : forcedRoots.values()) {
                try {
                    final String primaryType = root.getPrimaryType() != null
                            ? root.getPrimaryType()
                            : NT_UNDECLARED;
                    final List<String> mixinTypes = root.getMixinTypes() != null ? root.getMixinTypes() : Collections.emptyList();
                    Node rootNode = JcrUtils.getOrCreateByPath(root.getPath(), NT_UNDECLARED, primaryType, admin, false);
                    for (String mixinType : mixinTypes) {
                        rootNode.addMixin(mixinType);
                    }
                    admin.save();
                } catch (final Throwable e) {
                    errorListener.onForcedRootCreationError(e, root);
                    admin.refresh(false);
                }
            }
        }
    }
}
