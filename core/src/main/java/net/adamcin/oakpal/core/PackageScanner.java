/*
 * Copyright 2017 Mark Adamcin
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import aQute.bnd.annotation.ProviderType;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.jcr.repository.RepositoryImpl;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.security.user.RandomAuthorizableNodeName;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authorization.AuthorizationConfiguration;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.oak.spi.security.user.AuthorizableNodeName;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.fs.spi.CNDReader;
import org.apache.jackrabbit.vault.fs.spi.DefaultNodeTypeSet;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeInstaller;
import org.apache.jackrabbit.vault.fs.spi.ServiceProviderFactory;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.packaging.VaultPackage;

/**
 * Entry point for OakPAL Acceptance Library. See {@link PackageListener} for the event listener interface.
 */
@ProviderType
public final class PackageScanner {
    private static final ErrorListener DEFAULT_ERROR_LISTENER = new DefaultErrorListener();

    private final List<PackageListener> packageListeners;
    private final ErrorListener errorListener;
    private final List<File> preInstallPackages;
    private final List<File> cndFiles;
    // uri to prefix !!
    private final Map<String, String> namespaces;
    private final Set<String> privileges;
    private final Map<String, ForcedRoot> forcedRoots;

    private PackageScanner(List<PackageListener> packageListeners,
                           ErrorListener errorListener,
                           List<File> preInstallPackages,
                           List<File> cndFiles,
                           Map<String, String> namespaces,
                           Set<String> privileges,
                           Map<String, ForcedRoot> forcedRoots) {
        this.packageListeners = packageListeners;
        this.errorListener = errorListener;
        this.preInstallPackages = preInstallPackages;
        this.cndFiles = cndFiles;
        this.namespaces = namespaces;
        this.privileges = privileges;
        this.forcedRoots = forcedRoots;
    }

    public static final class ForcedRoot {
        private String path;
        private String primaryType;
        private List<String> mixinTypes;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPrimaryType() {
            return primaryType;
        }

        public void setPrimaryType(String primaryType) {
            this.primaryType = primaryType;
        }

        public List<String> getMixinTypes() {
            return mixinTypes;
        }

        public void setMixinTypes(List<String> mixinTypes) {
            this.mixinTypes = mixinTypes;
        }
    }

    /**
     * Use the builder to construct the {@link PackageScanner}.
     */
    public static class Builder {
        private Map<String, String> namespaces = new HashMap<>();
        private Set<String> privileges = new HashSet<>();
        private Map<String, ForcedRoot> forcedRoots = new HashMap<>();
        private List<PackageListener> packageListeners = Collections.emptyList();
        private ErrorListener errorListener = DEFAULT_ERROR_LISTENER;
        private List<File> preInstallPackages = Collections.emptyList();
        private List<File> cndFiles = Collections.emptyList();

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
         * Set a single instance of {@link PackageListener} for the scan.
         *
         * @param listener the package listeners
         * @return my builder self
         */
        public Builder withPackageListener(PackageListener... listener) {
            if (listener != null) {
                return this.withPackageListeners(Arrays.asList(listener));
            } else {
                return this.withPackageListeners(Collections.emptyList());
            }
        }

        /**
         * Set the list of {@link PackageListener}s for the scan.
         *
         * @param listeners the list of packageListeners
         * @return my builder self
         */
        public Builder withPackageListeners(List<? extends PackageListener> listeners) {
            if (listeners != null) {
                this.packageListeners = new ArrayList<>(listeners);
            } else {
                this.packageListeners = Collections.emptyList();
            }
            return this;
        }

        /**
         * Set the single {@link ErrorListener} for the scan.
         *
         * @param errorListener the error handler
         * @return my builder self
         */
        public Builder withErrorListener(ErrorListener errorListener) {
            if (errorListener == null) {
                this.errorListener = DEFAULT_ERROR_LISTENER;
            } else {
                this.errorListener = errorListener;
            }
            return this;
        }

        /**
         * Provide a list of package files to install before each scan. Install events raised during
         * pre-install are not passed to each {@link PackageListener}, but errors raised are passed to
         * the {@link ErrorListener}.
         *
         * @param preInstallPackage the list of pre-install package files
         * @return my builder self
         */
        public Builder withPreInstallPackage(File... preInstallPackage) {
            if (preInstallPackage != null) {
                return this.withPreInstallPackages(Arrays.asList(preInstallPackage));
            } else {
                return this.withPreInstallPackages(Collections.emptyList());
            }
        }

        /**
         * Provide a list of package files to install before each scan. Install events raised during
         * pre-install are not passed to each {@link PackageListener}, but errors raised are passed to
         * the {@link ErrorListener}.
         *
         * @param preInstallPackages the list of pre-install package files
         * @return my builder self
         */
        public Builder withPreInstallPackages(List<File> preInstallPackages) {
            if (preInstallPackages != null) {
                this.preInstallPackages = new ArrayList<>(preInstallPackages);
            } else {
                this.preInstallPackages = Collections.emptyList();
            }
            return this;
        }

        /**
         * Provide a list of cnd files to install.
         *
         * @param cndFiles the list of cnd files
         * @return my builder self
         */
        public Builder withCndFiles(List<File> cndFiles) {
            if (cndFiles != null) {
                this.cndFiles = new ArrayList<>(cndFiles);
            } else {
                this.cndFiles = Collections.emptyList();
            }
            return this;
        }

        /**
         * Provide a list of cnd files to install.
         *
         * @param cndFile the list of cnd files
         * @return my builder self
         */
        public Builder withCndFile(File... cndFile) {
            if (cndFile != null) {
                return this.withCndFiles(Arrays.asList(cndFile));
            } else {
                return this.withCndFiles(Collections.emptyList());
            }
        }

        /**
         * Construct a {@link PackageScanner} from the {@link Builder} state.
         *
         * @return a {@link PackageScanner}
         */
        public PackageScanner build() {
            return new PackageScanner(packageListeners,
                    errorListener,
                    preInstallPackages,
                    cndFiles,
                    namespaces,
                    privileges,
                    forcedRoots);
        }
    }

    public List<PackageListener> getPackageListeners() {
        return packageListeners;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public List<File> getPreInstallPackages() {
        return preInstallPackages;
    }

    public List<ViolationReport> scanPackage(File... file) throws AbortedScanException {
        if (file != null) {
            return scanPackages(Arrays.asList(file));
        } else {
            return scanPackages(Collections.emptyList());
        }
    }

    public List<ViolationReport> scanPackages(List<File> files) throws AbortedScanException {
        Session admin = null;
        Repository scanRepo = null;
        try {
            getErrorListener().startedScan();
            packageListeners.forEach(PackageListener::startedScan);

            scanRepo = initRepository();
            admin = loginAdmin(scanRepo);

            if (!cndFiles.isEmpty()) {
                final DefaultNodeTypeSet nodeTypes = new DefaultNodeTypeSet("internal");
                final CNDReader cndReader = ServiceProviderFactory.getProvider().getCNDReader();
                final NamespaceMapping mapping =
                        new NamespaceMapping(new SessionNamespaceResolver(admin));

                for (File cndFile : cndFiles) {
                    Reader reader = null;
                    try {
                        reader = new InputStreamReader(new FileInputStream(cndFile), "utf8");
                        cndReader.read(reader, cndFile.toURI().toString(), mapping);
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                }

                nodeTypes.add(cndReader);
                NodeTypeInstaller installer = ServiceProviderFactory.getProvider().getDefaultNodeTypeInstaller(admin);
                installer.install(null, nodeTypes);
            }

            // uri to prefix !!
            if (!namespaces.isEmpty()) {
                for (Map.Entry<String, String> nsEntry : namespaces.entrySet()) {
                    admin.getWorkspace().getNamespaceRegistry().registerNamespace(nsEntry.getValue(), nsEntry.getKey());
                }
            }

            if (!privileges.isEmpty()) {
                if (admin.getWorkspace() instanceof JackrabbitWorkspace) {
                    PrivilegeManager pm = ((JackrabbitWorkspace) admin.getWorkspace()).getPrivilegeManager();
                    for (String privilege : privileges) {
                        pm.registerPrivilege(privilege, false, new String[0]);
                    }
                }
            }

            if (!forcedRoots.isEmpty()) {
                for (ForcedRoot root : forcedRoots.values()) {
                    final String path = root.getPath();
                    final String primaryType = root.getPrimaryType() != null ? root.getPrimaryType() : "nt:unstructured";
                    final List<String> mixinTypes = root.getMixinTypes() != null ? root.getMixinTypes() : Collections.emptyList();
                    Node rootNode = JcrUtils.getOrCreateByPath(root.getPath(), primaryType, admin);
                    for (String mixinType : mixinTypes) {
                        rootNode.addMixin(mixinType);
                    }
                }
                admin.save();
            }

            JcrPackageManager manager = PackagingService.getPackageManager(admin);

            for (File file : preInstallPackages) {
                processPackageFile(admin, manager, file, true);
            }

            if (files != null) {
                for (File file : files) {
                    processPackageFile(admin, manager, file, false);
                }
            }
        } catch (RepositoryException | IOException e) {
            throw new AbortedScanException(e);
        } finally {
            if (admin != null) {
                admin.logout();
            }
            shutdownRepository(scanRepo);

            packageListeners.forEach(PackageListener::finishedScan);
            getErrorListener().finishedScan();
        }

        List<ViolationReport> reports = new ArrayList<>();
        if (getErrorListener() instanceof ViolationReporter) {
            reports.add(SimpleViolationReport.generateReport((ViolationReporter) getErrorListener()));
        }

        List<ViolationReport> listenerReports = packageListeners.stream()
                .filter(l -> l instanceof ViolationReporter)
                .map(l -> SimpleViolationReport.generateReport((ViolationReporter) l))
                .collect(Collectors.toList());

        reports.addAll(listenerReports);

        return Collections.unmodifiableList(reports);
    }

    private void processPackage(Session admin, JcrPackageManager manager, JcrPackage jcrPackage, final boolean preInstall)
            throws IOException, PackageException, RepositoryException {
        final PackageId packageId = jcrPackage.getPackage().getId();
        final SessionFacade inspectSession = new SessionFacade(admin, false);
        final ProgressTrackerListener tracker =
                new ImporterListenerAdapter(packageId, packageListeners, inspectSession, preInstall);

        ImportOptions options = new ImportOptions();
        options.setNonRecursive(true);
        options.setDependencyHandling(DependencyHandling.IGNORE);
        options.setListener(tracker);

        List<PackageId> subpacks = Arrays.asList(jcrPackage.extractSubpackages(options));

        final VaultPackage vaultPackage = jcrPackage.getPackage();

        if (!preInstall) {
            packageListeners.forEach(handler -> handler.beforeExtract(packageId,
                    vaultPackage.getProperties(), vaultPackage.getMetaInf(), subpacks));
        }

        jcrPackage.extract(options);

        jcrPackage.close();

        if (!preInstall) {
            packageListeners.forEach(handler -> {
                try {
                    handler.afterExtract(packageId, inspectSession);
                } catch (RepositoryException e) {
                    errorListener.onListenerException(e, handler, packageId);
                }
            });
        }

        for (PackageId subpackId : subpacks) {
            processSubpackage(admin, manager, subpackId, packageId, preInstall);
        }
    }

    private void processSubpackage(Session admin, JcrPackageManager manager, PackageId packageId, PackageId parentId, final boolean preInstall) {
        JcrPackage jcrPackage = null;
        try {
            jcrPackage = manager.open(packageId);

            if (!preInstall) {
                packageListeners.forEach(handler -> handler.identifySubpackage(packageId, parentId));
            }

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            getErrorListener().onSubpackageException(e, packageId);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }

    private void processPackageFile(Session admin, JcrPackageManager manager, final File file, final boolean preInstall)
            throws AbortedScanException {
        JcrPackage jcrPackage = null;

        try {
            jcrPackage = manager.upload(file, false, true, null, true);
            final PackageId packageId = jcrPackage.getPackage().getId();

            if (!preInstall) {
                packageListeners.forEach(handler -> {
                    try {
                        handler.identifyPackage(packageId, file);
                    } catch (Exception e) {
                        getErrorListener().onListenerException(e, handler, packageId);
                    }
                });
            }

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            throw new AbortedScanException(e, file);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }

    private Repository initRepository() throws RepositoryException {
        Properties userProps = new Properties();
        AuthorizableNodeName nameGenerator = new RandomAuthorizableNodeName();

        userProps.put(UserConstants.PARAM_USER_PATH, "/home/users");
        userProps.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
        userProps.put(AccessControlAction.USER_PRIVILEGE_NAMES, new String[]{PrivilegeConstants.JCR_ALL});
        userProps.put(AccessControlAction.GROUP_PRIVILEGE_NAMES, new String[]{PrivilegeConstants.JCR_READ});
        userProps.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);
        userProps.put(UserConstants.PARAM_AUTHORIZABLE_NODE_NAME, nameGenerator);
        userProps.put("cacheExpiration", 3600 * 1000);
        Properties authzProps = new Properties();
        authzProps.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);
        Properties securityProps = new Properties();
        securityProps.put(UserConfiguration.NAME, ConfigurationParameters.of(userProps));
        securityProps.put(AuthorizationConfiguration.NAME, ConfigurationParameters.of(authzProps));

        Jcr jcr = new Jcr();
        Repository repository = jcr
                .with(new SecurityProviderImpl(ConfigurationParameters.of(securityProps)))
                .withAtomicCounter()
                .createRepository();

        return repository;
    }

    private void shutdownRepository(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            ((RepositoryImpl) repository).shutdown();
        }
    }

    private Session loginAdmin(Repository repository) throws RepositoryException {
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    private class ImporterListenerAdapter implements ProgressTrackerListener {
        private final PackageId packageId;
        private final List<PackageListener> handlers;
        private final SessionFacade session;
        private final boolean preInstall;

        ImporterListenerAdapter(PackageId packageId, List<PackageListener> handlers, SessionFacade session, boolean preInstall) {
            this.packageId = packageId;
            this.handlers = handlers;
            this.session = session;
            this.preInstall = preInstall;
        }

        @Override
        public void onMessage(Mode mode, String action, String path) {
            if (preInstall) {
                return;
            }
            if (path != null && path.startsWith("/")) {
                if ("D".equals(action) || "!".equals(action)) {
                    handlers.forEach(handler -> handler.deletedPath(packageId, path));
                } else {
                    try {
                        Node node = session.getNode(path);
                        handlers.forEach(handler -> {
                            try {
                                handler.importedPath(packageId, path, node);
                            } catch (Exception e) {
                                PackageScanner.this.getErrorListener().onListenerPathException(e, handler, packageId, path);
                            }
                        });
                    } catch (RepositoryException e) {
                        PackageScanner.this.getErrorListener().onImporterException(e, packageId, path);
                    }
                }
            }
        }

        @Override
        public void onError(Mode mode, String path, Exception e) {
            PackageScanner.this.getErrorListener().onImporterException(e, packageId, path);
        }
    }
}
