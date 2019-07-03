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

import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.uncheckVoid1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthenticationConfiguration;
import org.apache.jackrabbit.oak.spi.security.authorization.AuthorizationConfiguration;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessorFactory;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for OakPAL Acceptance Library. See {@link ProgressCheck} for the event listener interface.
 */
public final class OakMachine {
    public static final String NS_URI_OAKPAL = "oakpaltmp";
    public static final String NS_PREFIX_OAKPAL = "oakpaltmp";
    public static final String LN_UNDECLARED = "Undeclared";
    public static final String NT_UNDECLARED = "{" + NS_URI_OAKPAL + "}" + LN_UNDECLARED;

    private static final ErrorListener DEFAULT_ERROR_LISTENER = new DefaultErrorListener();

    private final Packaging packagingService;

    private final List<ProgressCheck> progressChecks;

    private final ErrorListener errorListener;

    private final List<URL> preInstallUrls;

    private final List<InitStage> initStages;

    private final JcrCustomizer jcrCustomizer;

    private final InstallHookProcessorFactory installHookProcessorFactory;

    private final boolean skipInstallHooks;

    private OakMachine(final Packaging packagingService,
                       final List<ProgressCheck> progressChecks,
                       final ErrorListener errorListener,
                       final List<URL> preInstallUrls,
                       final List<InitStage> initStages,
                       final JcrCustomizer jcrCustomizer,
                       final InstallHookProcessorFactory installHookProcessorFactory,
                       final boolean skipInstallHooks) {
        this.packagingService = packagingService != null ? packagingService : new DefaultPackagingService();
        this.progressChecks = progressChecks;
        this.errorListener = errorListener;
        this.preInstallUrls = preInstallUrls;
        this.initStages = initStages;
        this.jcrCustomizer = jcrCustomizer;
        this.installHookProcessorFactory = installHookProcessorFactory;
        this.skipInstallHooks = skipInstallHooks;
    }

    /**
     * Use the builder to construct the {@link OakMachine}.
     */
    public static class Builder {
        private Packaging packagingService;

        private final List<InitStage> initStages = new ArrayList<>();

        private final List<ProgressCheck> progressChecks = new ArrayList<>();

        private ErrorListener errorListener = DEFAULT_ERROR_LISTENER;

        private List<URL> preInstallUrls = Collections.emptyList();

        private JcrCustomizer jcrCustomizer;

        private InstallHookProcessorFactory installHookProcessorFactory;

        private boolean skipInstallHooks;

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
         * @deprecated 1.3.1 pre-install packages are now handled as URIs
         */
        @Deprecated
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
         * @deprecated 1.3.1 pre-install packages are now handled as URIs
         */
        @Deprecated
        public Builder withPreInstallPackages(List<File> preInstallPackages) {
            if (preInstallPackages != null) {
                this.preInstallUrls = preInstallPackages.stream().map(uncheck1(File::toURL))
                        .collect(Collectors.toList());
            } else {
                this.preInstallUrls = Collections.emptyList();
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
        public Builder withPreInstallUrl(@NotNull URL... preInstallPackage) {
            return this.withPreInstallUrls(Arrays.asList(preInstallPackage));
        }

        /**
         * Provide a list of package files to install before each scan. Install events raised during
         * pre-install are not passed to each {@link ProgressCheck}, but errors raised are passed to
         * the {@link ErrorListener}.
         *
         * @param preInstallPackages the list of pre-install package files
         * @return my builder self
         */
        public Builder withPreInstallUrls(@NotNull List<URL> preInstallPackages) {
            this.preInstallUrls = new ArrayList<>(preInstallPackages);
            return this;
        }


        /**
         * Provide a function to customize the {@link Jcr} builder prior to the scan.
         *
         * @param jcrCustomizer the Jcr builder customization function
         * @return my builder self
         */
        public Builder withJcrCustomizer(final JcrCustomizer jcrCustomizer) {
            this.jcrCustomizer = jcrCustomizer;
            return this;
        }

        /**
         * Provide an {@link InstallHookProcessorFactory}.
         *
         * @param installHookProcessorFactory the factory
         * @return my builder self
         */
        public Builder withInstallHookProcessorFactory(final InstallHookProcessorFactory installHookProcessorFactory) {
            this.installHookProcessorFactory = installHookProcessorFactory;
            return this;
        }

        /**
         * Set to {@code true} to skip any install hook processing during the scan.
         *
         * @param skipInstallHooks true to skip install hooks
         * @return my builder self
         */
        public Builder withSkipInstallHooks(final boolean skipInstallHooks) {
            this.skipInstallHooks = skipInstallHooks;
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
                    preInstallUrls,
                    initStages,
                    jcrCustomizer,
                    installHookProcessorFactory,
                    skipInstallHooks);
        }
    }

    public List<ProgressCheck> getProgressChecks() {
        return progressChecks;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * Return the urls filtered and mapped back to files.
     *
     * @return the urls filtered and mapped back to files
     * @deprecated 1.3.1 pre-install packages are now handled as URIs. use {@link #getPreInstallUrls()}.
     */
    @Deprecated
    public List<File> getPreInstallFiles() {
        return preInstallUrls.stream()
                .map(uncheck1(URL::toURI))
                .map(File::new)
                .collect(Collectors.toList());
    }

    public List<URL> getPreInstallUrls() {
        return preInstallUrls;
    }

    public List<CheckReport> scanPackage(File... file) throws AbortedScanException {
        if (file != null) {
            return scanPackages(Arrays.asList(file));
        } else {
            return scanPackages(Collections.emptyList());
        }
    }

    /**
     * Functional interface for {@link #initAndInspect(InspectBody)}.
     */
    @FunctionalInterface
    public interface InspectBody<E extends Throwable> {
        void tryAccept(final Session session) throws E;
    }

    /**
     * Run arbitrary admin session logic against a post-InitStage OakPAL session.
     *
     * @param inspectBody arbitrary logic to run against a Session
     * @param <E>         an error type thrown by the inspectBody
     * @throws RepositoryException for repository errors
     * @throws E                   for any number of other reasons
     */
    public <E extends Throwable> void initAndInspect(final InspectBody<E> inspectBody) throws RepositoryException, E {
        Session admin = null;
        Repository scanRepo = null;
        try {
            scanRepo = initRepository();

            admin = loginAdmin(scanRepo);

            addOakpalTypes(admin);

            for (InitStage initStage : this.initStages) {
                initStage.initSession(admin, getErrorListener());
            }

            final Session inspectSession = Util.wrapSessionReadOnly(admin);

            inspectBody.tryAccept(inspectSession);
        } finally {
            if (admin != null) {
                admin.logout();
            }

            shutdownRepository(scanRepo);
        }
    }

    /**
     * Execute a scan by installing each of the provided package files in sequence. The scan proceeds in the following
     * order:
     * <ol>
     * <li>{@link #initRepository()} creates an fresh Oak repository.</li>
     * <li>{@link #loginAdmin(Repository)} opens an admin user JCR session.</li>
     * <li>{@link InitStage#initSession(Session, ErrorListener)} is called for each registered {@link InitStage}</li>
     * <li>{@link #processPackageFile(Session, JcrPackageManager, boolean, File)} is performed for each of the
     * {@link #preInstallUrls}</li>
     * <li>Each registered {@link ProgressCheck} receives a {@link ProgressCheck#startedScan()} event.</li>
     * <li>{@link #processPackageFile(Session, JcrPackageManager, boolean, File)} is performed for each of the elements
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
    public List<CheckReport> scanPackages(final List<File> files) throws AbortedScanException {
        getErrorListener().startedScan();

        Session admin = null;
        Repository scanRepo = null;
        try {
            scanRepo = initRepository();

            admin = loginAdmin(scanRepo);

            final JcrPackageManager manager = packagingService.getPackageManager(admin);

            addOakpalTypes(admin);

            for (InitStage initStage : this.initStages) {
                initStage.initSession(admin, getErrorListener());
            }

            for (URL url : preInstallUrls) {
                processPackageUrl(admin, manager, true, url);
            }

            progressChecks.forEach(ProgressCheck::startedScan);

            if (files != null) {
                for (File file : files) {
                    processPackageFile(admin, manager, false, file);
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

    private void addOakpalTypes(final Session admin) throws RepositoryException {
        admin.getWorkspace().getNamespaceRegistry().registerNamespace(NS_PREFIX_OAKPAL, NS_URI_OAKPAL);
        admin.setNamespacePrefix(NS_PREFIX_OAKPAL, NS_URI_OAKPAL);
        TemplateBuilderFactory builderFactory = new TemplateBuilderFactory(admin);
        builderFactory.setNamespace(NS_PREFIX_OAKPAL, NS_URI_OAKPAL);

        DefinitionBuilderFactory.AbstractNodeTypeDefinitionBuilder<NodeTypeTemplate> builder =
                builderFactory.newNodeTypeDefinitionBuilder();

        builder.setName(NT_UNDECLARED);
        builder.addSupertype(NodeTypeConstants.NT_FOLDER);

        DefinitionBuilderFactory.AbstractPropertyDefinitionBuilder<NodeTypeTemplate> multiDef =
                builder.newPropertyDefinitionBuilder();

        multiDef.setRequiredType(PropertyType.UNDEFINED);
        multiDef.setMultiple(true);
        multiDef.setOnParentVersion(OnParentVersionAction.IGNORE);
        multiDef.setName(NodeTypeConstants.RESIDUAL_NAME);
        multiDef.build();

        DefinitionBuilderFactory.AbstractPropertyDefinitionBuilder<NodeTypeTemplate> propDef =
                builder.newPropertyDefinitionBuilder();
        propDef.setRequiredType(PropertyType.UNDEFINED);
        propDef.setOnParentVersion(OnParentVersionAction.IGNORE);
        propDef.setName(NodeTypeConstants.RESIDUAL_NAME);
        propDef.build();

        DefinitionBuilderFactory.AbstractNodeDefinitionBuilder<NodeTypeTemplate> nodeDef =
                builder.newNodeDefinitionBuilder();

        nodeDef.addRequiredPrimaryType(NodeTypeConstants.NT_BASE);
        nodeDef.setDefaultPrimaryType(NT_UNDECLARED);
        nodeDef.setOnParentVersion(OnParentVersionAction.IGNORE);
        nodeDef.setName(NodeTypeConstants.RESIDUAL_NAME);
        nodeDef.setAllowsSameNameSiblings(true);
        nodeDef.build();

        admin.getWorkspace().getNodeTypeManager().registerNodeType(builder.build(), false);
    }

    private void processPackage(Session admin, JcrPackageManager manager, JcrPackage jcrPackage, final boolean preInstall)
            throws IOException, PackageException, RepositoryException {

        final PackageId packageId = jcrPackage.getPackage().getId();

        if (!preInstall) {
            Optional.ofNullable(jcrPackage.getData()).map(uncheck1(Property::getBinary)).ifPresent(
                    uncheckVoid1(binary -> {
                        try (InputStream input = binary.getStream();
                             JarInputStream jarInput = new JarInputStream(input)) {
                            final Manifest manifest = jarInput.getManifest();
                            if (manifest != null) {
                                progressChecks.forEach(handler ->
                                        handler.readManifest(packageId, new Manifest(manifest)));
                            }
                        }
                    }));
        }

        final Session inspectSession = Util.wrapSessionReadOnly(admin);
        final ProgressTrackerListener tracker =
                new ImporterListenerAdapter(packageId, progressChecks, inspectSession, preInstall);

        InternalImportOptions options = new InternalImportOptions();
        options.setNonRecursive(true);
        options.setDependencyHandling(DependencyHandling.IGNORE);
        options.setListener(tracker);
        options.setSkipInstallHooks(skipInstallHooks);
        options.setInstallHookProcessorFactoryDelegate(installHookProcessorFactory);

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
        try (JcrPackage jcrPackage = manager.open(packageId)) {

            if (!preInstall) {
                progressChecks.forEach(handler -> handler.identifySubpackage(packageId, parentId));
            }

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            getErrorListener().onSubpackageException(e, packageId);
        }
    }

    private void processUploadedPackage(final Session admin,
                                        final JcrPackageManager manager,
                                        final boolean preInstall,
                                        final JcrPackage jcrPackage) throws IOException, PackageException, RepositoryException {
        final VaultPackage vaultPackage = jcrPackage.getPackage();
        final PackageId packageId = vaultPackage.getId();
        final File packageFile = vaultPackage.getFile();

        if (!preInstall) {
            progressChecks.forEach(handler -> {
                try {
                    handler.identifyPackage(packageId, packageFile);
                } catch (Exception e) {
                    getErrorListener().onListenerException(e, handler, packageId);
                }
            });
        }

        processPackage(admin, manager, jcrPackage, preInstall);
    }

    private void processPackageUrl(final Session admin, final JcrPackageManager manager, final boolean preInstall, final URL url)
            throws AbortedScanException {
        try (InputStream input = url.openStream();
             JcrPackage jcrPackage = manager.upload(input, true, true)) {
            processUploadedPackage(admin, manager, preInstall, jcrPackage);
        } catch (IOException | PackageException | RepositoryException | Fun.FunRuntimeException e) {
            throw new AbortedScanException(e, url);
        }
    }

    private void processPackageFile(final Session admin, final JcrPackageManager manager, final boolean preInstall, final File file)
            throws AbortedScanException {

        try (JcrPackage jcrPackage = manager.upload(file, false, true, null, true)) {
            processUploadedPackage(admin, manager, preInstall, jcrPackage);
        } catch (IOException | PackageException | RepositoryException | Fun.FunRuntimeException e) {
            throw new AbortedScanException(e, file);
        }
    }

    @FunctionalInterface
    public interface JcrCustomizer {
        void customize(Jcr jcr);
    }

    private static NodeBuilder authzPath(final NodeBuilder parent, final String name) {
        NodeBuilder child = parent.child(name);
        child.setProperty(JcrConstants.JCR_PRIMARYTYPE, "rep:AuthorizableFolder", Type.NAME);
        child.setProperty(JcrConstants.JCR_MIXINTYPES, Collections.singleton("rep:AccessControllable"), Type.NAMES);
        return child;
    }

    private Repository initRepository() throws RepositoryException {
        final Jcr jcr = new Jcr();

        Properties userProps = new Properties();
        userProps.put(UserConstants.PARAM_USER_PATH, "/home/users");
        userProps.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
        userProps.put(AccessControlAction.USER_PRIVILEGE_NAMES, new String[]{PrivilegeConstants.JCR_ALL});
        userProps.put(AccessControlAction.GROUP_PRIVILEGE_NAMES, new String[]{PrivilegeConstants.JCR_READ});
        userProps.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);
        userProps.put("cacheExpiration", 3600 * 1000);

        Properties authzProps = new Properties();
        authzProps.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);

        Properties authnProps = new Properties();
        authnProps.put(AuthenticationConfiguration.PARAM_APP_NAME, "oakpal.jcr");

        Properties securityProps = new Properties();
        securityProps.put(UserConfiguration.NAME, ConfigurationParameters.of(userProps));
        securityProps.put(AuthorizationConfiguration.NAME, ConfigurationParameters.of(authzProps));
        securityProps.put(AuthenticationConfiguration.NAME, ConfigurationParameters.of(authnProps));

        jcr.with(new SecurityProviderImpl(ConfigurationParameters.of(securityProps)));

        RepositoryInitializer homeCreator = builder -> {
            NodeBuilder home = authzPath(builder, "home");
            NodeBuilder groups = authzPath(home, "groups");
            NodeBuilder users = authzPath(home, "users");
            NodeBuilder system = authzPath(users, "system");
        };

        jcr.with(homeCreator);

        if (jcrCustomizer != null) {
            jcrCustomizer.customize(jcr);
        }

        return jcr.withAtomicCounter().createRepository();
    }

    private void shutdownRepository(Repository repository) {
        if (repository instanceof JackrabbitRepository) {
            ((JackrabbitRepository) repository).shutdown();
        }
    }

    private Session loginAdmin(Repository repository) throws RepositoryException {
        final Thread thread = Thread.currentThread();
        final ClassLoader loader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(Oak.class.getClassLoader());
            return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    private class ImporterListenerAdapter implements ProgressTrackerListener {
        private final PackageId packageId;

        private final List<ProgressCheck> handlers;

        private final Session session;

        private final boolean preInstall;

        ImporterListenerAdapter(PackageId packageId, List<ProgressCheck> handlers, Session session, boolean preInstall) {
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
