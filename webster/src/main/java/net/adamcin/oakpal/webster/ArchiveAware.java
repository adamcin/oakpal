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

package net.adamcin.oakpal.webster;

import java.io.File;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface that allows the {@link WebsterPlan} to provide a {@link WebsterTarget} with a fresh FileVault
 * {@link Archive} to traverse.
 */
@ProviderType
public interface ArchiveAware {

    /**
     * Provides a newly opened archive to the target for scanning.
     *
     * @param archive      the archive
     * @param writeBackDir an archive-root-relative directory to use for writing updated files. If the Archive is a
     *                     FileArchive and the underlying directory is writable, this is same root directory that was
     *                     used to create the FileArchive. In other situations this will likely be a temporary directory.
     */
    void setArchive(Archive archive, File writeBackDir);
}
