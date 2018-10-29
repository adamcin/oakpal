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
import java.util.Collection;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

/**
 * Internal facade class which serves to:
 * 1) ensure that a configured checkName is actually respected
 * 2) guard {@link PackageCheckFactory}s from being externally re-configured during a scan
 */
class PackageCheckAliasFacade implements PackageCheck {

    private final PackageCheck wrapped;
    private final String alias;

    PackageCheckAliasFacade(final PackageCheck wrapped, final String alias) {
        this.wrapped = wrapped;
        this.alias = alias;
    }

    @Override
    public String getCheckName() {
        if (alias != null) {
            return alias;
        } else {
            return wrapped.getCheckName();
        }
    }

    @Override
    public void startedScan() {
        wrapped.startedScan();
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return wrapped.getReportedViolations();
    }

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        wrapped.identifyPackage(packageId, file);
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        wrapped.identifySubpackage(packageId, parentId);
    }

    @Override
    public void beforeExtract(final PackageId packageId, final PackageProperties packageProperties, final MetaInf metaInf, final List<PackageId> subpackages) {
        wrapped.beforeExtract(packageId, packageProperties, metaInf, subpackages);
    }

    @Override
    public void importedPath(final PackageId packageId, final String path, final Node node) throws RepositoryException {
        wrapped.importedPath(packageId, path, node);
    }

    @Override
    public void deletedPath(final PackageId packageId, final String path) {
        wrapped.deletedPath(packageId, path);
    }

    @Override
    public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        wrapped.afterExtract(packageId, inspectSession);
    }

    @Override
    public void finishedScan() {
        wrapped.finishedScan();
    }
}

