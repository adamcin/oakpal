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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import aQute.bnd.annotation.ProviderType;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.apache.jackrabbit.oak.InitialContent;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.jcr.repository.RepositoryImpl;
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider;
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler;
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.property.OrderedPropertyIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.reference.ReferenceEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.reference.ReferenceIndexProvider;
import org.apache.jackrabbit.oak.plugins.name.NameValidatorProvider;
import org.apache.jackrabbit.oak.plugins.name.NamespaceEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider;
import org.apache.jackrabbit.oak.plugins.observation.ChangeCollectorProvider;
import org.apache.jackrabbit.oak.plugins.version.VersionHook;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.security.user.RandomAuthorizableNodeName;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authorization.AuthorizationConfiguration;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.oak.spi.security.user.AuthorizableNodeName;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
import org.apache.jackrabbit.oak.spi.whiteboard.DefaultWhiteboard;
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.packaging.VaultPackage;

/**
 * Entry point for OakPAL Acceptance Library. See {@link ProgressCheck} for the event listener interface.
 */
@ProviderType
public final class OakMachine {
    private static final ErrorListener DEFAULT_ERROR_LISTENER = new DefaultErrorListener();

    private final Packaging packagingService;

    private final List<ProgressCheck> progressChecks;

    private final ErrorListener errorListener;

    private final List<File> preInstallPackages;

    private final List<InitStage> initStages;

    private OakMachine(final Packaging packagingService,
                       final List<ProgressCheck> progressChecks,
                       final ErrorListener errorListener,
                       final List<File> preInstallPackages,
                       final List<InitStage> initStages) {
        this.packagingService = packagingService;
        this.progressChecks = progressChecks;
        this.errorListener = errorListener;
        this.preInstallPackages = preInstallPackages;
        this.initStages = initStages;
    }

    /**
     * Use the builder to construct the {@link OakMachine}.
     */
    public static class Builder {
        private Packaging packagingService;

        private final List<InitStage> initStages = new ArrayList<>();

        private final List<ProgressCheck> progressChecks = new ArrayList<>();

        private ErrorListener errorListener = DEFAULT_ERROR_LISTENER;

        private List<File> preInstallPackages = Collections.emptyList();


        /**
         * Provide a {@link Packaging} service for use in retrieving a {@link JcrPackageManager} for an admin session.
         * <p>
         * Currently only supported when using this in an OSGi context.
         *
         * @param packagingService a specific service to use, configured with alternative package registries.
         * @return my builder self
         */
        public Builder withPackagingService(final Packaging packagingService) {
            this.packagingService = packagingService;
            return this;
        }

        /**
         * Add a single instance of {@link InitStage} (or more) to the scan.
         *
         * @param initStage the init stage
         * @return my builder self
         */
        public Builder withInitStage(InitStage... initStage) {
            if (initStage != null) {
                return this.withInitStages(Arrays.asList(initStage));
            }
            return this;
        }

        /**
         * Add a list of {@link InitStage}s to the scan.
         *
         * @param initStages the list of init stages
         * @return my builder self
         */
        public Builder withInitStages(List<InitStage> initStages) {
            if (initStages != null) {
                this.initStages.addAll(initStages);
            }
            return this;
        }

        /**
         * Set a single instance of {@link ProgressCheck} for the scan.
         *
         * @param progressCheck the progress checks
         * @return my builder self
         */
        public Builder withProgressChecks(ProgressCheck... progressCheck) {
            if (progressCheck != null) {
                return this.withProgressChecks(Arrays.asList(progressCheck));
            }
            return this;
        }

        /**
         * Set the list of {@link ProgressCheck}s for the scan.
         *
         * @param progressChecks the list of packageListeners
         * @return my builder self
         */
        public Builder withProgressChecks(List<? extends ProgressCheck> progressChecks) {
            if (progressChecks != null) {
                this.progressChecks.addAll(progressChecks);
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
         * pre-install are not passed to each {@link ProgressCheck}, but errors raised are passed to
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
         * pre-install are not passed to each {@link ProgressCheck}, but errors raised are passed to
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
         * Construct a {@link OakMachine} from the {@link Builder} state.
         *
         * @return a {@link OakMachine}
         */
        public OakMachine build() {
            return new OakMachine(packagingService,
                    progressChecks,
                    errorListener,
                    preInstallPackages,
                    initStages);
        }
    }

    public List<ProgressCheck> getProgressChecks() {
        return progressChecks;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public List<File> getPreInstallPackages() {
        return preInstallPackages;
    }

    public List<CheckReport> scanPackage(File... file) throws AbortedScanException {
        if (file != null) {
            return scanPackages(Arrays.asList(file));
        } else {
            return scanPackages(Collections.emptyList());
        }
    }

    /**
     * Execute a scan by installing each of the provided package files in sequence. The scan proceeds in the following
     * order:
     * <ol>
     * <li>{@link #initRepository()} creates an fresh Oak repository.</li>
     * <li>{@link #loginAdmin(Repository)} opens an admin user JCR session.</li>
     * <li>{@link InitStage#initSession(Session, ErrorListener)} is called for each registered {@link InitStage}</li>
     * <li>{@link #processPackageFile(Session, JcrPackageManager, File, boolean)} is performed for each of the
     * {@link #preInstallPackages}</li>
     * <li>Each registered {@link ProgressCheck} receives a {@link ProgressCheck#startedScan()} event.</li>
     * <li>{@link #processPackageFile(Session, JcrPackageManager, File, boolean)} is performed for each of the elements
     * of the {@code files} array.</li>
     * <li>Each registered {@link ProgressCheck} receives a {@link ProgressCheck#finishedScan()} event.</li>
     * <li>The admin session is closed.</li>
     * <li>The repository is shutdown.</li>
     * </ol>
     *
     * @param files a list of FileVault content package files to be installed in sequence.
     * @return a list of any CheckReports reported during the scan.
     * @throws AbortedScanException for any errors that terminate the scan.
     */
    public List<CheckReport> scanPackages(List<File> files) throws AbortedScanException {
        Session admin = null;
        Repository scanRepo = null;
        try {
            getErrorListener().startedScan();

            scanRepo = initRepository();

            admin = loginAdmin(scanRepo);

            for (InitStage initStage : this.initStages) {
                initStage.initSession(admin, getErrorListener());
            }

            final JcrPackageManager manager;

            if (packagingService != null) {
                manager = packagingService.getPackageManager(admin);
            } else {
                manager = PackagingService.getPackageManager(admin);
            }

            for (File file : preInstallPackages) {
                processPackageFile(admin, manager, file, true);
            }

            progressChecks.forEach(ProgressCheck::startedScan);

            if (files != null) {
                for (File file : files) {
                    processPackageFile(admin, manager, file, false);
                }
            }
        } catch (RepositoryException e) {
            throw new AbortedScanException(e);
        } finally {
            progressChecks.forEach(ProgressCheck::finishedScan);

            if (admin != null) {
                admin.logout();
            }

            shutdownRepository(scanRepo);

            getErrorListener().finishedScan();
        }

        List<CheckReport> reports = new ArrayList<>();
        reports.add(SimpleReport.generateReport(getErrorListener()));

        List<CheckReport> listenerReports = progressChecks.stream()
                .map(SimpleReport::generateReport)
                .collect(Collectors.toList());

        reports.addAll(listenerReports);

        return Collections.unmodifiableList(reports);
    }

    private void processPackage(Session admin, JcrPackageManager manager, JcrPackage jcrPackage, final boolean preInstall)
            throws IOException, PackageException, RepositoryException {

        final PackageId packageId = jcrPackage.getPackage().getId();
        final SessionFacade inspectSession = new SessionFacade(admin, false);
        final ProgressTrackerListener tracker =
                new ImporterListenerAdapter(packageId, progressChecks, inspectSession, preInstall);

        ImportOptions options = new ImportOptions();
        options.setNonRecursive(true);
        options.setDependencyHandling(DependencyHandling.IGNORE);
        options.setListener(tracker);

        List<PackageId> subpacks = Arrays.asList(jcrPackage.extractSubpackages(options));

        final VaultPackage vaultPackage = jcrPackage.getPackage();
        if (!vaultPackage.isValid()) {
            throw new PackageException("Package is not valid: " + packageId);
        }

        if (!preInstall) {
            progressChecks.forEach(handler -> {
                try {
                    handler.beforeExtract(packageId, inspectSession,
                            vaultPackage.getProperties(), vaultPackage.getMetaInf(), subpacks);
                } catch (final RepositoryException e) {
                    getErrorListener().onListenerException(e, handler, packageId);
                }
            });
        }

        jcrPackage.extract(options);
        admin.save();

        jcrPackage.close();

        if (!preInstall) {
            progressChecks.forEach(handler -> {
                try {
                    handler.afterExtract(packageId, inspectSession);
                } catch (final RepositoryException e) {
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
                progressChecks.forEach(handler -> handler.identifySubpackage(packageId, parentId));
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
                progressChecks.forEach(handler -> {
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

        Oak oak = new Oak();
        Jcr jcr = new Jcr(oak, false);
        Repository repository = jcr
                .with(new SecurityProviderImpl(ConfigurationParameters.of(securityProps)))
                .with(new VersionHook())
                .with(new DefaultWhiteboard())
                .with(new InitialContent())
                .with(new NameValidatorProvider())
                .with(new NamespaceEditorProvider())
                .with(new TypeEditorProvider(true))
                .with(new ConflictValidatorProvider())
                .with(new ChangeCollectorProvider())
                .with(JcrConflictHandler.createJcrConflictHandler())
                .with(new ReferenceIndexProvider())
                .with(new PropertyIndexProvider())
                .with(new NodeTypeIndexProvider())
                .with(new PropertyIndexEditorProvider())
                .with(new ReferenceEditorProvider())
                .with(new OrderedPropertyIndexEditorProvider())
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

        private final List<ProgressCheck> handlers;

        private final SessionFacade session;

        private final boolean preInstall;

        ImporterListenerAdapter(PackageId packageId, List<ProgressCheck> handlers, SessionFacade session, boolean preInstall) {
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
                    handlers.forEach(handler -> {
                        try {
                            handler.deletedPath(packageId, path, session);
                        } catch (Exception e) {
                            OakMachine.this.getErrorListener().onListenerPathException(e, handler, packageId, path);
                        }
                    });
                } else {
                    try {
                        Node node = session.getNode(path);
                        handlers.forEach(handler -> {
                            try {
                                handler.importedPath(packageId, path, node);
                            } catch (Exception e) {
                                OakMachine.this.getErrorListener().onListenerPathException(e, handler, packageId, path);
                            }
                        });
                    } catch (RepositoryException e) {
                        OakMachine.this.getErrorListener().onImporterException(e, packageId, path);
                    }
                }
            }
        }

        @Override
        public void onError(Mode mode, String path, Exception e) {
            OakMachine.this.getErrorListener().onImporterException(e, packageId, path);
        }
    }
}
