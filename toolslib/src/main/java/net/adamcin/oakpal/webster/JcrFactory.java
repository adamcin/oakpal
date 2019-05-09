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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import javax.jcr.Repository;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.composite.CompositeNodeStore;
import org.apache.jackrabbit.oak.composite.InitialContentMigrator;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.apache.jackrabbit.oak.spi.mount.MountInfoProvider;
import org.apache.jackrabbit.oak.spi.mount.Mounts;
import org.apache.jackrabbit.oak.spi.state.NodeStore;

/**
 * JCR Repository factory for {@link NodeStoreFixture} instances defined by oak-run option sets.
 */
public final class JcrFactory {

    private JcrFactory() {
        // do nothing
    }

    /**
     * Create an in-memory JCR repository backed by a read-only composite mount on top of the provided node store fixture.
     *
     * @param nodeStoreFixture seed node store defined by oak-run option sets
     * @return a working JCR Repository for use with admin/admin credentials
     * @throws IOException           if {@link InitialContentMigrator#migrate()} throws
     * @throws CommitFailedException if {@link InitialContentMigrator#migrate()} throws
     */
    public static Repository getJcr(final NodeStoreFixture nodeStoreFixture) throws IOException, CommitFailedException {
        return getJcr(nodeStoreFixture, new MemoryNodeStore());
    }

    /**
     * Create an in-memory JCR repository backed by a read-only composite mount on top of the provided node store fixture.
     *
     * @param nodeStoreFixture seed node store defined by oak-run option sets
     * @param globalStore      a read/write node store to initialize as the composite global store
     * @return a working JCR Repository for use with admin/admin credentials
     * @throws IOException           if {@link InitialContentMigrator#migrate()} throws
     * @throws CommitFailedException if {@link InitialContentMigrator#migrate()} throws
     */
    public static Repository getJcr(final NodeStoreFixture nodeStoreFixture, final NodeStore globalStore)
            throws IOException, CommitFailedException {

        final List<String> globalRoots = Arrays.asList("jcr:system", "oak:index", "rep:security");
        final Predicate<String> ownedRootFilter = ((Predicate<String>) globalRoots::contains).negate();

        // only mount nodes that aren't owned by the global mount
        final String[] mountPaths = StreamSupport
                .stream(nodeStoreFixture.getStore().getRoot().getChildNodeNames().spliterator(),
                        false).filter(ownedRootFilter).map(child -> "/" + child).toArray(String[]::new);

        MountInfoProvider mounts = Mounts.newBuilder().readOnlyMount("source", mountPaths).build();

        // migrate some jcr:system children to composite global store for read/write
        // "/jcr:system/rep:namespaces", "/jcr:system/rep:privileges", "/jcr:system/jcr:nodeTypes"),
        new InitialContentMigrator(globalStore, nodeStoreFixture.getStore(), mounts.getMountByName("source")).migrate();

        CompositeNodeStore cns = new CompositeNodeStore.Builder(mounts, globalStore)
                .addMount("source", nodeStoreFixture.getStore()).build();
        final Oak oak = new Oak(cns).with(nodeStoreFixture.getWhiteboard());
        Jcr jcr = new Jcr(oak, true);

        return jcr.createRepository();
    }

}
