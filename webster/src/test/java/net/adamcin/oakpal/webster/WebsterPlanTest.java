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
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

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

    @Test(expected = IllegalStateException.class)
    public void testBuilder_perform_withGlobalMemory_throws() throws Exception {
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withGlobalMemoryStore();
        final JcrNs fooNs = JcrNs.create("foo", "http://foo.com");
        final List<JcrNs> jcrNs = Collections.singletonList(fooNs);
        builder.withFixtureProvider(() -> {
            final NodeStoreFixture fixture = mock(NodeStoreFixture.class);
            doThrow(IllegalStateException.class).when(fixture).getStore();
            return fixture;
        });

        final File targetFile = new File(testOutDir, "testBuilder_perform_withGlobalMemory_throws.json");
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

        final File targetFile = new File(testOutDir, "testBuilder_perform_withGlobalSegment.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        builder.withTarget(JsonTargetFactory.CHECKLIST.createTarget(targetFile,
                obj().key("selectPaths", arr().val("/foo")).get()));
        builder.build().perform();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_perform_withGlobalSegment_throws() throws Exception {
        final File globalStore = new File(testOutDir, "testBuilder_perform_withGlobalSegment_throws/segmentstore");
        if (globalStore.exists()) {
            FileUtils.deleteDirectory(globalStore);
        }
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withGlobalSegmentStore(globalStore);
        final JcrNs fooNs = JcrNs.create("foo", "http://foo.com");
        final List<JcrNs> jcrNs = Collections.singletonList(fooNs);
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNs);
        builder.withFixtureProvider(() -> {
            final NodeStoreFixture fixture = mock(NodeStoreFixture.class);
            doThrow(IllegalStateException.class).when(fixture).getStore();
            return fixture;
        });

        final File targetFile = new File(testOutDir, "testBuilder_perform_withGlobalSegment_throws.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        builder.withTarget(JsonTargetFactory.CHECKLIST.createTarget(targetFile,
                obj().key("selectPaths", arr().val("/foo")).get()));
        builder.build().perform();
    }

    private interface ArchiveAwareTarget extends WebsterTarget, ArchiveAware {

    }

    @Test
    public void testBuilder_withArchiveRoot() throws Exception {
        final File testBase = new File(testOutDir, "testBuilder_withArchiveRoot");
        final File archiveSrc = new File("src/test/resources/filevault/noReferences");
        final File archiveRoot = new File(testBase, "noReferences");
        FileUtils.deleteDirectory(archiveRoot);
        FileUtils.copyDirectory(archiveSrc, archiveRoot);
        final JcrNs fooNs = JcrNs.create("foo", "http://foo.com");
        final List<JcrNs> jcrNs = Collections.singletonList(fooNs);
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNs);
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
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

        final CompletableFuture<File> slot = new CompletableFuture<>();
        final ArchiveAwareTarget target = mock(ArchiveAwareTarget.class);
        doAnswer(call -> slot.complete(call.getArgument(1, File.class)))
                .when(target).setArchive(any(Archive.class), any(File.class));

        builder.withArchiveRoot(archiveRoot);
        builder.withTarget(target);
        builder.build().perform();

        assertSame("should be same as archiveRoot", archiveRoot, slot.getNow(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateArchive_nullArchiveRoot() throws Exception {
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.build().createArchive();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateArchive_nonExistArchiveRoot() throws Exception {
        final File testBase = new File(testOutDir, "testCreateArchive_nonExistArchiveRoot");
        final File archiveRoot = new File(testBase, "notAProject");
        FileUtils.deleteDirectory(archiveRoot);
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withArchiveRoot(archiveRoot);
        assertFalse("archiveRoot should not exist", archiveRoot.exists());
        builder.build().createArchive();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateArchive_noJcrRoot() throws Exception {
        final File testBase = new File(testOutDir, "testCreateArchive_noJcrRoot");
        final File archiveSrc = new File("src/test/resources/filevault/noJcrRoot");
        final File archiveRoot = new File(testBase, "noJcrRoot");
        FileUtils.deleteDirectory(archiveRoot);
        FileUtils.copyDirectory(archiveSrc, archiveRoot);
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withArchiveRoot(archiveRoot);
        assertTrue("archiveRoot should exist", archiveRoot.exists());
        builder.build().createArchive();
    }

    @Test
    public void testCreateArchive_noMetaInfVault() throws Exception {
        final File testBase = new File(testOutDir, "testCreateArchive_noMetaInfVault");
        final File archiveSrc = new File("src/test/resources/filevault/noMetaInfVault");
        final File archiveRoot = new File(testBase, "noMetaInfVault");
        FileUtils.deleteDirectory(archiveRoot);
        FileUtils.copyDirectory(archiveSrc, archiveRoot);
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withArchiveRoot(archiveRoot);
        assertTrue("archiveRoot/jcr_root should exist", new File(archiveRoot, "jcr_root").exists());
        assertFalse("archiveRoot/META-INF/vault should not exist", new File(archiveRoot, "META-INF/vault").exists());
        builder.build().createArchive();
        assertTrue("archiveRoot/META-INF/vault should exist now", new File(archiveRoot, "META-INF/vault").exists());
    }


}