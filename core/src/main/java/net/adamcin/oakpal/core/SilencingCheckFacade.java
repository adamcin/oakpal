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

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.SilenceableCheck;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Internal facade class which serves to forcibly silence the wrapped {@link ProgressCheck} by not passing events when
 * silenced.
 */
class SilencingCheckFacade implements SilenceableCheck {

    private final ProgressCheck wrapped;
    private boolean silenced;

    SilencingCheckFacade(final @NotNull ProgressCheck wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String getCheckName() {
        return wrapped.getCheckName();
    }

    @Override
    public @Nullable String getResourceBundleBaseName() {
        return wrapped.getResourceBundleBaseName();
    }

    @Override
    public void setResourceBundle(final ResourceBundle resourceBundle) {
        wrapped.setResourceBundle(resourceBundle);
    }

    @Override
    public void setSilenced(final boolean silenced) {
        this.silenced = silenced;
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return wrapped.getReportedViolations();
    }

    @Override
    public void simulateSling(final SlingSimulator slingSimulator, final Set<String> runModes) {
        wrapped.simulateSling(slingSimulator, runModes);
    }

    @Override
    public void startedScan() {
        wrapped.startedScan();
    }

    @Override
    public void finishedScan() {
        wrapped.finishedScan();
    }

    //**********************
    // SILENCEABLE EVENTS...
    //**********************

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        if (!silenced) {
            wrapped.identifyPackage(packageId, file);
        }
    }

    @Override
    public void readManifest(final PackageId packageId, final Manifest manifest) {
        if (!silenced) {
            wrapped.readManifest(packageId, manifest);
        }
    }

    @Override
    public void beforeExtract(final PackageId packageId, final Session inspectSession,
                              final PackageProperties packageProperties, final MetaInf metaInf,
                              final List<PackageId> subpackages) throws RepositoryException {
        if (!silenced) {
            wrapped.beforeExtract(packageId, inspectSession, packageProperties, metaInf, subpackages);
        }
    }

    @Override
    public void importedPath(final PackageId packageId, final String path, final Node node,
                             final PathAction action) throws RepositoryException {
        if (!silenced) {
            wrapped.importedPath(packageId, path, node, action);
        }
    }

    @Override
    public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
            throws RepositoryException {
        if (!silenced) {
            wrapped.deletedPath(packageId, path, inspectSession);
        }
    }

    @Override
    public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        if (!silenced) {
            wrapped.afterExtract(packageId, inspectSession);
        }
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        if (!silenced) {
            wrapped.identifySubpackage(packageId, parentId);
        }
    }

    @Override
    public void beforeSlingInstall(final PackageId lastPackage,
                                   final SlingInstallable slingInstallable,
                                   final Session inspectSession) throws RepositoryException {
        if (!silenced) {
            wrapped.beforeSlingInstall(lastPackage, slingInstallable, inspectSession);
        }
    }

    @Override
    public void identifyEmbeddedPackage(final PackageId packageId, final PackageId parentId, final String jcrPath) {
        if (!silenced) {
            wrapped.identifyEmbeddedPackage(packageId, parentId, jcrPath);
        }
    }

    @Override
    public void appliedRepoInitScripts(final PackageId lastPackage,
                                       final SlingInstallable slingInstallable,
                                       final Session inspectSession) throws RepositoryException {
        if (!silenced) {
            wrapped.appliedRepoInitScripts(lastPackage, slingInstallable, inspectSession);
        }
    }

    @Override
    public void afterScanPackage(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        if (!silenced) {
            wrapped.afterScanPackage(packageId, inspectSession);
        }
    }


}

