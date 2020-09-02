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

package net.adamcin.oakpal.core.sling;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class EmbeddedPackageInstallableParamsTest {

    @Test
    public void testConstructor() {
        final PackageId expectId = PackageId.fromString("test");
        EmbeddedPackageInstallableParams params = new EmbeddedPackageInstallableParams(expectId);
        assertSame("expect same embeddedId", expectId, params.getEmbeddedId());
        assertSame("expect correct installableType", EmbeddedPackageInstallable.class,
                params.getInstallableType());
    }

    @Test
    public void testCreateInstallable() {
        final PackageId expectParentId = PackageId.fromString("test");
        final String expectJcrPath = "/some/package/path.zip";
        final PackageId expectEmbeddedId = PackageId.fromString("testtest");
        EmbeddedPackageInstallableParams params = new EmbeddedPackageInstallableParams(expectEmbeddedId);
        EmbeddedPackageInstallable installable = params.createInstallable(expectParentId, expectJcrPath);

        assertSame("expect same parentId", expectParentId, installable.getParentId());
        assertSame("expect same jcrPath", expectJcrPath, installable.getJcrPath());
        assertSame("expect same embeddedId", expectEmbeddedId, installable.getEmbeddedId());
    }
}