/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.api;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An installable path identified as an embedded package.
 */
public final class EmbeddedPackageInstallable implements SlingInstallable<JcrPackage> {
    private final @NotNull PackageId parentId;
    private final @NotNull String jcrPath;
    private final @NotNull PackageId embeddedId;

    /**
     * Constructor.
     *
     * @param parentId   the parent package id
     * @param jcrPath    the embedded jcr path
     * @param embeddedId the embedded package id
     */
    public EmbeddedPackageInstallable(final @NotNull PackageId parentId,
                                      final @NotNull String jcrPath,
                                      final @NotNull PackageId embeddedId) {
        this.parentId = parentId;
        this.jcrPath = jcrPath;
        this.embeddedId = embeddedId;
    }

    @NotNull
    @Override
    public PackageId getParentId() {
        return parentId;
    }

    @NotNull
    @Override
    public String getJcrPath() {
        return jcrPath;
    }

    @NotNull
    public PackageId getEmbeddedId() {
        return embeddedId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final EmbeddedPackageInstallable that = (EmbeddedPackageInstallable) o;
        return parentId.equals(that.parentId) &&
                jcrPath.equals(that.jcrPath) &&
                embeddedId.equals(that.embeddedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId, jcrPath, embeddedId);
    }
}
