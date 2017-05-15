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
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

/**
 * Default implementation of {@link PackageListener}.
 */
public class DefaultPackageListener extends AbstractViolationReporter implements PackageListener {

    @Override
    public void startedScan() {

    }

    @Override
    public void finishedScan() {

    }

    @Override
    public void identifyPackage(PackageId packageId, File file) {

    }

    @Override
    public void identifySubpackage(PackageId packageId, PackageId parentId) {

    }

    @Override
    public void beforeExtract(PackageId packageId, PackageProperties packageProperties, MetaInf metaInf, List<PackageId> subpackages) {

    }

    @Override
    public void importedPath(PackageId packageId, String path, Node node) throws RepositoryException {

    }

    @Override
    public void deletedPath(PackageId packageId, String path) {

    }

    @Override
    public void afterExtract(PackageId packageId, Session inspectSession) throws RepositoryException {

    }

}
