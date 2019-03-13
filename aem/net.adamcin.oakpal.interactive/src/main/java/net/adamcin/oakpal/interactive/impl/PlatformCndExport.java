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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefWriter;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoCloseable export of all nodetypes in a Session to a temp .cnd file.
 */
class PlatformCndExport implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformCndExport.class);

    private final ResourceResolver resourceResolver;
    private File tmpFile;
    private boolean opened;

    PlatformCndExport(final ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    URL open() throws IOException {
        if (!this.opened) {
            this.tmpFile = File.createTempFile("oakpaltmp", ".cnd");
            final Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                throw new IOException("Failed to adapt ResourceResolver to Session.");
            }
            try (Writer writer = new FileWriter(this.tmpFile)) {
                CompactNodeTypeDefWriter cnd =
                        new CompactNodeTypeDefWriter(writer, session, true);
                NodeTypeIterator iter = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();
                while (iter.hasNext()) {
                    cnd.write(iter.nextNodeType());
                }
                cnd.close();
            } catch (RepositoryException e) {
                LOGGER.error("Failed to export platform nodetypes.", e);
                throw new IOException(e);
            }
            this.opened = true;
        }
        return this.tmpFile.toURI().toURL();
    }

    @Override
    public void close() {
        if (this.tmpFile != null) {
            if (this.tmpFile.exists()) {
                this.tmpFile.delete();
            }
        }
        this.opened = false;
    }
}
