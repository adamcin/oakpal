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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathMapping;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessor;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessorFactory;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.junit.Test;

public class InternalImportOptionsTest {

    private static final PackageId pid = PackageId.fromString("test:name:1.0-SNAPSHOT");
    private static final ClassLoader pcl = Packaging.class.getClassLoader();

    @Test
    public void testNoopHookProcessor() throws Exception {
        InternalImportOptions.NOOP_INSTALL_HOOK_PROCESSOR.registerHooks(null, null);
        InternalImportOptions.NOOP_INSTALL_HOOK_PROCESSOR.registerHook(null, null);
        assertFalse("does not have hooks", InternalImportOptions.NOOP_INSTALL_HOOK_PROCESSOR.hasHooks());
        assertTrue("execute returns true", InternalImportOptions.NOOP_INSTALL_HOOK_PROCESSOR.execute(null));
    }

    @Test
    public void testConstruct() {
        InternalImportOptions optionsNoClass = new InternalImportOptions(pid, new URLClassLoader(new URL[0], null));
        InternalImportOptions options = new InternalImportOptions(pid, pcl);
    }

    static class TestInstallHookProcessor implements InstallHookProcessor {
        @Override
        public void registerHooks(final Archive archive, final ClassLoader classLoader) throws PackageException {
        }

        @Override
        public void registerHook(final VaultInputSource input, final ClassLoader classLoader) throws IOException, PackageException {
        }

        @Override
        public boolean hasHooks() {
            return false;
        }

        @Override
        public boolean execute(final InstallContext context) {
            return true;
        }
    }

    @Test
    public void testCreateInstallHookProcessor() {
        InternalImportOptions options = new InternalImportOptions(pid, pcl);
        assertTrue("should be instance of wrapper",
                options.createInstallHookProcessor() instanceof InternalImportOptions.InstallHookProcessorWrapper);
        options.setInstallHookPolicy(InstallHookPolicy.SKIP);
        assertSame("should be noop with skip policy",
                InternalImportOptions.NOOP_INSTALL_HOOK_PROCESSOR,
                options.createInstallHookProcessor());
        options.setInstallHookPolicy(InstallHookPolicy.DEFAULT);
        options.setInstallHookProcessorFactoryDelegate(TestInstallHookProcessor::new);
        assertTrue("should be test processor intance",
                ((InternalImportOptions.InstallHookProcessorWrapper) options.createInstallHookProcessor())
                        .wrapped instanceof TestInstallHookProcessor);
        options.setInstallHookProcessorFactoryDelegate(null);

        InternalImportOptions optsNoClass = new InternalImportOptions(pid, null,
                InstallHookProcessor.class);
        assertSame("should be noop with just processor interface",
                InternalImportOptions.NOOP_INSTALL_HOOK_PROCESSOR,
                optsNoClass.createInstallHookProcessor());
    }

    @Test
    public void testIsSkip() {
        InternalImportOptions options = new InternalImportOptions(pid, pcl);
        for (InstallHookPolicy policy : InstallHookPolicy.values()) {
            options.setInstallHookPolicy(policy);
            if (policy == InstallHookPolicy.SKIP) {
                assertTrue("skip policy should be skip", options.isSkip());
            } else {
                assertFalse("non skip policy should not be skip", options.isSkip());
            }
        }
    }

    @Test
    public void testCopy() {
        final DefaultErrorListener errorListener = new DefaultErrorListener();
        final InstallHookPolicy policy = InstallHookPolicy.PROHIBIT;
        final InstallHookProcessorFactory factoryDelegate = TestInstallHookProcessor::new;
        InternalImportOptions options = new InternalImportOptions(pid, pcl);
        options.setViolationReporter(errorListener);
        options.setInstallHookPolicy(policy);
        options.setInstallHookProcessorFactoryDelegate(factoryDelegate);
        ImportOptions copy = options.copy();
        assertTrue("copy instance should be instance of same class",
                copy instanceof InternalImportOptions);
        InternalImportOptions typedCopy = (InternalImportOptions) copy;
        assertSame("same policy", InstallHookPolicy.PROHIBIT,
                typedCopy.getInstallHookPolicy());
        assertSame("same delegate", factoryDelegate,
                typedCopy.getInstallHookProcessorFactoryDelegate());
        assertSame("same reporter", errorListener,
                typedCopy.getViolationReporter());
    }

    @Test
    public void testWrapperRegisterHooksProhibited() throws Exception {
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        when(wrapped.hasHooks()).thenReturn(true);
        final ErrorListener errorListener = mock(ErrorListener.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperProhibited =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.PROHIBIT,
                        errorListener, wrapped);
        final CompletableFuture<PackageId> pidLatch = new CompletableFuture<>();
        doAnswer(invoked -> pidLatch.complete(invoked.getArgument(0)))
                .when(errorListener).onProhibitedInstallHookRegistration(any(PackageId.class));
        wrapperProhibited.registerHooks(null, null);
        assertEquals("pidLatch is same", pid, pidLatch.getNow(null));
    }

    @Test
    public void testWrapperRegisterHookProhibited() throws Exception {
        final VaultInputSource source = mock(VaultInputSource.class);
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        final ErrorListener errorListener = mock(ErrorListener.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperProhibited =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.PROHIBIT,
                        errorListener, wrapped);
        wrapperProhibited.registerHook(source, null);
    }

    @Test
    public void testWrapperRegisterHookProhibitedThrows() throws Exception {
        final VaultInputSource source = mock(VaultInputSource.class);
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        final NoClassDefFoundError ncdf = new NoClassDefFoundError();
        doThrow(ncdf).when(wrapped)
                .registerHook(any(VaultInputSource.class), nullable(ClassLoader.class));
        final ErrorListener errorListener = mock(ErrorListener.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperProhibited =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.PROHIBIT,
                        errorListener, wrapped);
        final CompletableFuture<PackageId> pidLatch = new CompletableFuture<>();
        final CompletableFuture<Throwable> errorLatch = new CompletableFuture<>();
        doAnswer(invoked -> {
            errorLatch.complete(invoked.getArgument(0));
            pidLatch.complete(invoked.getArgument(1));
            return true;
        }).when(errorListener).onInstallHookError(any(Throwable.class), any(PackageId.class));
        wrapperProhibited.registerHook(source, null);
        assertEquals("pidLatch is same", pid, pidLatch.getNow(null));
        Throwable error = errorLatch.getNow(null);
        assertTrue("errorLatch has PackageException", error instanceof PackageException);
        assertSame("errorLatch cause is NoClassDefFoundError", ncdf, error.getCause());
    }

    @Test
    public void testWrapperRegisterHooksProhibitedThrows() throws Exception {
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        final NoClassDefFoundError ncdf = new NoClassDefFoundError();
        doThrow(ncdf).when(wrapped)
                .registerHooks(nullable(Archive.class), nullable(ClassLoader.class));
        when(wrapped.hasHooks()).thenReturn(true);
        final ErrorListener errorListener = mock(ErrorListener.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperProhibited =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.PROHIBIT,
                        errorListener, wrapped);
        final CompletableFuture<PackageId> pidLatch = new CompletableFuture<>();
        final CompletableFuture<Throwable> errorLatch = new CompletableFuture<>();
        doAnswer(invoked -> pidLatch.complete(invoked.getArgument(0)))
                .when(errorListener).onProhibitedInstallHookRegistration(any(PackageId.class));
        doAnswer(invoked -> errorLatch.complete(invoked.getArgument(0)))
                .when(errorListener).onInstallHookError(any(Throwable.class), any(PackageId.class));
        wrapperProhibited.registerHooks(null, null);
        assertEquals("pidLatch is same", pid, pidLatch.getNow(null));
        Throwable error = errorLatch.getNow(null);
        assertTrue("errorLatch has PackageException", error instanceof PackageException);
        assertSame("errorLatch cause is NoClassDefFoundError", ncdf, error.getCause());

    }

    @Test(expected = PackageException.class)
    public void testWrapperRegisterHooksAbortThrows() throws Exception {
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        final NoClassDefFoundError ncdf = new NoClassDefFoundError();
        doThrow(ncdf).when(wrapped)
                .registerHooks(nullable(Archive.class), nullable(ClassLoader.class));
        final ErrorListener errorListener = mock(ErrorListener.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperAbort =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.ABORT,
                        errorListener, wrapped);
        wrapperAbort.registerHooks(null, null);
    }

    @Test(expected = PackageException.class)
    public void testWrapperRegisterHookAbortThrows() throws Exception {
        final VaultInputSource source = mock(VaultInputSource.class);
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        final NoClassDefFoundError ncdf = new NoClassDefFoundError();
        doThrow(ncdf).when(wrapped)
                .registerHook(nullable(VaultInputSource.class), nullable(ClassLoader.class));
        final ErrorListener errorListener = mock(ErrorListener.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperAbort =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.ABORT,
                        errorListener, wrapped);
        wrapperAbort.registerHook(source, null);
    }

    @Test
    public void testWrapperHasHooks() {
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);

        final InternalImportOptions.InstallHookProcessorWrapper wrapper =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.ABORT,
                        null, wrapped);

        when(wrapped.hasHooks()).thenReturn(false);
        assertFalse("has hooks now", wrapper.hasHooks());
        when(wrapped.hasHooks()).thenReturn(true);
        assertTrue("no hooks now", wrapper.hasHooks());

        final InternalImportOptions.InstallHookProcessorWrapper wrapperProhibit =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.PROHIBIT,
                        null, wrapped);

        when(wrapped.hasHooks()).thenReturn(false);
        assertFalse("never hooks on PROHIBIT", wrapperProhibit.hasHooks());
        when(wrapped.hasHooks()).thenReturn(true);
        assertFalse("never hooks on PROHIBIT", wrapperProhibit.hasHooks());
    }

    @Test
    public void testExecute() {
        final InstallHookProcessor wrapped = mock(InstallHookProcessor.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapper =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.ABORT,
                        null, wrapped);

        final CompletableFuture<InstallContext> contextLatch = new CompletableFuture<>();
        doAnswer(call -> {
            contextLatch.complete(call.getArgument(0));
            return false;
        }).when(wrapped).execute(any(InstallContext.class));

        final InstallContext context = mock(InstallContext.class);
        assertFalse("wrapped returns false", wrapper.execute(context));
        assertSame("context should be received to latch", context, contextLatch.getNow(null));

        final InstallHookProcessor wrappedProhibit = mock(InstallHookProcessor.class);
        final InternalImportOptions.InstallHookProcessorWrapper wrapperProhibit =
                new InternalImportOptions.InstallHookProcessorWrapper(pid, InstallHookPolicy.PROHIBIT,
                        null, wrappedProhibit);

        final CompletableFuture<InstallContext> prohibitLatch = new CompletableFuture<>();
        doAnswer(call -> {
            prohibitLatch.complete(call.getArgument(0));
            return false;
        }).when(wrappedProhibit).execute(any(InstallContext.class));

        assertTrue("wrapped returns true for prohibit", wrapperProhibit.execute(context));
        assertFalse("latch should be not done", prohibitLatch.isDone());
    }

    @Test
    public void testIsStrict() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setStrict(false);
        assertFalse("isStrict false", options.isStrict());
        delegate.setStrict(true);
        assertTrue("isStrict true", options.isStrict());
        options.setStrict(false);
        assertFalse("isStrict false", delegate.isStrict());
        options.setStrict(true);
        assertTrue("isStrict true", delegate.isStrict());
    }

    @Test
    public void testSetListener() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        final ProgressTrackerListener listener1 = mock(ProgressTrackerListener.class);
        final ProgressTrackerListener listener2 = mock(ProgressTrackerListener.class);
        delegate.setListener(listener1);
        assertSame("listener1", listener1, options.getListener());
        delegate.setListener(listener2);
        assertSame("listener2", listener2, options.getListener());
        options.setListener(listener1);
        assertSame("listener1", listener1, delegate.getListener());
        options.setListener(listener2);
        assertSame("listener2", listener2, delegate.getListener());
    }

    @Test
    public void testSetPatchParentPath() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setPatchParentPath(null);
        assertNull("patchParentPath null", options.getPatchParentPath());
        delegate.setPatchParentPath("/path");
        assertEquals("patchParentPath /path", "/path", options.getPatchParentPath());
        options.setPatchParentPath(null);
        assertNull("patchParentPath null", delegate.getPatchParentPath());
        options.setPatchParentPath("/path");
        assertEquals("patchParentPath /path", "/path", delegate.getPatchParentPath());
    }

    @Test
    public void testSetPatchDirectory() throws Exception {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setPatchDirectory(null);
        assertNull("patchDirectory null", options.getPatchDirectory());
        delegate.setPatchDirectory(new File("."));
        assertSame("patchDirectory file", delegate.getPatchDirectory(), options.getPatchDirectory());
        options.setPatchDirectory(null);
        assertNull("patchDirectory null", delegate.getPatchDirectory());
        options.setPatchDirectory(new File("."));
        assertSame("patchDirectory file", options.getPatchDirectory(), delegate.getPatchDirectory());
    }

    @Test
    public void testSetPatchKeepInRepo() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setPatchKeepInRepo(false);
        assertFalse("isPatchKeepInRepo false", options.isPatchKeepInRepo());
        delegate.setPatchKeepInRepo(true);
        assertTrue("isPatchKeepInRepo true", options.isPatchKeepInRepo());
        options.setPatchKeepInRepo(false);
        assertFalse("isPatchKeepInRepo false", delegate.isPatchKeepInRepo());
        options.setPatchKeepInRepo(true);
        assertTrue("isPatchKeepInRepo true", delegate.isPatchKeepInRepo());
    }

    @Test
    public void testSetAccessControlHandling() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setAccessControlHandling(null);
        assertNull("acHandling null", options.getAccessControlHandling());
        delegate.setAccessControlHandling(AccessControlHandling.CLEAR);
        assertSame("acHandling clear", AccessControlHandling.CLEAR, options.getAccessControlHandling());
        options.setAccessControlHandling(null);
        assertNull("acHandling null", delegate.getAccessControlHandling());
        options.setAccessControlHandling(AccessControlHandling.CLEAR);
        assertSame("acHandling clear", AccessControlHandling.CLEAR, delegate.getAccessControlHandling());
    }

    @Test
    public void testSetNonRecursive() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setNonRecursive(false);
        assertFalse("isNonRecursive false", options.isNonRecursive());
        delegate.setNonRecursive(true);
        assertTrue("isNonRecursive true", options.isNonRecursive());
        options.setNonRecursive(false);
        assertFalse("isNonRecursive false", delegate.isNonRecursive());
        options.setNonRecursive(true);
        assertTrue("isNonRecursive true", delegate.isNonRecursive());
    }

    @Test
    public void testSetCndPattern() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        final Pattern listener1 = delegate.getCndPattern();
        final Pattern listener2 = Pattern.compile(listener1.pattern());
        delegate.setCndPattern(listener1.pattern());
        assertEquals("instance1", listener1.pattern(), options.getCndPattern().pattern());
        delegate.setCndPattern(listener2.pattern());
        assertEquals("instance2", listener2.pattern(), options.getCndPattern().pattern());
        options.setCndPattern(listener1.pattern());
        assertEquals("instance1", listener1.pattern(), delegate.getCndPattern().pattern());
        options.setCndPattern(listener2.pattern());
        assertEquals("instance2", listener2.pattern(), delegate.getCndPattern().pattern());
    }

    @Test
    public void testSetDryRun() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setDryRun(false);
        assertFalse("isDryRun false", options.isDryRun());
        delegate.setDryRun(true);
        assertTrue("isDryRun true", options.isDryRun());
        options.setDryRun(false);
        assertFalse("isDryRun false", delegate.isDryRun());
        options.setDryRun(true);
        assertTrue("isDryRun true", delegate.isDryRun());
    }

    @Test
    public void testSetAutoSaveThreshold() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setAutoSaveThreshold(-1);
        assertEquals("isAutoSaveThreshold false", -1, options.getAutoSaveThreshold());
        delegate.setAutoSaveThreshold(1);
        assertEquals("isAutoSaveThreshold true", 1, options.getAutoSaveThreshold());
        options.setAutoSaveThreshold(-1);
        assertEquals("isAutoSaveThreshold false", -1, delegate.getAutoSaveThreshold());
        options.setAutoSaveThreshold(1);
        assertEquals("isAutoSaveThreshold true", 1, delegate.getAutoSaveThreshold());
    }

    @Test
    public void testSetImportMode() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setImportMode(null);
        assertNull("importHandling null", options.getImportMode());
        delegate.setImportMode(ImportMode.MERGE);
        assertSame("importHandling merge", ImportMode.MERGE, options.getImportMode());
        options.setImportMode(null);
        assertNull("importHandling null", delegate.getImportMode());
        options.setImportMode(ImportMode.MERGE);
        assertSame("importHandling merge", ImportMode.MERGE, delegate.getImportMode());
    }

    @Test
    public void testSetFilter() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        final WorkspaceFilter listener1 = mock(WorkspaceFilter.class);
        final WorkspaceFilter listener2 = mock(WorkspaceFilter.class);
        delegate.setFilter(listener1);
        assertSame("instance1", listener1, options.getFilter());
        delegate.setFilter(listener2);
        assertSame("instance2", listener2, options.getFilter());
        options.setFilter(listener1);
        assertSame("instance1", listener1, delegate.getFilter());
        options.setFilter(listener2);
        assertSame("instance2", listener2, delegate.getFilter());
    }

    @Test
    public void testSetHookClassLoader() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        final ClassLoader listener1 = new URLClassLoader(new URL[0], null);
        final ClassLoader listener2 = new URLClassLoader(new URL[0], null);
        delegate.setHookClassLoader(listener1);
        assertSame("instance1", listener1, options.getHookClassLoader());
        delegate.setHookClassLoader(listener2);
        assertSame("instance2", listener2, options.getHookClassLoader());
        options.setHookClassLoader(listener1);
        assertSame("instance1", listener1, delegate.getHookClassLoader());
        options.setHookClassLoader(listener2);
        assertSame("instance2", listener2, delegate.getHookClassLoader());
    }

    @Test
    public void testSetPathMapping() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        final PathMapping listener1 = mock(PathMapping.class);
        final PathMapping listener2 = mock(PathMapping.class);
        delegate.setPathMapping(listener1);
        assertSame("instance1", listener1, options.getPathMapping());
        delegate.setPathMapping(listener2);
        assertSame("instance2", listener2, options.getPathMapping());
        options.setPathMapping(listener1);
        assertSame("instance1", listener1, delegate.getPathMapping());
        options.setPathMapping(listener2);
        assertSame("instance2", listener2, delegate.getPathMapping());
    }

    @Test
    public void testSetDependencyHandling() {
        final ImportOptions delegate = new ImportOptions();
        final InternalImportOptions options = new InternalImportOptions(pid, delegate, null);
        delegate.setDependencyHandling(null);
        assertNull("dependencyHandling null", options.getDependencyHandling());
        delegate.setDependencyHandling(DependencyHandling.STRICT);
        assertSame("dependencyHandling strict", DependencyHandling.STRICT, options.getDependencyHandling());
        options.setDependencyHandling(null);
        assertNull("dependencyHandling null", delegate.getDependencyHandling());
        options.setDependencyHandling(DependencyHandling.STRICT);
        assertSame("dependencyHandling strict", DependencyHandling.STRICT, delegate.getDependencyHandling());
    }

}