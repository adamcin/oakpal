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

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.core.installable.JcrInstallWatcher;
import net.adamcin.oakpal.core.installable.NoopInstallWatcher;
import net.adamcin.oakpal.core.installable.PathInstallable;
import net.adamcin.oakpal.core.installable.RepoInitInstallable;
import net.adamcin.oakpal.core.installable.SubpackageInstallable;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.security.internal.SecurityProviderBuilder;
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
import org.apache.jackrabbit.oak.spi.state.NodeStore;
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
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.jetbrains.annotations.NotNull;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.version.OnParentVersionAction;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.constantly1;
import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.Fun.uncheckVoid1;
import static net.adamcin.oakpal.core.repoinit.DefaultRepoInitFactory.newDefaultRepoInitProcessor;

/**
 * Entry point for OakPAL Acceptance Library. See {@link ProgressCheck} for the event listener interface.
 */
public final class OakMachine {
    public static final String NS_URI_OAKPAL = "oakpaltmp";
    public static final String NS_PREFIX_OAKPAL = "oakpaltmp";
    public static final String LN_UNDECLARED = "Undeclared";
    public static final String NT_UNDECLARED = "{" + NS_URI_OAKPAL + "}" + LN_UNDECLARED;

    private final Packaging packagingService;

    private final List<ProgressCheck> progressChecks;

    private final ErrorListener errorListener;

    private final List<URL> preInstallUrls;

    private final List<InitStage> initStages;

    private final JcrCustomizer jcrCustomizer;

    private final InstallHookProcessorFactory installHookProcessorFactory;

    private final ClassLoader installHookClassLoader;

    private final boolean enablePreInstallHooks;

    private final InstallHookPolicy scanInstallHookPolicy;

    private final Supplier<NodeStore> nodeStoreSupplier;

    private final SubpackageSilencer subpackageSilencer;

    private final RepoInitProcessor repoInitProcessor;

    private final JcrInstallWatcher installWatcher;

    private OakMachine(final Packaging packagingService,
                       final List<ProgressCheck> progressChecks,
                       final ErrorListener errorListener,
                       final List<URL> preInstallUrls,
                       final List<InitStage> initStages,
                       final JcrCustomizer jcrCustomizer,
                       final InstallHookProcessorFactory installHookProcessorFactory,
                       final ClassLoader installHookClassLoader,
                       final boolean enablePreInstallHooks,
                       final InstallHookPolicy scanInstallHookPolicy,
                       final Supplier<NodeStore> nodeStoreSupplier,
                       final SubpackageSilencer subpackageSilencer,
                       final RepoInitProcessor repoInitProcessor,
                       final JcrInstallWatcher installWatcher) {
        this.packagingService = packagingService != null ? packagingService : newOakpalPackagingService();
        this.progressChecks = progressChecks;
        this.errorListener = errorListener;
        this.preInstallUrls = preInstallUrls;
        this.initStages = initStages;
        this.jcrCustomizer = jcrCustomizer;
        this.installHookProcessorFactory = installHookProcessorFactory;
        this.installHookClassLoader = installHookClassLoader;
        this.enablePreInstallHooks = enablePreInstallHooks;
        this.scanInstallHookPolicy = scanInstallHookPolicy;
        this.nodeStoreSupplier = nodeStoreSupplier != null ? nodeStoreSupplier : MemoryNodeStore::new;
        this.subpackageSilencer = subpackageSilencer != null ? subpackageSilencer : (packageId, parentId) -> false;
        this.repoInitProcessor = repoInitProcessor != null
                ? repoInitProcessor
                : newDefaultRepoInitProcessor(Util.getDefaultClassLoader());
        this.installWatcher = installWatcher != null ? installWatcher : NoopInstallWatcher.instance();
    }

    /**
     * Functional interface for a repoinit processor that unifies the RepoInitParser and JcrRepoInitOpsProcessor
     * signatures.
     */
    @FunctionalInterface
    public interface RepoInitProcessor {
        void apply(Session admin, Reader repoInitReader) throws RepoInitParsingException;
    }

    /**
     * Use the builder to construct the {@link OakMachine}.
     */
    public static class Builder {
        private Packaging packagingService;

        private final List<InitStage> initStages = new ArrayList<>();

        private final List<ProgressCheck> progressChecks = new ArrayList<>();

        private ErrorListener errorListener = new DefaultErrorListener();

        private List<URL> preInstallUrls = Collections.emptyList();

        private JcrCustomizer jcrCustomizer;

        private ClassLoader installHookClassLoader;

        private InstallHookProcessorFactory installHookProcessorFactory;

        private boolean enablePreInstallHooks;

        private InstallHookPolicy scanInstallHookPolicy;

        private Supplier<NodeStore> nodeStoreSupplier;

        private SubpackageSilencer subpackageSilencer;

        private RepoInitProcessor repoInitProcesser;

        private JcrInstallWatcher installWatcher;

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
        public Builder withProgressCheck(final @NotNull ProgressCheck... progressCheck) {
            return this.withProgressChecks(Arrays.asList(progressCheck));
        }

        /**
         * Set a single instance of {@link ProgressCheck} for the scan.
         *
         * @param progressCheck the progress checks
         * @return my builder self
         * @deprecated 1.4.0 use {@link #withProgressCheck(ProgressCheck...)} instead.
         */
        @Deprecated
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
                this.errorListener = new DefaultErrorListener();
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
         * Provide a classloader to
         * {@link org.apache.jackrabbit.vault.fs.io.ImportOptions#setHookClassLoader(ClassLoader)}.
         *
         * @param classLoader the classloader to use for loading hooks
         * @return my builder self
         */
        public Builder withInstallHookClassLoader(final ClassLoader classLoader) {
            this.installHookClassLoader = classLoader;
            return this;
        }

        /**
         * Set to {@code true} to enable pre-install package install hooks for the scan.
         *
         * @param enablePreInstallHooks true to enable pre-install package install hooks
         * @return my builder self
         */
        public Builder withEnablePreInstallHooks(final boolean enablePreInstallHooks) {
            this.enablePreInstallHooks = enablePreInstallHooks;
            return this;
        }

        /**
         * Specify an InstallHookPolicy for the scan.
         *
         * @param installHookPolicy the InstallHookPolicy to enforce
         * @return my builder self
         */
        public Builder withInstallHookPolicy(final InstallHookPolicy installHookPolicy) {
            this.scanInstallHookPolicy = installHookPolicy;
            return this;
        }

        /**
         * Specify a supplier that will produce a {@link NodeStore} for the scan. By default,
         * {@link MemoryNodeStore#MemoryNodeStore()} will be used (e.g. {@code MemoryNodeStore::new}).
         * <p>
         * Note: OakMachine will call {@link Supplier#get} for every execution of {@link #scanPackage(File...)}.
         * Beyond the call to this supplier function, it is the client's responsibility to manage the external
         * NodeStore's state between scans when using the same {@link OakMachine} instance.
         *
         * @param nodeStoreSupplier the NodeStore
         * @return my builder self
         */
        public Builder withNodeStoreSupplier(final Supplier<NodeStore> nodeStoreSupplier) {
            this.nodeStoreSupplier = nodeStoreSupplier;
            return this;
        }

        /**
         * Provide a predicate taking the subpackage PackageId as the first argument and the parent  PackageId as the
         * second argument, returning true if events for the subpackage and any of ITS subpackages should be silenced
         * during the scan.
         *
         * @param subpackageSilencer a predicate taking the subpackage PackageId as the first argument and the parent
         *                           PackageId as the second argument, returning true if events for the subpackage and
         *                           any of ITS subpackages should be silenced during the scan.
         * @return my builder self
         */
        public Builder withSubpackageSilencer(final SubpackageSilencer subpackageSilencer) {
            this.subpackageSilencer = subpackageSilencer;
            return this;
        }

        /**
         * Provide a RepoInitProcessor.
         *
         * @param repoInitProcesser the repoinit processor
         * @return my builder self
         * @since 2.1.0
         */
        public Builder withRepoInitProcesser(final RepoInitProcessor repoInitProcesser) {
            this.repoInitProcesser = repoInitProcesser;
            return this;
        }

        /**
         * Provide an install watcher.
         *
         * @param installWatcher the jcr install watcher
         * @return my builder self
         * @since 2.1.0
         */
        public Builder withInstallWatcher(final JcrInstallWatcher installWatcher) {
            this.installWatcher = installWatcher;
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
                    installHookClassLoader != null ? installHookClassLoader : Util.getDefaultClassLoader(),
                    enablePreInstallHooks,
                    scanInstallHookPolicy,
                    nodeStoreSupplier,
                    subpackageSilencer,
                    repoInitProcesser,
                    installWatcher);
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

    /**
     * Return the urls for preinstall packages.
     *
     * @return the urls for preinstall packages
     */
    public List<URL> getPreInstallUrls() {
        return preInstallUrls;
    }

    /**
     * Functional interface for {@link #initAndInspect(InspectBody)}.
     */
    @FunctionalInterface
    public interface InspectBody<E extends Throwable> {
        void tryAccept(final Session session) throws E;
    }

    /**
     * Functional interface for {@code Builder.withSubpackageSilencer(SubpackageSilencer)}.
     */
    @FunctionalInterface
    public interface SubpackageSilencer extends BiPredicate<PackageId, PackageId> {
        boolean test(final PackageId subpackageId, final PackageId parentId);
    }

    /**
     * Run arbitrary read-only session logic against a post-InitStage OakPAL session.
     *
     * @param inspectBody arbitrary logic to run against a Session
     * @param <E>         an error type thrown by the inspectBody
     * @throws AbortedScanException for preinstall errors
     * @throws RepositoryException  for repository errors
     * @throws E                    for any number of other reasons
     */
    @SuppressWarnings("WeakerAccess")
    public <E extends Throwable> void initAndInspect(final InspectBody<E> inspectBody)
            throws AbortedScanException, RepositoryException, E {
        adminInitAndInspect(admin -> {
            final Session inspectSession = Util.wrapSessionReadOnly(admin);
            inspectBody.tryAccept(inspectSession);
        });
    }

    /**
     * Run arbitrary admin session logic against a post-InitStage OakPAL session.
     *
     * @param inspectBody arbitrary logic to run against a Session
     * @param <E>         an error type thrown by the inspectBody
     * @throws AbortedScanException for preinstall errors
     * @throws RepositoryException  for repository errors
     * @throws E                    for any number of other reasons
     */
    public <E extends Throwable> void adminInitAndInspect(final InspectBody<E> inspectBody)
            throws AbortedScanException, RepositoryException, E {
        Session admin = null;
        Repository scanRepo = null;
        try {
            scanRepo = initRepository();
            admin = loginAdmin(scanRepo);
            addOakpalTypes(admin);

            final JcrPackageManager manager = packagingService.getPackageManager(admin);

            for (InitStage initStage : this.initStages) {
                initStage.initSession(admin, getErrorListener(), repoInitProcessor);
            }

            for (final URL url : preInstallUrls) {
                processPackageUrl(admin, manager, true, url);
            }

            inspectBody.tryAccept(admin);
        } finally {
            if (admin != null) {
                admin.logout();
            }

            shutdownRepository(scanRepo);
        }
    }

    /**
     * Perform a scan of the provided package file or files.
     *
     * @param file the file or files to scan
     * @return a list of check reports
     * @throws AbortedScanException if the scan was aborted because of an unrecoverable exception
     * @see #scanPackages(List) this is an alias for the plurally-named method
     */
    public List<CheckReport> scanPackage(final @NotNull File... file) throws AbortedScanException {
        return scanPackages(Arrays.asList(file));
    }

    /**
     * Execute a scan by installing each of the provided package files in sequence. The scan proceeds in the following
     * order:
     * <ol>
     * <li>{@link #initRepository()} creates an fresh Oak repository.</li>
     * <li>{@link #loginAdmin(Repository)} opens an admin user JCR session.</li>
     * <li>{@code InitStage.initSession(Session, ErrorListener, RepoInitProcessor)} is called for each registered {@link InitStage}</li>
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

            addOakpalTypes(admin);

            final JcrPackageManager manager = packagingService.getPackageManager(admin);

            for (final InitStage initStage : this.initStages) {
                initStage.initSession(admin, getErrorListener(), repoInitProcessor);
            }

            installWatcher.startedScan();
            progressChecks.forEach(ProgressCheck::startedScan);

            for (final URL url : preInstallUrls) {
                processPackageUrl(admin, manager, true, url);
            }

            if (files != null) {
                for (final File file : files) {
                    processPackageFile(admin, manager, false, file);
                }
            }

        } catch (RepositoryException e) {
            throw new AbortedScanException(e);
        } finally {
            installWatcher.finishedScan();
            progressChecks.forEach(ProgressCheck::finishedScan);

            if (admin != null) {
                admin.logout();
            }

            shutdownRepository(scanRepo);

            getErrorListener().finishedScan();
        }

        List<CheckReport> reports = new ArrayList<>();
        reports.add(SimpleReport.generateReport(getErrorListener()));
        reports.add(SimpleReport.generateReport(installWatcher));
        List<CheckReport> listenerReports = progressChecks.stream()
                .map(SimpleReport::generateReport)
                .collect(Collectors.toList());

        reports.addAll(listenerReports);

        return Collections.unmodifiableList(reports);
    }

    private void addOakpalTypes(final Session admin) throws RepositoryException {
        this.installVltNodetypes(admin);
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
        Optional.ofNullable(jcrPackage.getData()).map(uncheck1(Property::getBinary)).ifPresent(
                uncheckVoid1(binary -> {
                    try (InputStream input = binary.getStream();
                         JarInputStream jarInput = new JarInputStream(input)) {
                        final Manifest manifest = jarInput.getManifest();
                        if (manifest != null) {
                            propagateCheckPackageEvent(preInstall, packageId,
                                    handler -> handler.readManifest(packageId, new Manifest(manifest)));
                        }
                    }
                }));

        final Session inspectSession = Util.wrapSessionReadOnly(admin);
        final ProgressTrackerListener tracker =
                new ImporterListenerAdapter(packageId, inspectSession, preInstall);

        InternalImportOptions options = new InternalImportOptions(packageId, Packaging.class.getClassLoader());
        options.setNonRecursive(true);
        options.setDependencyHandling(DependencyHandling.IGNORE);
        options.setListener(tracker);
        options.setInstallHookProcessorFactoryDelegate(installHookProcessorFactory);
        options.setHookClassLoader(installHookClassLoader);
        options.setViolationReporter(errorListener);
        // we default to disabling install hooks for preinstall packages, since preinstall packages are
        // 1) more likely to come off-the-shelf, targeting a larger application's class path
        // 2) not the subject of an oakpal scan, and thus primarily valuable for the packaged content, not for hook behavior
        if (preInstall) {
            options.setInstallHookPolicy(enablePreInstallHooks
                    ? InstallHookPolicy.ABORT
                    : InstallHookPolicy.SKIP);
        } else {
            options.setInstallHookPolicy(scanInstallHookPolicy);
        }

        List<PackageId> subpacks = Arrays.asList(jcrPackage.extractSubpackages(options));

        final VaultPackage vaultPackage = jcrPackage.getPackage();
        if (!vaultPackage.isValid()) {
            throw new PackageException("Package is not valid: " + packageId);
        }

        propagateCheckPackageEvent(preInstall, packageId, handler ->
                handler.beforeExtract(packageId, inspectSession, vaultPackage.getProperties(),
                        vaultPackage.getMetaInf(), subpacks));

        jcrPackage.extract(options);
        admin.save();

        jcrPackage.close();

        propagateCheckPackageEvent(preInstall, packageId, handler -> handler.afterExtract(packageId, inspectSession));

        for (PackageId subpackId : subpacks) {
            processSubpackage(admin, manager, subpackId, packageId,
                    preInstall || subpackageSilencer.test(subpackId, packageId));
        }
    }

    static Consumer<ProgressCheck>
    newProgressCheckEventConsumer(final boolean silenced,
                                  final @NotNull Fun.ThrowingConsumer<ProgressCheck> checkVisitor,
                                  final @NotNull BiConsumer<ProgressCheck, Exception> onError) {
        Consumer<ProgressCheck> consumer = check -> {
            try {
                if (silenced && check instanceof SilenceableViolationReporter) {
                    ((SilenceableViolationReporter) check).setSilenced(silenced);
                }
                if (!silenced || check instanceof SilenceableViolationReporter) {
                    checkVisitor.tryAccept(check);
                }
            } catch (final Exception e) {
                if (!silenced) {
                    onError.accept(check, e);
                }
            }
        };
        return consumer.andThen(check -> {
            if (silenced && check instanceof SilenceableViolationReporter) {
                ((SilenceableViolationReporter) check).setSilenced(false);
            }
        });
    }

    final void propagateCheckPackageEvent(final boolean silenced,
                                          final @NotNull PackageId packageId,
                                          final @NotNull Fun.ThrowingConsumer<ProgressCheck> checkVisitor) {
        final Consumer<ProgressCheck> checkConsumer = newProgressCheckEventConsumer(silenced, checkVisitor,
                (check, error) -> getErrorListener().onListenerException(error, check, packageId));
        checkConsumer.accept(installWatcher);
        progressChecks.forEach(checkConsumer);
    }

    final void propagateCheckPathEvent(final boolean silenced,
                                       final @NotNull PackageId packageId,
                                       final @NotNull String path,
                                       final @NotNull Fun.ThrowingConsumer<ProgressCheck> checkVisitor) {
        final Consumer<ProgressCheck> checkConsumer = newProgressCheckEventConsumer(silenced, checkVisitor,
                (check, error) -> getErrorListener().onListenerPathException(error, check, packageId, path));
        checkConsumer.accept(installWatcher);
        progressChecks.forEach(checkConsumer);
    }

    final void internalProcessSubpackage(final @NotNull Session admin,
                                         final @NotNull JcrPackageManager manager,
                                         final @NotNull PackageId packageId,
                                         final @NotNull PackageId parentId,
                                         final boolean preInstall,
                                         final @NotNull Fun.ThrowingSupplier<JcrPackage> jcrPackageSupplier,
                                         final @NotNull Function<JcrPackage, String> jcrPathFn,
                                         final @NotNull Consumer<Exception> onError) throws RepositoryException {
        try (JcrPackage jcrPackage = jcrPackageSupplier.tryGet()) {
            if (jcrPackage != null) {
                propagateCheckPackageEvent(preInstall, packageId, progressCheck ->
                        progressCheck.identifySubpackage(packageId, parentId, jcrPathFn.apply(jcrPackage)));

                processPackage(admin, manager, jcrPackage, preInstall);
            } else {
                throw new PackageException("JcrPackageManager returned null package");
            }
        } catch (IOException | PackageException | RepositoryException e) {
            onError.accept(e);
            admin.refresh(false);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    final void processSubpackage(final @NotNull Session admin,
                                 final @NotNull JcrPackageManager manager,
                                 final @NotNull PackageId packageId,
                                 final @NotNull PackageId parentId,
                                 final boolean preInstall) throws RepositoryException {
        internalProcessSubpackage(admin, manager, packageId, parentId, preInstall,
                () -> manager.open(packageId),
                jcrPackage -> Optional.ofNullable(jcrPackage.getNode())
                        .flatMap(compose1(result1(Node::getPath), Result::toOptional))
                        // use the deprecated installation path + .zip extension as last default
                        .orElse(packageId.getInstallationPath() + ".zip"),
                error -> getErrorListener().onSubpackageException(error, packageId));
    }

    final void processSubpackageInstallable(final @NotNull Session admin,
                                            final @NotNull JcrPackageManager manager,
                                            final @NotNull PackageId lastPackage,
                                            final @NotNull SubpackageInstallable installable,
                                            final boolean preInstall) throws RepositoryException {
        final Consumer<Exception> onError =
                error -> getErrorListener().onInstallableSubpackageError(error, lastPackage, installable);
        Optional<Fun.ThrowingSupplier<JcrPackage>> supplierOptional = installWatcher.openSubpackageInstallable(installable, admin, manager);
        if (supplierOptional.isPresent()) {
            internalProcessSubpackage(admin, manager, installable.getSubpackageId(), installable.getParentId(),
                    preInstall, supplierOptional.get(), constantly1(installable::getJcrPath), onError);
        }
    }

    private void processUploadedPackage(final @NotNull Session admin,
                                        final @NotNull JcrPackageManager manager,
                                        final boolean preInstall,
                                        final @NotNull JcrPackage jcrPackage)
            throws IOException, PackageException, RepositoryException {
        final VaultPackage vaultPackage = jcrPackage.getPackage();
        final PackageId packageId = vaultPackage.getId();
        final File packageFile = vaultPackage.getFile();
        propagateCheckPackageEvent(preInstall, packageId, handler -> handler.identifyPackage(packageId, packageFile));
        processPackage(admin, manager, jcrPackage, preInstall);
        processInstallableQueue(admin, manager, preInstall, packageId);
    }

    void processInstallableQueue(final @NotNull Session admin,
                                 final @NotNull JcrPackageManager manager,
                                 final boolean preInstall,
                                 final @NotNull PackageId lastUploadedPackage) throws RepositoryException {
        PathInstallable installable = installWatcher.dequeueInstallable();
        while (installable != null) {
            if (installable instanceof RepoInitInstallable) {
                for (final Fun.ThrowingSupplier<Reader> readerSupplier :
                        installWatcher.openRepoInitInstallable((RepoInitInstallable) installable, admin)) {
                    try (Reader reader = readerSupplier.tryGet()) {
                        repoInitProcessor.apply(admin, reader);
                    } catch (final Exception e) {
                        getErrorListener().onInstallableRepoInitError(e, lastUploadedPackage,
                                (RepoInitInstallable) installable);
                    }
                }
            } else if (installable instanceof SubpackageInstallable) {
                processSubpackageInstallable(admin, manager, lastUploadedPackage,
                        (SubpackageInstallable) installable, preInstall);
            }
            // do this at the end of the while scope, obviously.
            installable = installWatcher.dequeueInstallable();
        }
    }

    final void processPackageUrl(final @NotNull Session admin,
                                 final @NotNull JcrPackageManager manager,
                                 final boolean preInstall,
                                 final @NotNull URL url)
            throws AbortedScanException {
        try {
            admin.refresh(false);
        } catch (final RepositoryException e) {
            throw new AbortedScanException(e);
        }

        try (InputStream input = url.openStream();
             JcrPackage jcrPackage = manager.upload(input, true, true)) {
            processUploadedPackage(admin, manager, preInstall, jcrPackage);
        } catch (IOException | PackageException | RepositoryException | Fun.FunRuntimeException e) {
            throw new AbortedScanException(e, url);
        }
    }

    final void processPackageFile(final @NotNull Session admin,
                                  final @NotNull JcrPackageManager manager,
                                  final boolean preInstall,
                                  final @NotNull File file)
            throws AbortedScanException {
        try {
            admin.refresh(false);
        } catch (final RepositoryException e) {
            throw new AbortedScanException(e);
        }

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
        final NodeStore nodeStore = nodeStoreSupplier.get();
        final Oak oak = nodeStore == null ? new Oak() : new Oak(nodeStore);
        final Jcr jcr = new Jcr(oak);

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

        jcr.with(SecurityProviderBuilder.newBuilder()
                .with(ConfigurationParameters.of(securityProps))
                .build());

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

    private void installVltNodetypes(final Session admin) throws RepositoryException {
        new CNDURLInstaller(getErrorListener(),
                Collections.emptyList(),
                Collections.singletonList(JcrPackageManager.class.getResource(
                        "impl/nodetypes.cnd")))
                .register(admin);
    }

    final class ImporterListenerAdapter implements ProgressTrackerListener {
        private final PackageId packageId;

        private final Session session;

        private final boolean silenced;

        ImporterListenerAdapter(PackageId packageId, Session session, boolean silenced) {
            this.packageId = packageId;
            this.session = session;
            this.silenced = silenced;
        }

        @Override
        public void onMessage(Mode mode, String action, String path) {
            // NOP("-"), MOD("U"), REP("R"), ERR("E"), ADD("A"), DEL("D"), MIS("!")
            if (path != null && path.startsWith("/")) {
                if ("D".equals(action)) { // deleted
                    propagateCheckPathEvent(silenced, packageId, path, check -> check.deletedPath(packageId, path, session));
                } else if ("ARU-".contains(action)) { // added, replaced, updated
                    try {
                        Node node = session.getNode(path);
                        propagateCheckPathEvent(silenced, packageId, path, check ->
                                check.importedPath(packageId, path, node, PathAction.fromShortCode(action)));
                    } catch (RepositoryException e) {
                        if (!silenced) {
                            getErrorListener().onImporterException(e, packageId, path);
                        }
                    }
                } else if ("E".equals(action)) {
                    if (!silenced) {
                        onError(mode, path, new RuntimeException("Unknown error"));
                    }
                }
            }
        }

        @Override
        public void onError(Mode mode, String path, Exception e) {
            if (!silenced) {
                getErrorListener().onImporterException(e, packageId, path);
            }
        }
    }

    public static Packaging newOakpalPackagingService() {
        return new DefaultPackagingService();
    }

    public static Packaging newOakpalPackagingService(final @NotNull ClassLoader classLoader) {
        return new DefaultPackagingService(classLoader);
    }
}
