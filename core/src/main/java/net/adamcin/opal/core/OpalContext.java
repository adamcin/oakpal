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

package net.adamcin.opal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import aQute.bnd.annotation.ProviderType;
import net.adamcin.opal.core.jcrfacade.SessionFacade;
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
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.packaging.VaultPackage;

/**
 * Entry point for OpalContext Package Scanner.
 */
@ProviderType
public final class OpalContext {
    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new DefaultErrorHandler();

    private final List<OpalHandler> handlers;
    private final ErrorHandler errorHandler;
    private final List<File> preInstallPackages;

    private OpalContext(List<OpalHandler> handlers, ErrorHandler errorHandler, List<File> preInstallPackages) {
        this.handlers = handlers;
        this.errorHandler = errorHandler;
        this.preInstallPackages = preInstallPackages;
    }

    public static class Builder {
        private List<OpalHandler> handlers = Collections.emptyList();
        private ErrorHandler errorHandler = DEFAULT_ERROR_HANDLER;
        private List<File> preInstallPackages = Collections.emptyList();

        public Builder withHandlers(OpalHandler... handlers) {
            if (handlers != null) {
                this.handlers = Arrays.asList(handlers);
            } else {
                this.handlers = Collections.emptyList();
            }
            return this;
        }

        public Builder withErrorHandler(ErrorHandler errorHandler) {
            if (errorHandler == null) {
                this.errorHandler = DEFAULT_ERROR_HANDLER;
            } else {
                this.errorHandler = errorHandler;
            }
            return this;
        }

        public Builder withPreInstallPackages(File... preInstallPackages) {
            if (preInstallPackages != null) {
                this.preInstallPackages = Arrays.asList(preInstallPackages);
            } else {
                this.preInstallPackages = Collections.emptyList();
            }
            return this;
        }

        public OpalContext build() {
            return new OpalContext(handlers, errorHandler, preInstallPackages);
        }
    }

    public List<OpalHandler> getHandlers() {
        return handlers;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public List<File> getPreInstallPackages() {
        return preInstallPackages;
    }

    public Collection<OpalViolation> scanPackages(File... files) {
        // initialize listeners
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        Session admin = null;
        Repository scanRepo = null;
        try {
            scanRepo = initRepository();
            admin = loginAdmin(scanRepo);

            JcrPackageManager manager = PackagingService.getPackageManager(admin);

            for (File file : preInstallPackages) {
                processPackageFile(admin, manager, file, true);
            }

            for (File file : files) {
                processPackageFile(admin, manager, file, false);
            }

        } catch (RuntimeException e) {
            getErrorHandler().onFatalError(e);
            throw e;
        } catch (Exception e) {
            getErrorHandler().onFatalError(e);
        } finally {
            if (admin != null) {
                admin.logout();
            }
            shutdownRepository(scanRepo);

        }

        List<OpalViolation> violations = new ArrayList<>();
        violations.addAll(getErrorHandler().onComplete());
        handlers.forEach(handler -> violations.addAll(handler.onComplete()));

        return Collections.unmodifiableList(violations);
    }

    private void processPackage(Session admin, JcrPackageManager manager, JcrPackage jcrPackage, final boolean preInstall)
            throws PackageException, IOException, RepositoryException {
        final PackageId packageId = jcrPackage.getPackage().getId();
        final SessionFacade inspectSession = new SessionFacade(admin, false);
        final ProgressTrackerListener tracker =
                new ImporterListenerAdapter(packageId, handlers, inspectSession, preInstall);

        ImportOptions subpackOptions = new ImportOptions();
        subpackOptions.setNonRecursive(true);
        subpackOptions.setListener(tracker);
        List<PackageId> subpacks = Arrays.asList(jcrPackage.extractSubpackages(subpackOptions));

        final VaultPackage vaultPackage = jcrPackage.getPackage();

        handlers.forEach(handler -> handler.onOpen(packageId,
                vaultPackage.getProperties(), vaultPackage.getMetaInf(), subpacks));

        ImportOptions extractOptions = new ImportOptions();
        extractOptions.setNonRecursive(true);
        extractOptions.setListener(tracker);

        jcrPackage.extract(extractOptions);

        jcrPackage.close();

        handlers.forEach(handler -> {
            try {
                handler.onClose(packageId, inspectSession);
            } catch (RepositoryException e) {
                errorHandler.onHandlerException(e, handler, packageId);
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

            handlers.forEach(handler -> handler.onBeginSubpackage(packageId, parentId));

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            getErrorHandler().onPackageException(e, packageId);
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
                handlers.forEach(handler -> {
                    try {
                        handler.onBeginPackage(packageId, file);
                    } catch (Exception e) {
                        getErrorHandler().onHandlerException(e, handler, packageId);
                    }
                });
            }

            processPackage(admin, manager, jcrPackage, preInstall);

        } catch (IOException | PackageException | RepositoryException e) {
            getErrorHandler().onPackageException(e, file);
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
        private final List<OpalHandler> handlers;
        private final SessionFacade session;
        private final boolean preInstall;

        ImporterListenerAdapter(PackageId packageId, List<OpalHandler> handlers, SessionFacade session, boolean preInstall) {
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
                                OpalContext.this.getErrorHandler().onHandlerPathException(e, handler, packageId, path);
                            }
                        });
                    } catch (RepositoryException e) {
                        OpalContext.this.getErrorHandler().onImporterException(e, packageId, path);
                    }
                }
            }
        }

        @Override
        public void onError(Mode mode, String path, Exception e) {
            OpalContext.this.getErrorHandler().onImporterException(e, packageId, path);
        }
    }
}
