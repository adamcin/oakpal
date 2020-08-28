/*
 * Copyright 2020 Mark Adamcin
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

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.sling.NoopSlingSimulator;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SilencingCheckFacadeTest {

    @Test
    public void testGetCheckName() {
        final ProgressCheck delegate = mock(ProgressCheck.class);
        when(delegate.getCheckName()).thenReturn("delegate");
        final SilencingCheckFacade withoutAlias = new SilencingCheckFacade(delegate);
        assertEquals("delegate name", "delegate", withoutAlias.getCheckName());
    }

    private static class DelegateSuccessfullyCalledException extends RuntimeException {
    }

    @Test
    public void testStartedScan() {
        final CompletableFuture<Boolean> didIt = new CompletableFuture<>();
        final ProgressCheck delegate = mock(ProgressCheck.class);
        doAnswer(call -> didIt.complete(true)).when(delegate).startedScan();
        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.startedScan();
        assertTrue("did it", didIt.getNow(false));

        // startedScan is not silenced
        alias.setSilenced(true);
        final CompletableFuture<Boolean> didItAgain = new CompletableFuture<>();
        doAnswer(call -> didItAgain.complete(true)).when(delegate).startedScan();
        alias.startedScan();
        assertTrue("did it again", didItAgain.getNow(false));
    }

    @Test
    public void testGetReportedViolations() {
        final Collection<Violation> violations = new ArrayList<>();
        final ProgressCheck delegate = mock(ProgressCheck.class);
        when(delegate.getReportedViolations()).thenReturn(violations);
        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        assertSame("same value", violations, alias.getReportedViolations());
    }

    @Test
    public void testFinishedScan() {
        final CompletableFuture<Boolean> didIt = new CompletableFuture<>();
        final ProgressCheck delegate = mock(ProgressCheck.class);
        doAnswer(call -> didIt.complete(true)).when(delegate).finishedScan();
        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.finishedScan();
        assertTrue("did it", didIt.getNow(false));

        // finishedScan is not silenced
        alias.setSilenced(true);
        final CompletableFuture<Boolean> didItAgain = new CompletableFuture<>();
        doAnswer(call -> didItAgain.complete(true)).when(delegate).finishedScan();
        alias.finishedScan();
        assertTrue("did it again", didItAgain.getNow(false));
    }

    @Test
    public void testSimulateSling() {
        final SlingSimulator arg0 = NoopSlingSimulator.instance();
        final Set<String> arg1 = Stream.of("author", "publish").collect(Collectors.toSet());
        final CompletableFuture<SlingSimulator> slot0 = new CompletableFuture<>();
        final CompletableFuture<Set<?>> slot1 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, SlingSimulator.class));
            slot1.complete(call.getArgument(1, Set.class));
            return true;
        }).when(delegate).simulateSling(
                any(SlingSimulator.class),
                any(Set.class));

        // simulateSling is not silenced
        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.simulateSling(arg0, arg1);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
    }

    @Test
    public void testIdentifyPackage() {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final File arg1 = new File("./foo");

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<File> slot1 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, File.class));
            return true;
        }).when(delegate).identifyPackage(
                any(PackageId.class),
                any(File.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.identifyPackage(arg0, arg1);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());

        alias.setSilenced(false);
        alias.identifyPackage(arg0, arg1);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
    }

    @Test
    public void testReadManifest() {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Manifest arg1 = new Manifest();

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<Manifest> slot1 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, Manifest.class));
            return true;
        }).when(delegate).readManifest(
                any(PackageId.class),
                any(Manifest.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);

        alias.setSilenced(true);
        alias.readManifest(arg0, arg1);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());

        alias.setSilenced(false);
        alias.readManifest(arg0, arg1);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
    }

    @Test
    public void testIdentifySubpackage() {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final PackageId arg1 = PackageId.fromString("my_packages:other_example:1.0");

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<PackageId> slot1 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, PackageId.class));
            return true;
        }).when(delegate).identifySubpackage(
                any(PackageId.class),
                any(PackageId.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.identifySubpackage(arg0, arg1);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());

        alias.setSilenced(false);
        alias.identifySubpackage(arg0, arg1);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RepositoryException.class)
    public void testBeforeExtract_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Session arg1 = mock(Session.class);
        final PackageProperties arg2 = mock(PackageProperties.class);
        final MetaInf arg3 = mock(MetaInf.class);
        final List<PackageId> arg4 = new ArrayList<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).beforeExtract(
                any(PackageId.class),
                any(Session.class),
                any(PackageProperties.class),
                any(MetaInf.class),
                any(List.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.beforeExtract(arg0, arg1, arg2, arg3, arg4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBeforeExtract() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Session arg1 = mock(Session.class);
        final PackageProperties arg2 = mock(PackageProperties.class);
        final MetaInf arg3 = mock(MetaInf.class);
        final List<PackageId> arg4 = new ArrayList<>();

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<Session> slot1 = new CompletableFuture<>();
        final CompletableFuture<PackageProperties> slot2 = new CompletableFuture<>();
        final CompletableFuture<MetaInf> slot3 = new CompletableFuture<>();
        final CompletableFuture<List> slot4 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, Session.class));
            slot2.complete(call.getArgument(2, PackageProperties.class));
            slot3.complete(call.getArgument(3, MetaInf.class));
            slot4.complete(call.getArgument(4, List.class));
            return true;
        }).when(delegate).beforeExtract(
                any(PackageId.class),
                any(Session.class),
                any(PackageProperties.class),
                any(MetaInf.class),
                any(List.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.beforeExtract(arg0, arg1, arg2, arg3, arg4);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());
        assertFalse("arg2 should not be done", slot2.isDone());
        assertFalse("arg3 should not be done", slot3.isDone());
        assertFalse("arg4 should not be done", slot4.isDone());

        alias.setSilenced(false);
        alias.beforeExtract(arg0, arg1, arg2, arg3, arg4);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
        assertSame("same arg2", arg2, slot2.getNow(null));
        assertSame("same arg3", arg3, slot3.getNow(null));
        assertSame("same arg4", arg4, slot4.getNow(null));
    }

    @Test(expected = RepositoryException.class)
    public void testImportedPath_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final String arg1 = "/correct/path";
        final Node arg2 = mock(Node.class);
        final PathAction arg3 = PathAction.MODIFIED;

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).importedPath(
                any(PackageId.class),
                any(String.class),
                any(Node.class),
                any(PathAction.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.importedPath(arg0, arg1, arg2, arg3);
    }

    @Test
    public void testImportedPath() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final String arg1 = "/correct/path";
        final Node arg2 = mock(Node.class);
        final PathAction arg3 = PathAction.MODIFIED;

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<String> slot1 = new CompletableFuture<>();
        final CompletableFuture<Node> slot2 = new CompletableFuture<>();
        final CompletableFuture<PathAction> slot3 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, String.class));
            slot2.complete(call.getArgument(2, Node.class));
            slot3.complete(call.getArgument(3, PathAction.class));
            return true;
        }).when(delegate).importedPath(
                any(PackageId.class),
                any(String.class),
                any(Node.class),
                any(PathAction.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.importedPath(arg0, arg1, arg2, arg3);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());
        assertFalse("arg2 should not be done", slot2.isDone());
        assertFalse("arg3 should not be done", slot3.isDone());

        alias.setSilenced(false);
        alias.importedPath(arg0, arg1, arg2, arg3);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
        assertSame("same arg2", arg2, slot2.getNow(null));
        assertSame("same arg3", arg3, slot3.getNow(null));
    }

    @Test(expected = RepositoryException.class)
    public void testDeletedPath_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final String arg1 = "/correct/path";
        final Session arg2 = mock(Session.class);

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).deletedPath(
                any(PackageId.class),
                any(String.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.deletedPath(arg0, arg1, arg2);
    }

    @Test
    public void testDeletedPath() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final String arg1 = "/correct/path";
        final Session arg2 = mock(Session.class);

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<String> slot1 = new CompletableFuture<>();
        final CompletableFuture<Session> slot2 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, String.class));
            slot2.complete(call.getArgument(2, Session.class));
            return true;
        }).when(delegate).deletedPath(
                any(PackageId.class),
                any(String.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.deletedPath(arg0, arg1, arg2);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());
        assertFalse("arg2 should not be done", slot2.isDone());

        alias.setSilenced(false);
        alias.deletedPath(arg0, arg1, arg2);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
        assertSame("same arg2", arg2, slot2.getNow(null));
    }

    @Test(expected = RepositoryException.class)
    public void testAfterExtract_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Session arg1 = mock(Session.class);

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).afterExtract(
                any(PackageId.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.afterExtract(arg0, arg1);
    }

    @Test
    public void testAfterExtract() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Session arg1 = mock(Session.class);

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<Session> slot1 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, Session.class));
            return true;
        }).when(delegate).afterExtract(
                any(PackageId.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.afterExtract(arg0, arg1);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());

        alias.setSilenced(false);
        alias.afterExtract(arg0, arg1);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
    }

    @Test
    public void testSetResourceBundle() {
        final ProgressCheck wrappedCheck = mock(ProgressCheck.class);
        when(wrappedCheck.getResourceBundleBaseName()).thenReturn(getClass().getName());
        final CompletableFuture<ResourceBundle> slot = new CompletableFuture<>();
        doAnswer(call -> slot.complete(call.getArgument(0)))
                .when(wrappedCheck).setResourceBundle(nullable(ResourceBundle.class));

        final SilencingCheckFacade facade = new SilencingCheckFacade(wrappedCheck);
        // setResourceBundle is not silenced
        facade.setSilenced(true);
        final ResourceBundle expected = ResourceBundle.getBundle(getClass().getName());
        facade.setResourceBundle(ResourceBundle.getBundle(facade.getResourceBundleBaseName()));
        assertSame("expect same resource bundle", expected, slot.getNow(null));
    }

    @Test(expected = RepositoryException.class)
    public void testBeforeSlingInstall_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final SlingInstallable<?> arg1 = mock(SlingInstallable.class);
        final Session arg2 = mock(Session.class);

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).beforeSlingInstall(
                any(PackageId.class),
                any(SlingInstallable.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.beforeSlingInstall(arg0, arg1, arg2);
    }

    @Test
    public void testBeforeSlingInstall() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final SlingInstallable<?> arg1 = mock(SlingInstallable.class);
        final Session arg2 = mock(Session.class);

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<SlingInstallable<?>> slot1 = new CompletableFuture<>();
        final CompletableFuture<Session> slot2 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, SlingInstallable.class));
            slot2.complete(call.getArgument(2, Session.class));
            return true;
        }).when(delegate).beforeSlingInstall(
                any(PackageId.class),
                any(SlingInstallable.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.beforeSlingInstall(arg0, arg1, arg2);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());
        assertFalse("arg2 should not be done", slot2.isDone());

        alias.setSilenced(false);
        alias.beforeSlingInstall(arg0, arg1, arg2);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
        assertSame("same arg2", arg2, slot2.getNow(null));
    }

    @Test
    public void testIdentifyEmbeddedPackage() {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final PackageId arg1 = PackageId.fromString("my_packages:other_example:1.0");
        final String arg2 = "/some/path";

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<PackageId> slot1 = new CompletableFuture<>();
        final CompletableFuture<String> slot2 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, PackageId.class));
            slot2.complete(call.getArgument(2, String.class));
            return true;
        }).when(delegate).identifyEmbeddedPackage(
                any(PackageId.class),
                any(PackageId.class),
                any(String.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.identifyEmbeddedPackage(arg0, arg1, arg2);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());
        assertFalse("arg2 should not be done", slot2.isDone());

        alias.setSilenced(false);
        alias.identifyEmbeddedPackage(arg0, arg1, arg2);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
        assertSame("same arg2", arg2, slot2.getNow(null));
    }

    @Test(expected = RepositoryException.class)
    public void testAppliedRepoInitScripts_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final SlingInstallable<?> arg1 = mock(SlingInstallable.class);
        final Session arg2 = mock(Session.class);

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).appliedRepoInitScripts(
                any(PackageId.class),
                any(SlingInstallable.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.appliedRepoInitScripts(arg0, arg1, arg2);
    }

    @Test
    public void testAppliedRepoInitScripts() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final SlingInstallable<?> arg1 = mock(SlingInstallable.class);
        final Session arg2 = mock(Session.class);

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<SlingInstallable<?>> slot1 = new CompletableFuture<>();
        final CompletableFuture<Session> slot2 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, SlingInstallable.class));
            slot2.complete(call.getArgument(2, Session.class));
            return true;
        }).when(delegate).appliedRepoInitScripts(
                any(PackageId.class),
                any(SlingInstallable.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.appliedRepoInitScripts(arg0, arg1, arg2);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());
        assertFalse("arg2 should not be done", slot2.isDone());

        alias.setSilenced(false);
        alias.appliedRepoInitScripts(arg0, arg1, arg2);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
        assertSame("same arg2", arg2, slot2.getNow(null));
    }

    @Test(expected = RepositoryException.class)
    public void testAfterScanPackage_throws() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Session arg1 = mock(Session.class);

        final ProgressCheck delegate = mock(ProgressCheck.class);
        doThrow(RepositoryException.class).when(delegate).afterScanPackage(
                any(PackageId.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.afterScanPackage(arg0, arg1);
    }

    @Test
    public void testAfterScanPackage() throws Exception {
        final PackageId arg0 = PackageId.fromString("my_packages:example:1.0");
        final Session arg1 = mock(Session.class);

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<Session> slot1 = new CompletableFuture<>();

        final ProgressCheck delegate = mock(ProgressCheck.class);

        doAnswer(call -> {
            slot0.complete(call.getArgument(0, PackageId.class));
            slot1.complete(call.getArgument(1, Session.class));
            return true;
        }).when(delegate).afterScanPackage(
                any(PackageId.class),
                any(Session.class));

        final SilencingCheckFacade alias = new SilencingCheckFacade(delegate);
        alias.setSilenced(true);
        alias.afterScanPackage(arg0, arg1);

        assertFalse("arg0 should not be done", slot0.isDone());
        assertFalse("arg1 should not be done", slot1.isDone());

        alias.setSilenced(false);
        alias.afterScanPackage(arg0, arg1);

        assertSame("same arg0", arg0, slot0.getNow(null));
        assertSame("same arg1", arg1, slot1.getNow(null));
    }
}