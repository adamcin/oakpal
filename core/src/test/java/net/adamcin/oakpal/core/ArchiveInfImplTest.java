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

import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.packaging.SubPackageHandling;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.junit.Test;

import java.io.InputStream;
import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArchiveInfImplTest {

    @Test
    public void testConstruct() {
        final PackageProperties packageProperties = mock(PackageProperties.class);
        final Archive archive = mock(Archive.class);
        final ArchiveInfImpl inf = new ArchiveInfImpl(packageProperties, archive);
        assertSame("same properties", packageProperties, inf.packageProperties);
        assertSame("same archive", archive, inf.archive);
    }

    @Test
    public void testFactory() throws Exception {
        final PackageId packageId = PackageId.fromString("my_packages:test:1.0-SNAPSHOT");
        final VaultPackage vaultPackage = mock(VaultPackage.class);
        final PackageProperties packageProperties = mock(PackageProperties.class);
        final Archive archive = mock(Archive.class);
        when(vaultPackage.isValid()).thenReturn(true);
        when(vaultPackage.getProperties()).thenReturn(packageProperties);
        when(vaultPackage.getArchive()).thenReturn(archive);
        final ArchiveInfImpl inf = ArchiveInfImpl.readInf(packageId, vaultPackage);
        assertSame("same properties", packageProperties, inf.packageProperties);
        assertSame("same archive", archive, inf.archive);
    }

    @Test(expected = PackageException.class)
    public void testFactory_throws() throws Exception {
        final PackageId packageId = PackageId.fromString("my_packages:test:1.0-SNAPSHOT");
        final VaultPackage vaultPackage = mock(VaultPackage.class);
        final PackageProperties packageProperties = mock(PackageProperties.class);
        final Archive archive = mock(Archive.class);
        when(vaultPackage.isValid()).thenReturn(false);
        when(vaultPackage.getProperties()).thenReturn(packageProperties);
        when(vaultPackage.getArchive()).thenReturn(archive);
        ArchiveInfImpl.readInf(packageId, vaultPackage);
    }

    private ArchiveInfImpl getInf() {
        final PackageProperties packageProperties = mock(PackageProperties.class);
        final Archive archive = mock(Archive.class);
        return new ArchiveInfImpl(packageProperties, archive);
    }

    @Test
    public void testOpenInputStream() throws Exception {
        final ArchiveInfImpl inf = getInf();
        final InputStream inputStream = mock(InputStream.class);
        final Archive.Entry entry = mock(Archive.Entry.class);
        when(inf.archive.openInputStream(entry)).thenReturn(inputStream);
        assertSame("same inputStream", inputStream, inf.openInputStream(entry));
    }

    @Test
    public void testGetInputSource() throws Exception {
        final ArchiveInfImpl inf = getInf();
        final VaultInputSource inputSource = mock(VaultInputSource.class);
        final Archive.Entry entry = mock(Archive.Entry.class);
        when(inf.archive.getInputSource(entry)).thenReturn(inputSource);
        assertSame("same inputSource", inputSource, inf.getInputSource(entry));
    }

    @Test
    public void testGetJcrRoot() throws Exception {
        final ArchiveInfImpl inf = getInf();
        final Archive.Entry entry = mock(Archive.Entry.class);
        when(inf.archive.getJcrRoot()).thenReturn(entry);
        assertSame("same entry", entry, inf.getJcrRoot());
    }

    @Test
    public void testGetRoot() throws Exception {
        final ArchiveInfImpl inf = getInf();
        final Archive.Entry entry = mock(Archive.Entry.class);
        when(inf.archive.getRoot()).thenReturn(entry);
        assertSame("same entry", entry, inf.getRoot());
    }

    @Test
    public void testGetMetaInf() {
        final ArchiveInfImpl inf = getInf();
        final MetaInf metaInf = mock(MetaInf.class);
        when(inf.archive.getMetaInf()).thenReturn(metaInf);
        assertSame("same metaInf", metaInf, inf.getMetaInf());
    }

    @Test
    public void testGetEntry() throws Exception {
        final ArchiveInfImpl inf = getInf();
        final String path = "/some/path";
        final Archive.Entry entry = mock(Archive.Entry.class);
        when(inf.archive.getEntry(path)).thenReturn(entry);
        assertSame("same entry", entry, inf.getEntry(path));
    }

    @Test
    public void testGetId() {
        final ArchiveInfImpl inf = getInf();
        final PackageId packageId = PackageId.fromString("test:packs:1.0");
        when(inf.packageProperties.getId()).thenReturn(packageId);
        assertSame("same id", packageId, inf.getId());
    }

    @Test
    public void testGetLastModified() {
        final ArchiveInfImpl inf = getInf();
        final Calendar expect = Calendar.getInstance();
        when(inf.packageProperties.getLastModified()).thenReturn(expect);
        assertSame("expect same", expect, inf.getLastModified());
    }

    @Test
    public void testGetCreated() {
        final ArchiveInfImpl inf = getInf();
        final Calendar expect = Calendar.getInstance();
        when(inf.packageProperties.getCreated()).thenReturn(expect);
        assertSame("expect same", expect, inf.getCreated());
    }

    @Test
    public void testGetLastWrapped() {
        final ArchiveInfImpl inf = getInf();
        final Calendar expect = Calendar.getInstance();
        when(inf.packageProperties.getLastWrapped()).thenReturn(expect);
        assertSame("expect same", expect, inf.getLastWrapped());
    }

    @Test
    public void testGetDateProperty() {
        final ArchiveInfImpl inf = getInf();
        final String name = "name";
        final Calendar expect = Calendar.getInstance();
        when(inf.packageProperties.getDateProperty(name)).thenReturn(expect);
        assertSame("expect same", expect, inf.getDateProperty(name));
    }

    @Test
    public void testGetProperty() {
        final ArchiveInfImpl inf = getInf();
        final String name = "name";
        final String expect = "expect";
        when(inf.packageProperties.getProperty(name)).thenReturn(expect);
        assertSame("expect same", expect, inf.getProperty(name));
    }

    @Test
    public void testGetLastModifiedBy() {
        final ArchiveInfImpl inf = getInf();
        final String expect = "expect";
        when(inf.packageProperties.getLastModifiedBy()).thenReturn(expect);
        assertSame("expect same", expect, inf.getLastModifiedBy());
    }

    @Test
    public void testGetCreatedBy() {
        final ArchiveInfImpl inf = getInf();
        final String expect = "expect";
        when(inf.packageProperties.getCreatedBy()).thenReturn(expect);
        assertSame("expect same", expect, inf.getCreatedBy());
    }

    @Test
    public void testGetLastWrappedBy() {
        final ArchiveInfImpl inf = getInf();
        final String expect = "expect";
        when(inf.packageProperties.getLastWrappedBy()).thenReturn(expect);
        assertSame("expect same", expect, inf.getLastWrappedBy());
    }

    @Test
    public void testGetDescription() {
        final ArchiveInfImpl inf = getInf();
        final String expect = "expect";
        when(inf.packageProperties.getDescription()).thenReturn(expect);
        assertSame("expect same", expect, inf.getDescription());
    }

    @Test
    public void testRequiresRoot() {
        final ArchiveInfImpl inf = getInf();
        when(inf.packageProperties.requiresRoot()).thenReturn(false);
        assertFalse("expect false", inf.requiresRoot());
        when(inf.packageProperties.requiresRoot()).thenReturn(true);
        assertTrue("expect true", inf.requiresRoot());
    }

    @Test
    public void testGetDependencies() {
        final ArchiveInfImpl inf = getInf();
        final Dependency[] expect = new Dependency[0];
        when(inf.packageProperties.getDependencies()).thenReturn(expect);
        assertSame("expect same", expect, inf.getDependencies());
    }

    @Test
    public void testGetACHandling() {
        final ArchiveInfImpl inf = getInf();
        final AccessControlHandling expect = AccessControlHandling.CLEAR;
        when(inf.packageProperties.getACHandling()).thenReturn(expect);
        assertSame("expect same", expect, inf.getACHandling());
    }

    @Test
    public void testGetSubPackageHandling() {
        final ArchiveInfImpl inf = getInf();
        final SubPackageHandling expect = SubPackageHandling.DEFAULT;
        when(inf.packageProperties.getSubPackageHandling()).thenReturn(expect);
        assertSame("expect same", expect, inf.getSubPackageHandling());
    }

    @Test
    public void testGetPackageType() {
        final ArchiveInfImpl inf = getInf();
        final PackageType expect = PackageType.CONTENT;
        when(inf.packageProperties.getPackageType()).thenReturn(expect);
        assertSame("expect same", expect, inf.getPackageType());
    }
}
