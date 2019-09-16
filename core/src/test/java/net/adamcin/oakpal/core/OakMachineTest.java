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

import junitx.util.PrivateAccessor;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.state.ProxyNodeStore;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessor;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.impl.PackagingImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.toEntry;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.uncheckVoid1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class OakMachineTest {

    final File testOutDir = new File("target/test-out/OakMachineTest");

    @Before
    public void setUp() throws Exception {
        testOutDir.mkdirs();
    }

    private OakMachine.Builder builder() {
        return new OakMachine.Builder();
    }

    @Test
    public void testJcrSession() throws Exception {
        OakMachine machine = builder().build();
        machine.initAndInspect(session -> {
            assertTrue("Root node should be same as / node",
                    session.getRootNode().isSame(session.getNode("/")));
        });
    }

    @Test
    public void testHomePaths() throws Exception {
        OakMachine machine = builder().build();
        machine.initAndInspect(session -> {
            assertTrue("/home/users/system should exist", session.nodeExists("/home/users/system"));
            assertTrue("/home/groups should exist", session.nodeExists("/home/groups"));
        });
    }

    @Test
    public void testReadManifest() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("null-dependency-test.zip");
        final CompletableFuture<Boolean> manifestWasRead = new CompletableFuture<>();
        ProgressCheck check = new SimpleProgressCheck() {
            @Override
            public void readManifest(final PackageId packageId, final Manifest manifest) {
                manifestWasRead.complete(Util.getManifestHeaderValues(manifest, "Content-Package-Id")
                        .contains("my_packages:null-dependency-test"));
            }
        };

        builder().withProgressChecks(check).withProgressChecks((ProgressCheck[]) null).build().scanPackage(testPackage);
        assertTrue("manifest was read, and produced correct value for Content-Package-Id",
                manifestWasRead.isDone() && manifestWasRead.get());
    }

    @Test
    public void testBuildWithPackagingService() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("null-dependency-test.zip");
        assertTrue("no errors", builder().withPackagingService(new PackagingImpl()).build()
                .scanPackage(testPackage).get(0).getViolations().isEmpty());
    }

    @Test
    public void testBuildWithInitStage() throws Exception {
        InitStage stage = new InitStage.Builder().withNs("foo", "http://foo.com").build();
        builder().withInitStage(stage).withInitStage((InitStage[]) null).withInitStages(null)
                .build().initAndInspect(session -> {
            assertEquals("expect namespace uri",
                    session.getNamespaceURI("foo"), "http://foo.com");
        });
    }

    @Test
    public void testBuildWithErrorListener() throws Exception {
        final CompletableFuture<Map.Entry<String, String>> latch = new CompletableFuture<>();
        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> latch.complete(toEntry(call.getArgument(1), call.getArgument(2))))
                .when(errorListener)
                .onJcrNamespaceRegistrationError(any(Throwable.class), anyString(), anyString());
        builder().withErrorListener(null).withErrorListener(errorListener)
                .withInitStage(new InitStage.Builder().withNs("jcr", "http://foo.com").build())
                .build().initAndInspect(session -> {
            assertEquals("jcr namespace is mapped correctly",
                    NamespaceRegistry.NAMESPACE_JCR, session.getNamespaceURI("jcr"));
        });
        assertEquals("latch entry equals", toEntry("jcr", "http://foo.com"), latch.getNow(toEntry("", "")));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBuildWithPreInstallPackage() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        builder().withPreInstallPackage((File[]) null)
                .withPreInstallPackage(testPackage)
                .build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBuildWithPreInstallPackages() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        builder().withPreInstallPackages(null)
                .withPreInstallPackages(Collections.singletonList(testPackage))
                .build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
        });
    }

    @Test
    public void testBuildWithPreInstallUrl() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        builder().withPreInstallUrl(testPackage.toURI().toURL()).build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
        });
    }

    @Test
    public void testBuildWithJcrCustomizer() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final CompletableFuture<String> latch = new CompletableFuture<>();
        builder().withPreInstallUrl(testPackage.toURI().toURL()).withJcrCustomizer(jcr -> {
            jcr.with((before, after, info) -> {
                if (!latch.isDone() && after.hasChildNode("tmp")) {
                    final NodeState tmp = after.getChildNode("tmp");
                    if (tmp.hasChildNode("foo")) {
                        final NodeState foo = tmp.getChildNode("foo");
                        if (foo.hasChildNode("bar")) {
                            final NodeState bar = tmp.getChildNode("foo");
                            latch.complete("/tmp/foo/bar");
                        }
                    }
                }
                return after;
            });
        }).build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
        });
        assertEquals("latch should have path", "/tmp/foo/bar", latch.getNow(""));
    }

    @Test(expected = AbortedScanException.class)
    public void testBuildWithPreInstallHooks_throws() throws Exception {
        final File badInstallHookPackage = TestPackageUtil.prepareTestPackageFromFolder("bad-install-hook.zip",
                new File("src/test/resources/package_with_bad_installhook"));
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        doThrow(new PackageException("throws")).when(processor).registerHooks(any(Archive.class), any(ClassLoader.class));
        builder().withEnablePreInstallHooks(true)
                .withPreInstallUrl(testPackage.toURI().toURL(), badInstallHookPackage.toURI().toURL())
                .withInstallHookProcessorFactory(() -> processor)
                .build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
        });
    }

    @Test
    public void testBuildWithInstallHookProcessorFactory_nothrows() throws Exception {
        final File badInstallHookPackage = TestPackageUtil.prepareTestPackageFromFolder("bad-install-hook.zip",
                new File("src/test/resources/package_with_bad_installhook"));
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final CompletableFuture<Boolean> latch = new CompletableFuture<>();
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        when(processor.execute(any(InstallContext.class))).thenReturn(true);
        doAnswer(call -> latch.complete(true))
                .when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        builder().withEnablePreInstallHooks(true)
                .withPreInstallUrl(testPackage.toURI().toURL(), badInstallHookPackage.toURI().toURL())
                .withInstallHookProcessorFactory(() -> processor)
                .build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
            assertTrue("hook processor was called", latch.getNow(false));
        });
    }

    @Test
    public void testBuildWithInstallClassLoader() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final ClassLoader hookClassLoader = new URLClassLoader(new URL[]{testPackage.toURI().toURL()}, null);
        final CompletableFuture<Boolean> latch = new CompletableFuture<>();
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        when(processor.execute(any(InstallContext.class))).thenReturn(true);
        doAnswer(call -> latch.complete(hookClassLoader == call.getArgument(1, ClassLoader.class)))
                .when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        builder().withEnablePreInstallHooks(true)
                .withPreInstallUrl(testPackage.toURI().toURL())
                .withInstallHookProcessorFactory(() -> processor)
                .withInstallHookClassLoader(hookClassLoader)
                .build().initAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/tmp/foo/bar"));
            assertTrue("hook processor was called with specific class loader", latch.getNow(false));
        });
    }

    @Test
    public void testBuildWithInstallClassLoaderAndScan() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final ClassLoader hookClassLoader = new URLClassLoader(new URL[]{testPackage.toURI().toURL()}, null);
        final CompletableFuture<Boolean> latch = new CompletableFuture<>();
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        when(processor.execute(any(InstallContext.class))).thenReturn(true);
        doAnswer(call -> latch.complete(hookClassLoader == call.getArgument(1, ClassLoader.class)))
                .when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        builder().withInstallHookProcessorFactory(() -> processor)
                .withInstallHookClassLoader(hookClassLoader)
                .build().scanPackage(testPackage);
    }

    @Test(expected = AbortedScanException.class)
    public void testBuildAndScanWithInstallHookPolicyAbort() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        doThrow(new PackageException("abort")).when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        builder().withInstallHookProcessorFactory(() -> processor)
                .withInstallHookPolicy(InstallHookPolicy.ABORT)
                .build().scanPackage(testPackage);
    }

    @Test
    public void testBuildAndScanWithInstallHookPolicyReport() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        when(processor.execute(any(InstallContext.class))).thenReturn(true);
        doThrow(PackageException.class).when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        final Collection<CheckReport> reports = builder()
                .withInstallHookProcessorFactory(() -> processor)
                .withInstallHookPolicy(InstallHookPolicy.REPORT)
                .build().scanPackage(testPackage);
        assertTrue("reports has one report with one violation",
                reports.stream().anyMatch(report -> report.getViolations().size() == 1));
    }

    @Test
    public void testBuildAndScanWithInstallHookPolicySkip() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        InstallHookProcessor processor = mock(InstallHookProcessor.class, withSettings().lenient());

        doThrow(new PackageException("shouldn't be thrown")).when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        final Collection<CheckReport> reports = builder()
                .withInstallHookProcessorFactory(() -> processor)
                .withInstallHookPolicy(InstallHookPolicy.SKIP)
                .build().scanPackage(testPackage);
        assertTrue("reports has one report with no violations: " + reports.stream().findFirst(),
                reports.stream().anyMatch(report -> report.getViolations().size() == 0));
    }

    @Test
    public void testBuildAndScanWithInstallHookPolicyProhibit() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InstallHookProcessor processor = mock(InstallHookProcessor.class);
        when(processor.hasHooks()).thenReturn(true);
        doThrow(new PackageException("prohibit")).when(processor)
                .registerHooks(any(Archive.class), any(ClassLoader.class));
        final Collection<CheckReport> reports = builder()
                .withInstallHookProcessorFactory(() -> processor)
                .withInstallHookPolicy(InstallHookPolicy.PROHIBIT)
                .build().scanPackage(testPackage);
        assertTrue("reports has one report with two violations: " + reports.stream().findFirst(),
                reports.stream().anyMatch(report -> report.getViolations().size() == 2));
    }

    @Test
    public void testGetProgressChecks() {
        final ProgressCheck check1 = mock(ProgressCheck.class);
        final ProgressCheck check2 = mock(ProgressCheck.class);
        final List<ProgressCheck> checks = new ArrayList<>(Arrays.asList(check1, check2));
        assertEquals("expect checks", checks,
                builder().withProgressChecks(checks).build().getProgressChecks());
    }

    @Test
    public void testGetPreInstallFiles() throws Exception {
        final File file1 = new File("./test1.zip");
        final File file2 = new File("./test2.zip");
        final OakMachine machine = builder().withPreInstallUrl(file1.toURI().toURL(), file2.toURI().toURL()).build();
        assertEquals("expect files", Arrays.asList(file1.getAbsoluteFile(), file2.getAbsoluteFile()),
                machine.getPreInstallFiles());
    }

    @Test
    public void testGetPreInstallUrls() throws Exception {
        final File file1 = new File("./test1.zip");
        final File file2 = new File("./test2.zip");
        final OakMachine machine = builder().withPreInstallUrl(file1.toURI().toURL(), file2.toURI().toURL()).build();
        assertEquals("expect urls",
                Stream.of(file1.getAbsoluteFile(), file2.getAbsoluteFile())
                        .map(compose(File::toURI, uncheck1(URI::toURL))).collect(Collectors.toList()),
                machine.getPreInstallUrls());
    }

    @Test
    public void testScanWithInitStage() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InitStage stage = new InitStage.Builder().withNs("foo", "http://foo.com").build();
        final CompletableFuture<String> fooUrlLatch = new CompletableFuture<>();
        final ProgressCheck check = mock(ProgressCheck.class);
        doCallRealMethod().when(check).beforeExtract(
                any(PackageId.class),
                any(Session.class),
                any(ArchiveInf.class),
                any(List.class)
        );
        doAnswer(call -> {
            fooUrlLatch.complete(call.getArgument(1, Session.class).getNamespaceURI("foo"));
            return true;
        }).when(check)
                .beforeExtract(
                        any(PackageId.class),
                        any(Session.class),
                        any(PackageProperties.class),
                        any(MetaInf.class),
                        any(List.class)
                );
        builder().withInitStage(stage).withProgressCheck(check).build().scanPackage(testPackage);
        assertEquals("uri is same", "http://foo.com", fooUrlLatch.getNow(""));
    }

    @Test(expected = AbortedScanException.class)
    public void testScanWithInitStage_throws() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");

        final Properties userProps = new Properties();
        // force a different admin id to prevent login with the default admin
        userProps.put(UserConstants.PARAM_ADMIN_ID, "anotheradmin");

        final Properties securityProps = new Properties();
        securityProps.put(UserConfiguration.NAME, ConfigurationParameters.of(userProps));

        builder()
                .withJcrCustomizer(jcr -> {
                    jcr.with(new SecurityProviderImpl(ConfigurationParameters.of(securityProps)));
                })
                .build().scanPackage(testPackage);
    }

    @Test
    public void testScanWithPreInstallUrl() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InitStage stage = new InitStage.Builder().withNs("foo", "http://foo.com").build();
        final CompletableFuture<Boolean> fooUrlLatch = new CompletableFuture<>();
        final ProgressCheck check = mock(ProgressCheck.class);
        doCallRealMethod().when(check)
                .beforeExtract(
                        any(PackageId.class),
                        any(Session.class),
                        any(ArchiveInf.class),
                        any(List.class)
                );
        doAnswer(call -> {
            fooUrlLatch.complete(call.getArgument(1, Session.class).nodeExists("/tmp/foo/bar"));
            return true;
        }).when(check)
                .beforeExtract(
                        any(PackageId.class),
                        any(Session.class),
                        any(PackageProperties.class),
                        any(MetaInf.class),
                        any(List.class)
                );
        builder().withInitStage(stage)
                .withPreInstallUrl(testPackage.toURI().toURL())
                .withProgressCheck(check)
                .build().scanPackage(testPackage);
        assertTrue("uri is same", fooUrlLatch.getNow(false));
    }

    @Test(expected = AbortedScanException.class)
    public void testScanInvalidPackage() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("unfiltered_package.zip");
        builder().build().scanPackage(testPackage);
    }

    @Test
    public void testScanOnListenerExceptionFromBeforeExtract() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final ProgressCheck check = mock(ProgressCheck.class);
        final ErrorListener errorListener = mock(ErrorListener.class);
        doCallRealMethod().when(check).beforeExtract(
                any(PackageId.class),
                any(Session.class),
                any(ArchiveInf.class),
                any(List.class)
        );
        doThrow(RepositoryException.class).when(check).beforeExtract(
                any(PackageId.class),
                any(Session.class),
                any(PackageProperties.class),
                any(MetaInf.class),
                any(List.class)
        );
        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<ProgressCheck> handlerLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            handlerLatch.complete(call.getArgument(1, ProgressCheck.class));
            idLatch.complete(call.getArgument(2, PackageId.class));
            return true;
        }).when(errorListener).onListenerException(any(Exception.class), any(ProgressCheck.class), any(PackageId.class));
        builder().withProgressCheck(check).withErrorListener(errorListener).build().scanPackage(testPackage);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RepositoryException);
        assertSame("same check", check, handlerLatch.getNow(null));
        assertEquals("package id is", PackageId.fromString("my_packages:tmp_foo_bar"), idLatch.getNow(null));
    }

    @Test
    public void testScanOnListenerExceptionFromAfterExtract() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final ProgressCheck check = mock(ProgressCheck.class);
        final ErrorListener errorListener = mock(ErrorListener.class);
        doThrow(RepositoryException.class).when(check).afterExtract(
                any(PackageId.class),
                any(Session.class)
        );
        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<ProgressCheck> handlerLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            handlerLatch.complete(call.getArgument(1, ProgressCheck.class));
            idLatch.complete(call.getArgument(2, PackageId.class));
            return true;
        }).when(errorListener).onListenerException(any(Exception.class), any(ProgressCheck.class), any(PackageId.class));
        builder().withProgressCheck(check).withErrorListener(errorListener).build().scanPackage(testPackage);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RepositoryException);
        assertSame("same check", check, handlerLatch.getNow(null));
        assertEquals("package id is", PackageId.fromString("my_packages:tmp_foo_bar"), idLatch.getNow(null));
    }

    @Test
    public void testScanOnListenerExceptionFromIdentifyPackage() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final ProgressCheck check = mock(ProgressCheck.class);
        final ErrorListener errorListener = mock(ErrorListener.class);
        doThrow(RuntimeException.class).when(check).identifyPackage(
                any(PackageId.class),
                any(File.class)
        );
        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<ProgressCheck> handlerLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            handlerLatch.complete(call.getArgument(1, ProgressCheck.class));
            idLatch.complete(call.getArgument(2, PackageId.class));
            return true;
        }).when(errorListener).onListenerException(any(Exception.class), any(ProgressCheck.class), any(PackageId.class));
        builder().withProgressCheck(check).withErrorListener(errorListener).build().scanPackage(testPackage);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RuntimeException);
        assertSame("same check", check, handlerLatch.getNow(null));
        assertEquals("package id is",
                PackageId.fromString("my_packages:tmp_foo_bar"), idLatch.getNow(null));
    }

    @Test
    public void testScanOnListenerExceptionFromIdentifySubpackage() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("subsubtest.zip");
        final ProgressCheck check = mock(ProgressCheck.class);
        final ErrorListener errorListener = mock(ErrorListener.class);
        doThrow(RuntimeException.class).when(check).identifySubpackage(
                any(PackageId.class),
                any(PackageId.class)
        );
        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<ProgressCheck> handlerLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            handlerLatch.complete(call.getArgument(1, ProgressCheck.class));
            idLatch.complete(call.getArgument(2, PackageId.class));
            return true;
        }).when(errorListener).onListenerException(any(Exception.class), any(ProgressCheck.class), any(PackageId.class));
        builder().withProgressCheck(check).withErrorListener(errorListener).build().scanPackage(testPackage);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RuntimeException);
        assertSame("same check", check, handlerLatch.getNow(null));
        assertEquals("package id is",
                PackageId.fromString("my_packages:subtest"), idLatch.getNow(null));
    }

    @Test
    public void testScanWithSubpackages() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("subsubtest.zip");
        final Map<PackageId, PackageId> expectIds = new LinkedHashMap<>();
        final PackageId root = PackageId.fromString("my_packages:subsubtest");
        final PackageId sub1 = PackageId.fromString("my_packages:subtest");
        final PackageId suba = PackageId.fromString("my_packages:sub_a");
        final PackageId subb = PackageId.fromString("my_packages:sub_b");
        expectIds.put(sub1, root);
        expectIds.put(suba, sub1);
        expectIds.put(subb, sub1);
        final Map<PackageId, PackageId> subToParent = new LinkedHashMap<>();
        final ProgressCheck check = mock(ProgressCheck.class);
        doAnswer(call -> subToParent.put(call.getArgument(0), call.getArgument(1)))
                .when(check).identifySubpackage(any(PackageId.class), any(PackageId.class));
        builder().withProgressCheck(check).build().scanPackage(testPackage);
        assertEquals("expect ids", expectIds, subToParent);
    }

    @Test
    public void testProcessSubpackage_onSubpackageException() throws Exception {
        final JcrPackageManager manager = mock(JcrPackageManager.class);
        doThrow(RepositoryException.class).when(manager).open(any(PackageId.class));
        final PackageId root = PackageId.fromString("my_packages:subsubtest");
        final PackageId sub1 = PackageId.fromString("my_packages:subtest");
        final Session session = mock(Session.class);
        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            idLatch.complete(call.getArgument(1, PackageId.class));
            return true;
        }).when(errorListener).onSubpackageException(any(Exception.class), any(PackageId.class));
        builder().withErrorListener(errorListener).build()
                .processSubpackage(session, manager, sub1, root, false);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RepositoryException);
        assertEquals("package id is", sub1, idLatch.getNow(null));

    }

    @Test(expected = AbortedScanException.class)
    public void testProcessPackageUrl_abortOnRefreshFailure() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final Session session = mock(Session.class);
        doThrow(RepositoryException.class).when(session).refresh(false);
        // the manager mock will only be accessed on a failure, so we need to be lenient with unused stubs checking
        final JcrPackageManager manager = mock(JcrPackageManager.class, withSettings().lenient());
        // if the method reaches the manager.upload() call, we have failed to discard our session changes
        // so we throw a NullPointerException since that is not expected for this test, and not caught by the
        // upload try block
        doThrow(NullPointerException.class).when(manager)
                .upload(any(InputStream.class), anyBoolean(), anyBoolean());
        builder().build().processPackageUrl(session, manager, false, testPackage.toURI().toURL());
    }

    @Test(expected = AbortedScanException.class)
    public void testProcessPackageFile_abortOnRefreshFailure() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final Session session = mock(Session.class);
        doThrow(RepositoryException.class).when(session).refresh(false);
        // the manager mock will only be accessed on a failure, so we need to be lenient with unused stubs checking
        final JcrPackageManager manager = mock(JcrPackageManager.class, withSettings().lenient());
        // if the method reaches the manager.upload() call, we have failed to discard our session changes
        // so we throw a NullPointerException since that is not expected for this test, and not caught by the
        // upload try block
        doThrow(NullPointerException.class).when(manager)
                .upload(any(File.class), anyBoolean(), anyBoolean(), nullable(String.class), anyBoolean());
        builder().build().processPackageFile(session, manager, true, testPackage);
    }

    @Test
    public void testImporterListenerAdapter_onMessage_deletedPathException() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final InitStage stage = new InitStage.Builder().withForcedRoot("/tmp/foo/bar/cat").build();
        final ProgressCheck check = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(check)
                .deletedPath(any(PackageId.class), anyString(), any(Session.class));

        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<ProgressCheck> handlerLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        final CompletableFuture<String> pathLatch = new CompletableFuture<>();

        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            handlerLatch.complete(call.getArgument(1, ProgressCheck.class));
            idLatch.complete(call.getArgument(2, PackageId.class));
            pathLatch.complete(call.getArgument(3, String.class));
            return true;
        }).when(errorListener).onListenerPathException(any(Exception.class), any(ProgressCheck.class),
                any(PackageId.class), anyString());
        builder().withInitStage(stage)
                .withProgressCheck(check)
                .withErrorListener(errorListener)
                .build().scanPackage(testPackage);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RepositoryException);
        assertSame("same check", check, handlerLatch.getNow(null));
        assertEquals("package id is",
                PackageId.fromString("my_packages:tmp_foo_bar"), idLatch.getNow(null));
        assertEquals("path is", "/tmp/foo/bar/cat", pathLatch.getNow(""));
    }

    @Test
    public void testImporterListenerAdapter_onMessage_importedPathException() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final ProgressCheck check = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(check)
                .importedPath(any(PackageId.class), anyString(), any(Node.class));

        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<ProgressCheck> handlerLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        final CompletableFuture<String> pathLatch = new CompletableFuture<>();

        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            handlerLatch.complete(call.getArgument(1, ProgressCheck.class));
            idLatch.complete(call.getArgument(2, PackageId.class));
            pathLatch.complete(call.getArgument(3, String.class));
            return true;
        }).when(errorListener).onListenerPathException(any(Exception.class), any(ProgressCheck.class),
                any(PackageId.class), anyString());
        builder()
                .withProgressCheck(check)
                .withErrorListener(errorListener)
                .build().scanPackage(testPackage);
        assertTrue("error is of type", eLatch.getNow(null) instanceof RepositoryException);
        assertSame("same check", check, handlerLatch.getNow(null));
        assertEquals("package id is",
                PackageId.fromString("my_packages:tmp_foo_bar"), idLatch.getNow(null));
        assertEquals("path is", "/", pathLatch.getNow(""));
    }

    @Test
    public void testImporterListenerAdapter_onError() {
        final PackageId expectId = PackageId.fromString("my_packages:tmp_foo_bar");
        final String expectPath = "/correct/path";
        final Exception expectError = new RepositoryException("dummy");
        final Session session = mock(Session.class);

        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        final CompletableFuture<String> pathLatch = new CompletableFuture<>();

        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            idLatch.complete(call.getArgument(1, PackageId.class));
            pathLatch.complete(call.getArgument(2, String.class));
            return true;
        }).when(errorListener).onImporterException(any(Exception.class), any(PackageId.class), anyString());
        final OakMachine machine = builder().withErrorListener(errorListener).build();
        final OakMachine.ImporterListenerAdapter adapter = machine.new ImporterListenerAdapter(expectId,
                machine.getProgressChecks(), session, false);
        adapter.onError(ProgressTrackerListener.Mode.PATHS, expectPath, expectError);
        assertSame("error is same", expectError, eLatch.getNow(null));
        assertEquals("package id is", expectId, idLatch.getNow(null));
        assertEquals("path is", expectPath, pathLatch.getNow(null));
    }

    @Test(expected = RuntimeException.class)
    public void testImporterListenerAdapter_onMessage_error() throws Exception {
        final PackageId expectId = PackageId.fromString("my_packages:tmp_foo_bar");
        final String expectPath = "/correct/path";
        final Session session = mock(Session.class);

        final CompletableFuture<Exception> eLatch = new CompletableFuture<>();
        final CompletableFuture<PackageId> idLatch = new CompletableFuture<>();
        final CompletableFuture<String> pathLatch = new CompletableFuture<>();

        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> {
            eLatch.complete(call.getArgument(0, Exception.class));
            idLatch.complete(call.getArgument(1, PackageId.class));
            pathLatch.complete(call.getArgument(2, String.class));
            return true;
        }).when(errorListener).onImporterException(any(Exception.class), any(PackageId.class), anyString());
        final OakMachine machine = builder().withErrorListener(errorListener).build();
        final OakMachine.ImporterListenerAdapter adapter = machine.new ImporterListenerAdapter(expectId,
                machine.getProgressChecks(), session, false);
        adapter.onMessage(ProgressTrackerListener.Mode.PATHS, "E", expectPath);
        assertEquals("package id is", expectId, idLatch.getNow(null));
        assertEquals("path is", expectPath, pathLatch.getNow(null));
        throw eLatch.getNow(new Exception());
    }

    @Test
    public void testNewOakpalPackagingServiceNoArgs() throws Exception {
        Packaging service = OakMachine.newOakpalPackagingService();
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
        });
    }

    @Test
    public void testNewOakpalPackagingServiceWithClassLoader() throws Exception {
        Packaging service = OakMachine.newOakpalPackagingService(Util.getDefaultClassLoader());
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
        });
    }

    @Test
    public void testCustomNodeStore() throws Exception {
        CompletableFuture<Boolean> usedCustomNodeStore = new CompletableFuture<>();
        OakMachine.JcrCustomizer checker = uncheckVoid1(jcr -> {
            // relies a bit on the Oak internals, but safe enough for a test
            Object oak = PrivateAccessor.getField(jcr, "oak");
            Object nodeStore = PrivateAccessor.getField(oak, "store");
            usedCustomNodeStore.complete(nodeStore instanceof CustomNodeStore);
        })::accept;
        OakMachine machine = new OakMachine.Builder().withNodeStoreSupplier(CustomNodeStore::new).withJcrCustomizer(checker).build();
        machine.scanPackage();

        assertTrue("Custom node store was used", usedCustomNodeStore.getNow(false));
    }

    private static class CustomNodeStore extends ProxyNodeStore {
        private final NodeStore nodeStore;

        private CustomNodeStore() {
            this.nodeStore = new MemoryNodeStore();
        }

        @Override
        public NodeStore getNodeStore() {
            return nodeStore;
        }
    }

    @Test
    public void testFileBlobMemoryNodeStore() throws Exception {
        final File blobStoreFile = new File(testOutDir, "testFileBlobMemoryNodeStore/datastore");
        if (blobStoreFile.isDirectory()) {
            FileUtils.deleteDirectory(blobStoreFile);
        }

        final OakMachine.Builder machineBuilder = builder().withNodeStoreSupplier(() ->
                new FileBlobMemoryNodeStore(blobStoreFile.getAbsolutePath()));

        machineBuilder.build().adminInitAndInspect(session -> {
                    // less than AbtractBlobStore.minBlockSize - 2 (for varInt length prefix where length >= 128 && < 16384)
                    final Binary binary = alphaFill(session, 4093);

                    Node fooNode = session.getRootNode().addNode("foo", "nt:unstructured");
                    fooNode.setProperty("data", binary);
                    session.save();

                    assertTrue("blob is retrievable", fooNode.getProperty("data").getString().startsWith("abcdefg"));
                });

        final File[] inlineChildren = blobStoreFile.listFiles();
        assertNotNull("should have non-null inlineChildren", inlineChildren);
        assertEquals("inline children is empty <4k", 0, inlineChildren.length);

        machineBuilder.build().adminInitAndInspect(session -> {
                    // bigger than 4096
                    final Binary binary = alphaFill(session, 8192);
                    assertFalse("/foo should not exist", session.nodeExists("/foo"));
                    Node fooNode = session.getRootNode().addNode("foo", "nt:unstructured");
                    fooNode.setProperty("data", binary);
                    session.save();

                    assertTrue("blob is retrievable", fooNode.getProperty("data").getString().startsWith("abcdefg"));
                });

        final File[] blobChildren = blobStoreFile.listFiles();
        assertNotNull("should have non-null blobChildren", blobChildren);
        assertEquals("blobChildren is not empty @>4k", 1, blobChildren.length);
    }

    private static Binary alphaFill(final @NotNull Session session, final int bufSize) throws RepositoryException {
        final byte[] buffer = new byte[bufSize];
        final String fillString = "abcdefghijklmnopqrstuvwxyz";
        final byte[] fillData = fillString.getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        while (pos < bufSize - fillData.length) {
            pos += new ByteArrayInputStream(fillData).read(buffer, pos, fillData.length);
        }
        if (bufSize - pos > 0) {
            new ByteArrayInputStream(fillData).read(buffer, pos, bufSize - pos);
        }

        return session.getValueFactory().createBinary(new ByteArrayInputStream(buffer));
    }
}

