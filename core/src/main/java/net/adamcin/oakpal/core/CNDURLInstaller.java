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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for installation of CNDs by {@link URL}. Inspired by sling mocks.
 */
final class CNDURLInstaller {

    private static final int MAX_ITERATIONS = 5;

    private final List<URL> unorderedCnds;

    private final List<URL> postInstallCnds;

    private final ErrorListener errorListener;

    /**
     * Create a new installer. Finds MANIFEST.MF Sling-Nodetypes entries on construction.
     *
     * @throws IOException if the classloader can't scan.
     */
    CNDURLInstaller(final @Nullable ErrorListener errorListener,
                    final @NotNull List<URL> unorderedCnds,
                    final @Nullable List<URL> postInstallCnds) {
        this.errorListener = errorListener != null ? errorListener : new DefaultErrorListener();
        this.unorderedCnds = new ArrayList<>(unorderedCnds);
        this.postInstallCnds = postInstallCnds != null ? new ArrayList<>(postInstallCnds) : Collections.emptyList();
    }

    /**
     * Registers node types found in classpath in JCR repository.
     *
     * @param session Session
     */
    public void register(Session session) throws RepositoryException {
        register(session, unorderedCnds);
        registerByUrl(session, postInstallCnds);
    }

    /**
     * Registers node types found in classpath in JCR repository.
     *
     * @param session           Session
     * @param nodeTypeResources List of classpath resource URLs pointing to node type definitions
     * @return map of resulting node type errors
     */
    public void register(Session session, List<URL> nodeTypeResources) throws RepositoryException {
        registerNodeTypes(session, nodeTypeResources);
    }

    /**
     * Registers node types found in classpath in JCR repository.
     *
     * @param session      Session
     * @param nodeTypeUrls List of classpath resource URLs pointing to node type definitions
     * @return map of resulting node type errors
     */
    public void registerByUrl(Session session, List<URL> nodeTypeUrls) throws RepositoryException {
        registerNodeTypesByUrl(session, nodeTypeUrls);
    }

    /**
     * Registers node types found in classpath in JCR repository.
     *
     * @param session      Session
     * @param nodeTypeUrls List of classpath resource URLs pointing to node type definitions
     */
    private void registerNodeTypesByUrl(Session session, List<URL> nodeTypeUrls) throws RepositoryException {
        final Workspace workspace = session.getWorkspace();
        final NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
        final NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        final ValueFactory valueFactory = session.getValueFactory();

        for (URL nodeTypeUrl : nodeTypeUrls) {
            try {
                this.registerNodeTypesFromUrl(nodeTypeUrl, nodeTypeManager, namespaceRegistry, valueFactory);
            } catch (Throwable t) {
                errorListener.onNodeTypeRegistrationError(t, nodeTypeUrl);
            }
        }
    }

    /**
     * Registers node types found in classpath in JCR repository.
     *
     * @param session           Session
     * @param nodeTypeResources List of classpath resource URLs pointing to node type definitions
     */
    private void registerNodeTypes(Session session, List<URL> nodeTypeResources) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        ValueFactory valueFactory = session.getValueFactory();

        final List<URL> remainingNodeTypeResources = new ArrayList<>(nodeTypeResources);

        // try registering node types multiple times because the exact order is not known
        int iteration = 0;
        while (!remainingNodeTypeResources.isEmpty()) {
            iteration++;
            if (iteration > MAX_ITERATIONS) {
                break;
            } else if (iteration == MAX_ITERATIONS) {
                tryRegisterNodeTypes(remainingNodeTypeResources, nodeTypeManager,
                        namespaceRegistry, valueFactory, true);
            } else {
                tryRegisterNodeTypes(remainingNodeTypeResources, nodeTypeManager,
                        namespaceRegistry, valueFactory, false);
            }
        }
    }

    /**
     * Register node types found in classpath in JCR repository, and remove those that succeeded to register from the list.
     *
     * @param nodeTypeResources List of nodetype classpath resources
     * @param nodeTypeManager   the node type manager
     * @param namespaceRegistry the namespace registry
     * @param valueFactory      the value factory
     */
    private void tryRegisterNodeTypes(final List<URL> nodeTypeResources,
                                      final NodeTypeManager nodeTypeManager,
                                      final NamespaceRegistry namespaceRegistry,
                                      final ValueFactory valueFactory,
                                      final boolean logErrors) {
        Iterator<URL> namedResources = nodeTypeResources.iterator();
        while (namedResources.hasNext()) {
            final URL namedResource = namedResources.next();
            try {
                registerNodeTypesFromUrl(namedResource, nodeTypeManager, namespaceRegistry, valueFactory);
                namedResources.remove();
            } catch (Throwable ex) {
                if (logErrors) {
                    errorListener.onNodeTypeRegistrationError(ex, namedResource);
                }
            }
        }
    }

    /**
     * Do the registration of a particular cnd URL.
     *
     * @param namedResource     the CND URL
     * @param nodeTypeManager   the node type manager
     * @param namespaceRegistry the namespace registry
     * @param valueFactory      the value factory
     * @throws Throwable if an error occurs
     */
    private void registerNodeTypesFromUrl(final URL namedResource,
                                          final NodeTypeManager nodeTypeManager,
                                          final NamespaceRegistry namespaceRegistry,
                                          final ValueFactory valueFactory) throws Throwable {
        try (InputStream is = namedResource.openStream()) {
            Reader reader = new InputStreamReader(is);
            CndImporter.registerNodeTypes(reader, namedResource.toExternalForm(), nodeTypeManager, namespaceRegistry,
                    valueFactory, false);
        }
    }
}

