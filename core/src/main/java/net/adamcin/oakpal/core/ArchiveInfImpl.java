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

import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.packaging.SubPackageHandling;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

final class ArchiveInfImpl implements ArchiveInf {
    final PackageProperties packageProperties;
    final Archive archive;

    ArchiveInfImpl(final @NotNull PackageProperties packageProperties, final @NotNull Archive archive) {
        this.packageProperties = packageProperties;
        this.archive = archive;
    }

    @Override
    public @Nullable InputStream openInputStream(final @Nullable Archive.Entry entry) throws IOException {
        return archive.openInputStream(entry);
    }

    @Override
    public @Nullable VaultInputSource getInputSource(final @Nullable Archive.Entry entry) throws IOException {
        return archive.getInputSource(entry);
    }

    @Override
    public @Nullable Archive.Entry getJcrRoot() throws IOException {
        return archive.getJcrRoot();
    }

    @Override
    public @NotNull Archive.Entry getRoot() throws IOException {
        return archive.getRoot();
    }

    @Override
    public @NotNull MetaInf getMetaInf() {
        return archive.getMetaInf();
    }

    @Override
    public @Nullable Archive.Entry getEntry(final @NotNull String path) throws IOException {
        return archive.getEntry(path);
    }

    @Override
    public PackageId getId() {
        return packageProperties.getId();
    }

    @Override
    public Calendar getLastModified() {
        return packageProperties.getLastModified();
    }

    @Override
    public String getLastModifiedBy() {
        return packageProperties.getLastModifiedBy();
    }

    @Override
    public Calendar getCreated() {
        return packageProperties.getCreated();
    }

    @Override
    public String getCreatedBy() {
        return packageProperties.getCreatedBy();
    }

    @Override
    public Calendar getLastWrapped() {
        return packageProperties.getLastWrapped();
    }

    @Override
    public String getLastWrappedBy() {
        return packageProperties.getLastWrappedBy();
    }

    @Override
    public String getDescription() {
        return packageProperties.getDescription();
    }

    @Override
    public boolean requiresRoot() {
        return packageProperties.requiresRoot();
    }

    @Override
    public Dependency[] getDependencies() {
        return packageProperties.getDependencies();
    }

    @Override
    public AccessControlHandling getACHandling() {
        return packageProperties.getACHandling();
    }

    @Override
    public SubPackageHandling getSubPackageHandling() {
        return packageProperties.getSubPackageHandling();
    }

    @Override
    public Calendar getDateProperty(final String name) {
        return packageProperties.getDateProperty(name);
    }

    @Override
    public String getProperty(String name) {
        return packageProperties.getProperty(name);
    }

    @Override
    public PackageType getPackageType() {
        return packageProperties.getPackageType();
    }

    static ArchiveInfImpl readInf(final @NotNull PackageId packageId,
                                  final @NotNull VaultPackage vaultPackage) throws PackageException {
        if (!vaultPackage.isValid()) {
            throw new PackageException("Package is not valid: " + packageId);
        }

        return new ArchiveInfImpl(vaultPackage.getProperties(), vaultPackage.getArchive());
    }
}
