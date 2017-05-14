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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import aQute.bnd.annotation.ProviderType;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
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
import org.apache.jackrabbit.vault.fs.spi.ProgressTracker;
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

    private PackageScanner(List<PackageListener> packageListeners,
                           ErrorListener errorListener,
                           List<File> preInstallPackages,
                           List<File> cndFiles) {
        this.packageListeners = packageListeners;
        this.errorListener = errorListener;
        this.preInstallPackages = preInstallPackages;
        this.cndFiles = cndFiles;
    }

    /**
     * Use the builder to construct the {@link PackageScanner}.
     */
    public static class Builder {
        private List<PackageListener> packageListeners = Collections.emptyList();
        private ErrorListener errorListener = DEFAULT_ERROR_LISTENER;
        private List<File> preInstallPackages = Collections.emptyList();
        private List<File> cndFiles = Collections.emptyList();

        /**
         * Set the list of {@link PackageListener}s for the scan.
         *
         * @param listeners the list of packageListeners
         * @return my builder self
         */
        public Builder withPackageListeners(PackageListener... listeners) {
            if (listeners != null) {
                this.packageListeners = Arrays.asList(listeners);
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
         * @param preInstallPackages the list of pre-install package files
         * @return my builder self
         */
        public Builder withPreInstallPackages(File... preInstallPackages) {
            if (preInstallPackages != null) {
                this.preInstallPackages = Arrays.asList(preInstallPackages);
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
        public Builder withCndFiles(File... cndFiles) {
            if (this.cndFiles != null) {
                this.cndFiles = Arrays.asList(cndFiles);
            } else {
                this.cndFiles = Collections.emptyList();
            }
            return this;
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
                    cndFiles);
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

    public List<ViolationReport> scanPackages(File... files) {
        // initialize listeners
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        Session admin = null;
        Repository scanRepo = null;
        try {
            getErrorListener().onBeginScan();
            packageListeners.forEach(PackageListener::onBeginScan);

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

            JcrPackageManager manager = PackagingService.getPackageManager(admin);

            for (File file : preInstallPackages) {
                processPackageFile(admin, manager, file, true);
            }

            for (File file : files) {
                processPackageFile(admin, manager, file, false);
            }

        } catch (RuntimeException e) {
            getErrorListener().onFatalError(e);
            throw e;
        } catch (Exception e) {
            getErrorListener().onFatalError(e);
        } finally {
            if (admin != null) {
                admin.logout();
            }
            shutdownRepository(scanRepo);

            packageListeners.forEach(PackageListener::onEndScan);
            getErrorListener().onEndScan();
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
            throws PackageException, IOException, RepositoryException {
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

        packageListeners.forEach(handler -> handler.onOpen(packageId,
                vaultPackage.getProperties(), vaultPackage.getMetaInf(), subpacks));

        jcrPackage.extract(options);

        jcrPackage.close();

        packageListeners.forEach(handler -> {
            try {
                handler.onClose(packageId, inspectSession);
            } catch (RepositoryException e) {
                errorListener.onListenerException(e, handler, packageId);
            }
        });

        for (PackageId subpackId : subpacks) {
            processSubpackage(admin, manager, subpackId, packageId, preInstall);
        }
    }

    private void processSubpackage(Session admin, JcrPackageManager manager, PackageId packageId, PackageId parentId, final boolean preInstall) {
        JcrPackage jcrPackage = null;
        try {
            jcrPackage = manager.open(packageId);

            packageListeners.forEach(handler -> handler.onBeginSubpackage(packageId, parentId));

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            getErrorListener().onPackageException(e, packageId);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }

    private void processPackageFile(Session admin, JcrPackageManager manager, final File file, final boolean preInstall) {
        JcrPackage jcrPackage = null;

        try {
            jcrPackage = manager.upload(file, false, true, null, true);
            final PackageId packageId = jcrPackage.getPackage().getId();

            if (!preInstall) {
                packageListeners.forEach(handler -> {
                    try {
                        handler.onBeginPackage(packageId, file);
                    } catch (Exception e) {
                        getErrorListener().onListenerException(e, handler, packageId);
                    }
                });
            }

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            getErrorListener().onPackageException(e, file);
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
                    handlers.forEach(handler -> handler.onDeletePath(packageId, path));
                } else {
                    try {
                        Node node = session.getNode(path);
                        handlers.forEach(handler -> {
                            try {
                                handler.onImportPath(packageId, path, node);
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
