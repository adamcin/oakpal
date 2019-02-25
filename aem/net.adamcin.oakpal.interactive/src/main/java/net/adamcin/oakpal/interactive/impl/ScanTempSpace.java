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

package net.adamcin.oakpal.interactive.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.oak.commons.IOUtils;
import org.apache.sling.api.resource.Resource;

public class ScanTempSpace implements AutoCloseable {
    private final List<Resource> fileResources;
    private final List<File> tmpFiles;
    private boolean opened;

    public ScanTempSpace(final List<Resource> fileResources) {
        this.fileResources = new ArrayList<>(fileResources);
        this.tmpFiles = new ArrayList<>(fileResources.size());
    }

    public List<File> open() throws IOException {
        if (!this.opened) {
            for (int i = 0; i < fileResources.size(); i++) {
                final Resource resource = fileResources.get(i);
                final File tmpFile = File.createTempFile("oakpaltmp", ".zip");
                tmpFiles.add(i, tmpFile);

                try (InputStream in = resource.adaptTo(InputStream.class);
                     OutputStream out = new FileOutputStream(tmpFile)) {
                    if (in == null) {
                        throw new IOException("failed to adapt resource to InputStream: " + resource.getPath());
                    }
                    IOUtils.copy(in, out);
                }
            }
            this.opened = true;
        }
        return Collections.unmodifiableList(tmpFiles);
    }

    @Override
    public void close() throws Exception {
        for (File tmpFile : tmpFiles) {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }
}
