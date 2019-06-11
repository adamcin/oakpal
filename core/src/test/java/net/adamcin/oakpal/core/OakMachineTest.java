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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Manifest;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

public class OakMachineTest {

    @Test
    public void testJcrSession() throws Exception {
        OakMachine machine = new OakMachine.Builder().build();
        machine.initAndInspect(session -> {
            assertTrue("Root node should be same as / node",
                    session.getRootNode().isSame(session.getNode("/")));
        });
    }

    @Test
    public void testHomePaths() throws Exception {
        OakMachine machine = new OakMachine.Builder().build();
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

        new OakMachine.Builder().withProgressChecks(check).build().scanPackage(testPackage);
        assertTrue("manifest was read, and produced correct value for Content-Package-Id",
                manifestWasRead.isDone() && manifestWasRead.get());
    }
}
