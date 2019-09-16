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

package net.adamcin.oakpal.maven.mojo;

import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.webster.JcrFactory;
import net.adamcin.oakpal.webster.WebsterPlan;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class WebsterMojoTest {
    private final File srcDir = new File("src/test/resources/WebsterMojoTest");
    private final File testOutBaseDir = new File("target/test-out/WebsterMojoTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    private static WebsterMojo newMojo() {
        WebsterMojo mojo = new WebsterMojo();
        MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    private static MockMojoLog logFor(final @NotNull WebsterMojo mojo) {
        return MockMojoLog.class.cast(mojo.getLog());
    }

    @Test
    public void testSuppressOakLogging() {
        WebsterMojo mojo = newMojo();
        mojo.revealOakLogging = true;
        Properties props = new Properties();
        mojo.suppressOakLogging(props::setProperty);
        assertTrue("props is still empty: " + props, props.isEmpty());
        mojo.revealOakLogging = false;
        mojo.suppressOakLogging(props::setProperty);
        assertEquals("props is not empty: " + props, "ERROR",
                props.getProperty("org.slf4j.simpleLogger.log.org.apache.jackrabbit.oak"));
    }

    @Test
    public void testAddTargets() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testAddTargets");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        WebsterMojo mojo = newMojo();
        mojo.baseDir = new File(testOutDir, "baseDir");
        mojo.websterTargets = obj()
                .key("nodetypes", obj())
                .key("privileges", obj())
                .get();

        final WebsterTarget fakeTarget = mock(WebsterTarget.class);
        final CompletableFuture<Boolean> performed = new CompletableFuture<>();
        doAnswer(call -> performed.complete(true)).when(fakeTarget).perform(any(Session.class));
        final WebsterPlan.Builder builder = new WebsterPlan.Builder();
        mojo.addTargets(builder, (basedir, config) -> Collections.singletonList(fakeTarget));
        final File segmentStore = new File(testOutDir, "segmentstore");
        builder.withFixtureProvider(() -> JcrFactory.getReadWriteFixture(segmentStore));
        builder.build().perform();
        assertTrue("should be performed", performed.getNow(false));
    }

    @Test(expected = MojoFailureException.class)
    public void testAddTargets_throws() throws Exception {
        WebsterMojo mojo = newMojo();
        mojo.baseDir = new File(".");
        mojo.websterTargets = obj().get();
        mojo.addTargets(new WebsterPlan.Builder(), (baseDir, targets) -> {
            throw new IOException("failed yo");
        });
    }

    private static void withMockTarget(final @NotNull WebsterPlan.Builder builder,
                                       final @NotNull Fun.ThrowingConsumer<Session> performWith) throws Exception {
        final WebsterTarget mockTarget = mock(WebsterTarget.class);
        doAnswer(call -> {
            performWith.tryAccept(call.getArgument(0, Session.class));
            return true;
        }).when(mockTarget).perform(any(Session.class));
        builder.withTargets(Collections.singletonList(mockTarget));
    }

    @Test
    public void testGetFixtureProvider() throws Exception {
        WebsterMojo mojo = newMojo();
        assertNull("no fixture provider by default", mojo.getFixtureProvider());
    }

    @Test
    public void testGetFixtureProvider_withCliArgs() throws Exception {
        WebsterMojo mojo = newMojo();
        mojo.websterOakRunArgs = "memory";
        WebsterPlan.FixtureProvider cliProvider = mojo.getFixtureProvider();
        assertNotNull("cli fixture provider not null", cliProvider);
        final CompletableFuture<Boolean> cliPerformed = new CompletableFuture<>();
        final WebsterPlan.Builder cliBuilder = new WebsterPlan.Builder();
        withMockTarget(cliBuilder, session -> cliPerformed.complete(true));
        cliBuilder.withFixtureProvider(cliProvider);
        cliBuilder.build().perform();
        assertTrue("should be performed with cli", cliPerformed.getNow(false));
    }

    public static void prepareRepo(final @NotNull File repoDir,
                                   final @NotNull Fun.ThrowingConsumer<Session> sessionStrategy) throws Exception {
        FileUtils.deleteDirectory(repoDir);
        repoDir.mkdirs();

        final FileStore fs = FileStoreBuilder.fileStoreBuilder(repoDir).withMaxFileSize(256).build();
        final SegmentNodeStore ns = SegmentNodeStoreBuilders.builder(fs).build();
        final Repository repo = new Jcr(new Oak(ns), true).createRepository();
        Session session = null;

        try {
            session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
            sessionStrategy.tryAccept(session);
        } finally {
            if (session != null) {
                session.logout();
            }
            if (repo instanceof JackrabbitRepository) {
                ((JackrabbitRepository) repo).shutdown();
            }
            fs.close();
        }
    }

    @Test
    public void testGetFixtureProvider_withHome() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testGetFixtureProvider_withHome");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        WebsterMojo mojo = newMojo();
        mojo.websterOakRunArgs = null;
        final File repositoryHome = new File(testOutDir, "websterRepositoryHome");
        repositoryHome.mkdirs();
        mojo.websterRepositoryHome = repositoryHome;
        MockMojoLog log = logFor(mojo);
        log.entries.clear();
        assertNull("home fixture provider is null without journal.log", mojo.getFixtureProvider());
        log.printAll();
        assertTrue("log contains correct message",
                log.any(entry -> entry.message.startsWith("segmentstore/journal.log")));

        prepareRepo(new File(repositoryHome, "segmentstore"), session -> {
            session.getRootNode().addNode("foo");
            session.save();
        });

        WebsterPlan.FixtureProvider homeProvider = mojo.getFixtureProvider();
        assertNotNull("home fixture provider not null", homeProvider);
        final CompletableFuture<Boolean> homePerformed = new CompletableFuture<>();
        final WebsterPlan.Builder homeBuilder = new WebsterPlan.Builder();
        withMockTarget(homeBuilder, session -> homePerformed.complete(true));
        homeBuilder.withFixtureProvider(homeProvider);
        homeBuilder.build().perform();
        assertTrue("should be performed with home", homePerformed.getNow(false));
    }

    @Test
    public void testExecute_noTargets() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecute_noTargets");
        FileUtils.deleteDirectory(testOutDir);
        final File projectRoot = new File(testOutDir, "content-package");
        FileUtils.copyDirectory(new File(srcDir, "content-package"), projectRoot);
        WebsterMojo mojo = newMojo();
        mojo.websterArchiveRoot = new File(projectRoot, "src/main/resources");
        mojo.execute();
        Optional<MockMojoLog.MockMojoLogEntry> logEntry = logFor(mojo).last();
        assertTrue("log is not empty", logEntry.isPresent());
        assertTrue("log starts with 'No websterTargets",
                logEntry.get().message.startsWith("No websterTargets"));
    }

    @Test
    public void testExecute_noFixture() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecute_noFixture");
        FileUtils.deleteDirectory(testOutDir);
        final File projectRoot = new File(testOutDir, "content-package");
        FileUtils.copyDirectory(new File(srcDir, "content-package"), projectRoot);
        WebsterMojo mojo = newMojo();
        mojo.baseDir = projectRoot;
        mojo.websterArchiveRoot = new File(projectRoot, "src/main/resources");
        mojo.websterTargets = obj().key("nodetypes", obj()).key("privileges", obj()).get();
        mojo.execute();
        Optional<MockMojoLog.MockMojoLogEntry> logEntry = logFor(mojo).last();
        assertTrue("log is not empty", logEntry.isPresent());
        assertTrue("log starts with 'No source Oak",
                logEntry.get().message.startsWith("No source Oak"));
    }

    @Test
    public void testExecute() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecute");
        FileUtils.deleteDirectory(testOutDir);
        final File projectRoot = new File(testOutDir, "content-package");
        FileUtils.copyDirectory(new File(srcDir, "content-package"), projectRoot);
        WebsterMojo mojo = newMojo();
        final File archiveRoot = new File(projectRoot, "src/main/content");
        final File tempDir = new File(testOutDir, "webster-tmp");
        mojo.baseDir = projectRoot;
        mojo.websterTempDirectory = tempDir;
        mojo.websterArchiveRoot = archiveRoot;
        mojo.websterTargets = obj().key("nodetypes", obj()).key("privileges", obj()).get();
        mojo.websterOakRunArgs = "memory";
        mojo.execute();
        assertTrue("expect nodetypes", new File(archiveRoot, "META-INF/vault/nodetypes.cnd").exists());
        assertTrue("expect privileges", new File(archiveRoot, "META-INF/vault/privileges.xml").exists());
    }

    @Test(expected = MojoFailureException.class)
    public void testExecuteWebsterPlan_tempFails() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteWebsterPlan_tempFails");
        FileUtils.deleteDirectory(testOutDir);
        final File projectRoot = new File(testOutDir, "content-package");
        FileUtils.copyDirectory(new File(srcDir, "content-package"), projectRoot);
        WebsterMojo mojo = newMojo();
        final File tempDir = new File(testOutDir, "webster-tmp");
        FileUtils.touch(tempDir);
        mojo.websterTempDirectory = tempDir;
        final WebsterPlan.Builder builder = new WebsterPlan.Builder();
        mojo.executeWebsterPlan(builder);
    }

    @Test(expected = MojoFailureException.class)
    public void testExecuteWebsterPlan_targetFails() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteWebsterPlan_cleanTempFails");
        FileUtils.deleteDirectory(testOutDir);
        final File projectRoot = new File(testOutDir, "content-package");
        FileUtils.copyDirectory(new File(srcDir, "content-package"), projectRoot);
        final WebsterMojo mojo = newMojo();
        final File tempDir = new File(testOutDir, "webster-tmp");
        mojo.websterTempDirectory = tempDir;
        final WebsterPlan.Builder builder = new WebsterPlan.Builder();
        final File segmentStore = new File(testOutDir, "segmentstore");
        builder.withFixtureProvider(() -> JcrFactory.getReadWriteFixture(segmentStore));
        withMockTarget(builder, session -> {
            throw new Exception("Whoops.");
        });
        mojo.executeWebsterPlan(builder);
    }

    @Test
    public void testExecuteWebsterPlan_cleanTempFails() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteWebsterPlan_cleanTempFails");
        FileUtils.deleteDirectory(testOutDir);
        final File projectRoot = new File(testOutDir, "content-package");
        FileUtils.copyDirectory(new File(srcDir, "content-package"), projectRoot);
        final WebsterMojo mojo = newMojo();
        final File tempDir = new File(testOutDir, "webster-tmp");
        mojo.websterTempDirectory = tempDir;
        final WebsterPlan.Builder builder = new WebsterPlan.Builder();
        final File segmentStore = new File(testOutDir, "segmentstore");
        builder.withFixtureProvider(() -> JcrFactory.getReadWriteFixture(segmentStore));
        final CompletableFuture<File> globalHome = new CompletableFuture<>();
        withMockTarget(builder, session -> {
            final File tmp = tempDir.listFiles()[0];
            globalHome.complete(tmp);
        });
        mojo.tempDirDeleter = dir -> {
            throw new IOException("OMG I can't tho. Sorry, dir: " + dir.getAbsolutePath());
        };
        mojo.executeWebsterPlan(builder);
        assertTrue("expect log indicating failure to delete global home",
                logFor(mojo).any(entry -> entry.message.startsWith("Failed to delete temp global")));
    }

}