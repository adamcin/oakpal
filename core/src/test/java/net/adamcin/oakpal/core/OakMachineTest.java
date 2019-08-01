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

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessor;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.impl.PackagingImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.NamespaceRegistry;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.core.Fun.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OakMachineTest {

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

}
