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
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * Facade interface for {@link PackageProperties} that also exposes {@link org.apache.jackrabbit.vault.fs.io.Archive}
 * methods for reading entries.
 */
public interface ArchiveInf extends PackageProperties {

    /**
     * Opens an input stream for the given entry
     *
     * @param entry the entry
     * @return the input stream or {@code null} if the entry can't be read
     * @throws IOException if an error occurs
     */
    @Nullable
    InputStream openInputStream(@Nullable Archive.Entry entry) throws IOException;

    /**
     * Returns an input source for the given entry
     *
     * @param entry the entry
     * @return the input source or {@code null} if the entry can't be read
     * @throws IOException if an error occurs
     */
    @Nullable
    VaultInputSource getInputSource(@Nullable Archive.Entry entry) throws IOException;

    /**
     * Returns the entry that specifies the "jcr_root". if no such
     * entry exists, {@code null} is returned.
     *
     * @return the jcr_root entry or {@code null}
     * @throws IOException if an error occurs
     */
    @Nullable
    Archive.Entry getJcrRoot() throws IOException;

    /**
     * Returns the root entry.
     *
     * @return the root entry.
     * @throws IOException if an error occurs
     */
    @NotNull
    Archive.Entry getRoot() throws IOException;

    /**
     * Returns the meta inf. If the archive provides no specific meta data,
     * a default, empty meta inf is returned.
     *
     * @return the meta inf.
     */
    @NotNull
    MetaInf getMetaInf();

    /**
     * Returns the entry specified by path.
     *
     * @param path the path
     * @return the entry or {@code null} if not found.
     * @throws IOException if an error occurs
     */
    @Nullable
    Archive.Entry getEntry(@NotNull String path) throws IOException;
}
