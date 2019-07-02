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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.oak.commons.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AutoCloseable Handle for a set of temporary package files extracted for a scan.
 */
public final class ScanTempSpace<R> implements AutoCloseable {
    private final File tmpDir;
    private final Fun.ThrowingFunction<R, InputStream> opener;
    private final List<R> fileResources;
    private final List<File> tmpFiles;
    private boolean opened;

    /**
     * Create a temp file space for the provided list of package resources.
     *
     * @param fileResources list of file Resources, each adaptable to {@link InputStream}.
     */
    public ScanTempSpace(final @NotNull List<R> fileResources,
                         final @NotNull Fun.ThrowingFunction<R, InputStream> opener,
                         final @Nullable File tmpDir) {
        this.tmpDir = tmpDir;
        this.fileResources = new ArrayList<>(fileResources);
        this.tmpFiles = new ArrayList<>(fileResources.size());
        this.opener = opener;
    }

    /**
     * Open by copying the InputStreams of each package Resource to its own temp file.
     *
     * @return the list of temp files
     * @throws IOException for I/O exceptions
     */
    public List<File> open() throws IOException {
        if (!this.opened) {
            for (int i = 0; i < fileResources.size(); i++) {
                final R resource = fileResources.get(i);
                final File tmpFile = File.createTempFile("oakpaltmp", ".zip", tmpDir);
                tmpFiles.add(i, tmpFile);

                try (InputStream in = opener.tryApply(resource);
                     OutputStream out = new FileOutputStream(tmpFile)) {
                    IOUtils.copy(in, out);
                } catch (final Exception e) {
                    throw new IOException("failed to adapt resource to InputStream: " + resource.toString(), e);
                }
            }
            this.opened = true;
        }
        return Collections.unmodifiableList(tmpFiles);
    }

    /**
     * Close by deleting all the temporary files.
     */
    @Override
    public void close() {
        for (File tmpFile : tmpFiles) {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
        this.tmpFiles.clear();
        this.opened = false;
    }
}