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

package net.adamcin.oakpal.api;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.Node;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

public class ProgressCheckTest {

    @Test
    public void testDefaultMethods() throws Exception {
        ProgressCheck mock = new ProgressCheck() {
            @Override
            public Collection<Violation> getReportedViolations() {
                return Collections.emptyList();
            }
        };

        Assert.assertNotNull("expect nonnull checkName", mock.getCheckName());
        mock.simulateSling(null, null);
        mock.startedScan();
        mock.identifyPackage(null, null);
        mock.readManifest(null, null);
        mock.beforeExtract(null, null, null, null, null);
        mock.importedPath(null, null, null);
        mock.importedPath(null, null, null, null);
        mock.deletedPath(null, null, null);
        mock.afterExtract(null, null);
        mock.identifySubpackage(null, null);
        mock.beforeSlingInstall(null, null, null);
        mock.identifyEmbeddedPackage(null, null, null);
        mock.appliedRepoInitScripts(null, null, null, null);
        mock.afterScanPackage(null, null);
        mock.finishedScan();
    }

    @Test
    public void testImportedPathDelegation() throws Exception {
        final ProgressCheck check = mock(ProgressCheck.class);
        doCallRealMethod().when(check).importedPath(
                nullable(PackageId.class), nullable(String.class), nullable(Node.class), nullable(PathAction.class));

        final CompletableFuture<PackageId> slot0 = new CompletableFuture<>();
        final CompletableFuture<String> slot1 = new CompletableFuture<>();
        final CompletableFuture<Node> slot2 = new CompletableFuture<>();
        doAnswer(call -> {
            slot0.complete(call.getArgument(0));
            slot1.complete(call.getArgument(1));
            slot2.complete(call.getArgument(2));
            return true;
        }).when(check).importedPath(any(PackageId.class), any(String.class), any(Node.class));

        final PackageId expectParam0 = PackageId.fromString("group:sub");
        final String expectParam1 = "/apps";
        final Node expectParam2 = mock(Node.class);

        check.importedPath(expectParam0, expectParam1, expectParam2, PathAction.NOOP);

        assertSame("expect param0", expectParam0, slot0.getNow(null));
        assertSame("expect param1", expectParam1, slot1.getNow(null));
        assertSame("expect param2", expectParam2, slot2.getNow(null));
    }
}
