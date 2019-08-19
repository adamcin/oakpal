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

import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.webster.targets.JsonTargetFactory;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.*;

public class WebsterPlanTest {

    final File testOutDir = new File("target/test-out/WebsterPlanTest");

    @Before
    public void setUp() throws Exception {
        testOutDir.mkdirs();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_perform_emptyThrows() throws Exception {
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.build().perform();
    }

    @Test
    public void testBuilder_perform_withMemoryFixture() throws Exception {
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withFixtureProvider(() -> JcrFactory.getNodeStoreFixture(true, "memory"));
        builder.build().perform();
    }

    @Test
    public void testBuilder_perform_withGlobalMemory() throws Exception {
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withGlobalMemoryStore();
        final JcrNs fooNs = JcrNs.create("foo", "http://foo.com");
        final List<JcrNs> jcrNs = Collections.singletonList(fooNs);
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNs);
        builder.withFixtureProvider(() -> {
            final NodeStoreFixture fixture = JcrFactory.getNodeStoreFixture(true, "memory");
            new OakMachine.Builder().withNodeStoreSupplier(fixture::getStore).withInitStage(
                    new InitStage.Builder()
                            .withNs(jcrNs)
                            .withQNodeTypes(JsonCnd.getQTypesFromJson(obj().key("foo:mixin", obj().key("@", arr("mixin"))).get(), mapping))
                            .withForcedRoot("/foo", "nt:folder", "foo:mixin")
                            .build()).build().scanPackage();
            return fixture;
        });

        final File targetFile = new File(testOutDir, "testBuilder_perform_withGlobalMemory.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        builder.withTarget(JsonTargetFactory.CHECKLIST.createTarget(targetFile,
                obj().key("selectPaths", arr().val("/foo")).get()));
        builder.build().perform();
    }

    @Test
    public void testBuilder_perform_withGlobalSegment() throws Exception {
        final File globalStore = new File(testOutDir, "testBuilder_perform_withGlobalSegment/segmentstore");
        if (globalStore.exists()) {
            FileUtils.deleteDirectory(globalStore);
        }
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withGlobalSegmentStore(globalStore);
        final JcrNs fooNs = JcrNs.create("foo", "http://foo.com");
        final List<JcrNs> jcrNs = Collections.singletonList(fooNs);
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNs);
        builder.withFixtureProvider(() -> {
            final NodeStoreFixture fixture = JcrFactory.getNodeStoreFixture(true, "memory");
            new OakMachine.Builder().withNodeStoreSupplier(fixture::getStore).withInitStage(
                    new InitStage.Builder()
                            .withNs(jcrNs)
                            .withQNodeTypes(JsonCnd.getQTypesFromJson(obj().key("foo:mixin", obj().key("@", arr("mixin"))).get(), mapping))
                            .withForcedRoot("/foo", "nt:folder", "foo:mixin")
                            .build()).build().scanPackage();
            return fixture;
        });

        final File targetFile = new File(testOutDir, "testBuilder_perform_prepareJcr.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        builder.withTarget(JsonTargetFactory.CHECKLIST.createTarget(targetFile,
                obj().key("selectPaths", arr().val("/foo")).get()));
        builder.build().perform();
    }
}