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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

public class EmbeddedPackageInstallableTest {
    @Test
    public void testConstructorAndGetters() {
        final PackageId expectParentId = PackageId.fromString("test:test:1");
        final String expectJcrPath = "/some/path";
        final PackageId expectSubpackageId = PackageId.fromString("test:testtest:1");
        final EmbeddedPackageInstallable installable =
                new EmbeddedPackageInstallable(expectParentId, expectJcrPath, expectSubpackageId);

        assertSame("expect parentId", expectParentId, installable.getParentId());
        assertSame("expect jcrPath", expectJcrPath, installable.getJcrPath());
        assertSame("expect subpackageId", expectSubpackageId, installable.getEmbeddedId());
    }

    @Test
    public void testEquals() {
        EmbeddedPackageInstallable self = new EmbeddedPackageInstallable(
                PackageId.fromString("test:test:1"),
                "/some/path",
                PackageId.fromString("test:testtest:1"));

        assertEquals("expect self to equal self", self, self);
        assertFalse("expect self to not equal null", self.equals(null));

        assertEquals("expect self to equal other same params", self,
                new EmbeddedPackageInstallable(
                        self.getParentId(),
                        self.getJcrPath(),
                        self.getEmbeddedId()));
        assertNotEquals("expect self to equal other different path", self,
                new EmbeddedPackageInstallable(
                        self.getParentId(),
                        "/some/other/path",
                        self.getEmbeddedId()));
        assertNotEquals("expect self to equal other different parent id", self,
                new EmbeddedPackageInstallable(
                        self.getEmbeddedId(),
                        self.getJcrPath(),
                        self.getEmbeddedId()));
        assertNotEquals("expect self to equal other different embedded id", self,
                new EmbeddedPackageInstallable(
                        self.getParentId(),
                        self.getJcrPath(),
                        self.getParentId()));
    }

    @Test
    public void testHashCode() {
        EmbeddedPackageInstallable self = new EmbeddedPackageInstallable(
                PackageId.fromString("test:test:1"),
                "/some/path",
                PackageId.fromString("test:testtest:1"));

        assertEquals("expect self to equal self", self.hashCode(), self.hashCode());

        assertEquals("expect self to equal other same params", self.hashCode(),
                new EmbeddedPackageInstallable(
                        self.getParentId(),
                        self.getJcrPath(),
                        self.getEmbeddedId()).hashCode());
        assertNotEquals("expect self to equal other different path", self.hashCode(),
                new EmbeddedPackageInstallable(
                        self.getParentId(),
                        "/some/other/path",
                        self.getEmbeddedId()).hashCode());
        assertNotEquals("expect self to equal other different parent id", self.hashCode(),
                new EmbeddedPackageInstallable(
                        self.getEmbeddedId(),
                        self.getJcrPath(),
                        self.getEmbeddedId()).hashCode());
        assertNotEquals("expect self to equal other different embedded id", self.hashCode(),
                new EmbeddedPackageInstallable(
                        self.getParentId(),
                        self.getJcrPath(),
                        self.getParentId()).hashCode());
    }
}