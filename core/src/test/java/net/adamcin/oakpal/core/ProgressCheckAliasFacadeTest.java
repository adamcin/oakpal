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

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Violation;
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
import java.util.concurrent.CompletableFuture;
import java.util.jar.Manifest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProgressCheckAliasFacadeTest {

    @Test
    public void testGetCheckName() {
        final ProgressCheck delegate = mock(ProgressCheck.class);
        when(delegate.getCheckName()).thenReturn("delegate");
        final ProgressCheckAliasFacade withoutAlias = new ProgressCheckAliasFacade(delegate, null);
        assertEquals("delegate name", "delegate", withoutAlias.getCheckName());
        final ProgressCheckAliasFacade withAlias = new ProgressCheckAliasFacade(delegate, "alias");
        assertEquals("alias name", "alias", withAlias.getCheckName());
    }

    private static class DelegateSuccessfullyCalledException extends RuntimeException {
    }

    @Test
    public void testStartedScan() {
        final CompletableFuture<Boolean> didIt = new CompletableFuture<>();
        final ProgressCheck delegate = mock(ProgressCheck.class);
        doAnswer(call -> didIt.complete(true)).when(delegate).startedScan();
        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
        alias.startedScan();
        assertTrue("did it", didIt.getNow(false));
    }

    @Test
    public void testGetReportedViolations() {
        final Collection<Violation> violations = new ArrayList<>();
        final ProgressCheck delegate = mock(ProgressCheck.class);
        when(delegate.getReportedViolations()).thenReturn(violations);
        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
        assertSame("same value", violations, alias.getReportedViolations());
    }

    @Test
    public void testFinishedScan() {
        final CompletableFuture<Boolean> didIt = new CompletableFuture<>();
        final ProgressCheck delegate = mock(ProgressCheck.class);
        doAnswer(call -> didIt.complete(true)).when(delegate).finishedScan();
        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
        alias.finishedScan();
        assertTrue("did it", didIt.getNow(false));
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade alias = new ProgressCheckAliasFacade(delegate, null);
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

        final ProgressCheckAliasFacade facade = new ProgressCheckAliasFacade(wrappedCheck, "wrapper");
        final ResourceBundle expected = ResourceBundle.getBundle(getClass().getName());
        facade.setResourceBundle(ResourceBundle.getBundle(facade.getResourceBundleBaseName()));
        assertSame("expect same resource bundle", expected, slot.getNow(null));
    }
}