/*
 * Copyright 2019 Mark Adamcin
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
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathMapping;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessor;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessorFactory;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalImportOptions extends ImportOptions implements InstallHookProcessorFactory {
    private static final String INSTALL_HOOK_PROCESSOR_IMPL_CLASS =
            "org.apache.jackrabbit.vault.packaging.impl.InstallHookProcessorImpl";
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalImportOptions.class);
    static final InstallHookProcessor NOOP_INSTALL_HOOK_PROCESSOR = new InstallHookProcessor() {
        @Override
        public void registerHooks(Archive archive, ClassLoader classLoader) {
        }

        @Override
        public void registerHook(VaultInputSource vaultInputSource, ClassLoader classLoader) {
        }

        @Override
        public boolean hasHooks() {
            return false;
        }

        @Override
        public boolean execute(InstallContext installContext) {
            return true;
        }
    };

    private final PackageId packageId;
    private final ImportOptions optionsDelegate;
    private final Class<? extends InstallHookProcessor> internalInstallHookProcessorClazz;

    private InstallHookProcessorFactory installHookProcessorFactoryDelegate;
    private InstallHookPolicy installHookPolicy;
    private ErrorListener violationReporter;

    InternalImportOptions(final @NotNull PackageId packageId) {
        Class<? extends InstallHookProcessor> clazz = null;
        try {
            clazz = Packaging.class.getClassLoader()
                    .loadClass(INSTALL_HOOK_PROCESSOR_IMPL_CLASS).asSubclass(InstallHookProcessor.class);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Failed to load internal InstallHookProcessorImpl from Packaging classloader. Will default to " +
                    "noop if an InstallHookProcessorFactory delegate is not provided.", e);
        }
        this.packageId = packageId;
        this.internalInstallHookProcessorClazz = clazz;
        this.optionsDelegate = new ImportOptions();
    }

    private InternalImportOptions(final @NotNull PackageId packageId,
                                  final @Nullable ImportOptions optionsDelegate,
                                  final @Nullable Class<? extends InstallHookProcessor> internalInstallHookProcessorClazz) {
        this.packageId = packageId;
        this.optionsDelegate = optionsDelegate != null ? optionsDelegate : new ImportOptions();
        this.internalInstallHookProcessorClazz = internalInstallHookProcessorClazz;
    }

    @Override
    public InstallHookProcessor createInstallHookProcessor() {
        if (!isSkip()) {
            if (installHookProcessorFactoryDelegate != null) {
                return new InstallHookProcessorWrapper(packageId, installHookPolicy, violationReporter,
                        installHookProcessorFactoryDelegate.createInstallHookProcessor());
            } else if (internalInstallHookProcessorClazz != null) {
                try {
                    return new InstallHookProcessorWrapper(packageId, installHookPolicy, violationReporter,
                            internalInstallHookProcessorClazz.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    LOGGER.error("failed to create instance of wrapped processor class", e);
                }
            }
        }
        return NOOP_INSTALL_HOOK_PROCESSOR;
    }

    private boolean isSkip() {
        return installHookPolicy == InstallHookPolicy.SKIP;
    }

    @Override
    public ImportOptions copy() {
        InternalImportOptions options = new InternalImportOptions(packageId, optionsDelegate.copy(),
                internalInstallHookProcessorClazz);
        options.setInstallHookProcessorFactoryDelegate(this.installHookProcessorFactoryDelegate);
        options.setInstallHookPolicy(installHookPolicy);
        options.setViolationReporter(violationReporter);
        return options;
    }

    public InstallHookProcessorFactory getInstallHookProcessorFactoryDelegate() {
        return installHookProcessorFactoryDelegate;
    }

    public void setInstallHookProcessorFactoryDelegate(final InstallHookProcessorFactory installHookProcessorFactoryDelegate) {
        this.installHookProcessorFactoryDelegate = installHookProcessorFactoryDelegate;
    }

    public InstallHookPolicy getInstallHookPolicy() {
        return installHookPolicy;
    }

    public void setInstallHookPolicy(final InstallHookPolicy installHookPolicy) {
        this.installHookPolicy = installHookPolicy;
    }

    public ErrorListener getViolationReporter() {
        return violationReporter;
    }

    public void setViolationReporter(final ErrorListener violationReporter) {
        this.violationReporter = violationReporter;
    }

    static final class InstallHookProcessorWrapper implements InstallHookProcessor {
        private final PackageId packageId;
        private final InstallHookPolicy policy;
        private final ErrorListener reporter;
        private final InstallHookProcessor wrapped;

        InstallHookProcessorWrapper(final PackageId packageId,
                                    final InstallHookPolicy policy,
                                    final ErrorListener reporter,
                                    final InstallHookProcessor wrapped) {
            this.packageId = packageId;
            this.policy = policy;
            this.reporter = reporter;
            this.wrapped = wrapped;
        }

        @Override
        public void registerHooks(final Archive archive, final ClassLoader classLoader) throws PackageException {
            try {
                try {
                    this.wrapped.registerHooks(archive, classLoader);
                } catch (final LinkageError error) {
                    throw new PackageException("Uncaught linkage error: " + error.getMessage(), error);
                }
            } catch (final PackageException e) {
                if (getPolicy() == InstallHookPolicy.ABORT) {
                    throw e;
                } else {
                    reporter.onInstallHookError(e, packageId);
                }
            }
            if (getPolicy() == InstallHookPolicy.PROHIBIT) {
                reporter.onProhibitedInstallHookRegistration(packageId);
            }
        }

        @Override
        public void registerHook(final VaultInputSource input, final ClassLoader classLoader) throws IOException, PackageException {
            final String systemId = input.getSystemId() != null ? input.getSystemId() : "";
            try {
                this.wrapped.registerHook(input, classLoader);
            } catch (final LinkageError error) {
                final PackageException throwable =
                        new PackageException("Uncaught linkage error (systemId=" + systemId + "): " + error.getMessage(),
                                error);
                if (getPolicy() == InstallHookPolicy.ABORT) {
                    throw throwable;
                } else {
                    reporter.onInstallHookError(throwable, packageId);
                }
            }
        }

        @Override
        public boolean hasHooks() {
            if (getPolicy() == InstallHookPolicy.PROHIBIT) {
                return false;
            } else {
                return this.wrapped.hasHooks();
            }
        }

        @Override
        public boolean execute(final InstallContext context) {
            if (getPolicy() == InstallHookPolicy.PROHIBIT) {
                return true;
            } else {
                return this.wrapped.execute(context);
            }
        }

        private InstallHookPolicy getPolicy() {
            return Optional.ofNullable(policy).orElse(InstallHookPolicy.DEFAULT);
        }
    }

    @Override
    public boolean isStrict() {
        return optionsDelegate.isStrict();
    }

    @Override
    public void setStrict(final boolean strict) {
        optionsDelegate.setStrict(strict);
    }

    @Override
    public ProgressTrackerListener getListener() {
        return optionsDelegate.getListener();
    }

    @Override
    public void setListener(final ProgressTrackerListener listener) {
        optionsDelegate.setListener(listener);
    }

    @Override
    public String getPatchParentPath() {
        return optionsDelegate.getPatchParentPath();
    }

    @Override
    public void setPatchParentPath(final String patchParentPath) {
        optionsDelegate.setPatchParentPath(patchParentPath);
    }

    @Override
    public File getPatchDirectory() {
        return optionsDelegate.getPatchDirectory();
    }

    @Override
    public void setPatchDirectory(final File patchDirectory) throws IOException {
        optionsDelegate.setPatchDirectory(patchDirectory);
    }

    @Override
    public boolean isPatchKeepInRepo() {
        return optionsDelegate.isPatchKeepInRepo();
    }

    @Override
    public void setPatchKeepInRepo(final boolean patchKeepInRepo) {
        optionsDelegate.setPatchKeepInRepo(patchKeepInRepo);
    }

    @Override
    public AccessControlHandling getAccessControlHandling() {
        return optionsDelegate.getAccessControlHandling();
    }

    @Override
    public void setAccessControlHandling(final AccessControlHandling acHandling) {
        optionsDelegate.setAccessControlHandling(acHandling);
    }

    @Override
    public boolean isNonRecursive() {
        return optionsDelegate.isNonRecursive();
    }

    @Override
    public void setNonRecursive(final boolean nonRecursive) {
        optionsDelegate.setNonRecursive(nonRecursive);
    }

    @Override
    public Pattern getCndPattern() {
        return optionsDelegate.getCndPattern();
    }

    @Override
    public void setCndPattern(final String cndPattern) throws PatternSyntaxException {
        optionsDelegate.setCndPattern(cndPattern);
    }

    @Override
    public boolean isDryRun() {
        return optionsDelegate.isDryRun();
    }

    @Override
    public void setDryRun(final boolean dryRun) {
        optionsDelegate.setDryRun(dryRun);
    }

    @Override
    public void setAutoSaveThreshold(final int threshold) {
        optionsDelegate.setAutoSaveThreshold(threshold);
    }

    @Override
    public int getAutoSaveThreshold() {
        return optionsDelegate.getAutoSaveThreshold();
    }

    @Override
    public ImportMode getImportMode() {
        return optionsDelegate.getImportMode();
    }

    @Override
    public void setImportMode(final ImportMode importMode) {
        optionsDelegate.setImportMode(importMode);
    }

    @Override
    public WorkspaceFilter getFilter() {
        return optionsDelegate.getFilter();
    }

    @Override
    public void setFilter(final WorkspaceFilter filter) {
        optionsDelegate.setFilter(filter);
    }

    @Override
    public ClassLoader getHookClassLoader() {
        return optionsDelegate.getHookClassLoader();
    }

    @Override
    public void setHookClassLoader(final ClassLoader hookClassLoader) {
        optionsDelegate.setHookClassLoader(hookClassLoader);
    }

    @Override
    public PathMapping getPathMapping() {
        return optionsDelegate.getPathMapping();
    }

    @Override
    public void setPathMapping(final PathMapping pathMapping) {
        optionsDelegate.setPathMapping(pathMapping);
    }

    @Override
    public DependencyHandling getDependencyHandling() {
        return optionsDelegate.getDependencyHandling();
    }

    @Override
    public void setDependencyHandling(final DependencyHandling dependencyHandling) {
        optionsDelegate.setDependencyHandling(dependencyHandling);
    }
}
