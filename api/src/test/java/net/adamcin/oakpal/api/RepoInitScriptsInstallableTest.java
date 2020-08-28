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
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class RepoInitScriptsInstallableTest {

    @Test
    public void testConstructorAndGetters() {
        final PackageId expectParentId = PackageId.fromString("test:test:1");
        final String expectJcrPath = "/some/path";
        final RepoInitScriptsInstallable installable = new RepoInitScriptsInstallable(expectParentId, expectJcrPath,
                Collections.emptyList());

        assertSame("expect parentId", expectParentId, installable.getParentId());
        assertSame("expect jcrPath", expectJcrPath, installable.getJcrPath());
        assertNotNull("expect not null", installable.getScripts());
    }
}