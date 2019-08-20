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

import org.apache.jackrabbit.oak.api.Blob;
import org.apache.jackrabbit.oak.plugins.blob.BlobStoreBlob;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.state.ProxyNodeStore;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link MemoryNodeStore} that offloads binaries to a {@link FileBlobStore}. This is more memory-efficient for
 * production scans than a {@link MemoryNodeStore} by itself.
 *
 * If the size of the tree, excluding binaries, is likely to consume all heap, consider upgrading to a
 * {@link org.apache.jackrabbit.oak.segment.SegmentNodeStore SegmentNodeStore}.
 */
public class FileBlobMemoryNodeStore extends ProxyNodeStore {

    private final MemoryNodeStore nodeStore;
    private final FileBlobStore blobStore;

    public FileBlobMemoryNodeStore(final @NotNull String blobStorePath) {
        this.nodeStore = new MemoryNodeStore();
        this.blobStore = new FileBlobStore(blobStorePath);
    }

    @Override
    protected NodeStore getNodeStore() {
        return nodeStore;
    }

    @Override
    public @NotNull Blob createBlob(final @NotNull InputStream inputStream) throws IOException {
        return new BlobStoreBlob(this.blobStore, this.blobStore.writeBlob(inputStream));
    }
}
