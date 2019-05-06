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

package net.adamcin.oakpal.toolslib;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.apache.jackrabbit.oak.api.Type.NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NODE_TYPE;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.TYPE_PROPERTY_NAME;

import javax.jcr.Repository;

import com.google.common.collect.ImmutableList;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.composite.CompositeNodeStore;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.IndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.IndexUtils;
import org.apache.jackrabbit.oak.plugins.index.counter.NodeCounterEditorProvider;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.apache.jackrabbit.oak.spi.commit.CommitHook;
import org.apache.jackrabbit.oak.spi.commit.EditorProvider;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.mount.Mounts;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;

public final class JcrFactory {

    private JcrFactory() {
        // do nothing
    }

    static class IndexInitializer implements RepositoryInitializer {
        @Override
        public void initialize(final NodeBuilder builder) {
            if (!builder.hasChildNode(IndexConstants.INDEX_DEFINITIONS_NAME)) {
                NodeBuilder index = IndexUtils.getOrCreateOakIndex(builder);

                NodeBuilder uuid = IndexUtils.createIndexDefinition(index, "uuid", true, true,
                        ImmutableList.<String>of(JCR_UUID), null);
                uuid.setProperty("info",
                        "Oak index for UUID lookup (direct lookup of nodes with the mixin 'mix:referenceable').");
                NodeBuilder nodetype = IndexUtils.createIndexDefinition(index, "nodetype", true, false,
                        ImmutableList.of(JCR_PRIMARYTYPE, JCR_MIXINTYPES), null);
                nodetype.setProperty("info",
                        "Oak index for queries with node type, and possibly path restrictions, " +
                                "for example \"/jcr:root/content//element(*, mix:language)\".");
                IndexUtils.createReferenceIndex(index);

                index.child("counter")
                        .setProperty(JCR_PRIMARYTYPE, INDEX_DEFINITIONS_NODE_TYPE, NAME)
                        .setProperty(TYPE_PROPERTY_NAME, NodeCounterEditorProvider.TYPE)
                        .setProperty(IndexConstants.ASYNC_PROPERTY_NAME,
                                IndexConstants.ASYNC_PROPERTY_NAME)
                        .setProperty("info", "Oak index that allows to estimate " +
                                "how many nodes are stored below a given path, " +
                                "to decide whether traversing or using an index is faster.");
            }
        }
    }

    public static Repository getJcr(final NodeStoreFixture nodeStoreFixture) {
        CompositeNodeStore cns = new CompositeNodeStore.Builder(
                Mounts.newBuilder().readOnlyMount("source", "/apps", "/libs",
                        "/jcr:system/rep:namespaces",
                        "/jcr:system/rep:privileges",
                        "/jcr:system/jcr:nodeTypes"
                ).build(),
                new MemoryNodeStore()).addMount("source", nodeStoreFixture.getStore()).build();
        final Oak oak = new Oak(cns).with(nodeStoreFixture.getWhiteboard());
        final Oak.OakDefaultComponents defs = Oak.OakDefaultComponents.INSTANCE;
        Jcr jcr = new Jcr(oak, false)
                .with(new IndexInitializer())
                .with(defs.securityProvider());

        for (CommitHook ch : defs.commitHooks()) {
            jcr.with(ch);
        }
        for (EditorProvider ep : defs.editorProviders()) {
            jcr.with(ep);
        }
        for (IndexEditorProvider iep : defs.indexEditorProviders()) {
            jcr.with(iep);
        }
        for (QueryIndexProvider qip : defs.queryIndexProviders()) {
            jcr.with(qip);
        }

        return jcr.createRepository();
    }

}
