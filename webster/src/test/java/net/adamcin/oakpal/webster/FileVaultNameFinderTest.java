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

package net.adamcin.oakpal.webster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.QName;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.fs.io.FileArchive;
import org.apache.jackrabbit.vault.fs.io.ZipArchive;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileVaultNameFinderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVaultNameFinderTest.class);

    @Test
    public void test_zip_noReferences() throws Exception {
        File p = TestPackageUtil.prepareTestPackage("subtest_with_nodetypes.zip");
        ZipArchive archive = new ZipArchive(p);
        FileVaultNameFinder finder = new FileVaultNameFinder();
        Set<QName> names = finder.search(archive);

        LOGGER.info("names: {}", names);
        assertTrue("name search should be empty", names.isEmpty());
    }

    @Test
    public void test_file_oneNtRef() throws Exception {
        File p = new File("src/test/resources/filevault/oneNtRef");
        FileArchive archive = new FileArchive(p);
        FileVaultNameFinder finder = new FileVaultNameFinder();
        Set<QName> names = finder.search(archive);

        LOGGER.info("names: {}", names);
        assertEquals("name search should return a single reference", 1, names.size());
    }

    @Test
    public void test_file_noReferences() throws Exception {
        File p = new File("src/test/resources/filevault/noReferences");
        FileArchive archive = new FileArchive(p);
        FileVaultNameFinder finder = new FileVaultNameFinder();
        Set<QName> names = finder.search(archive);

        LOGGER.info("names: {}", names);
        assertTrue("name search should return no references", names.isEmpty());
    }

    static Set<String> toStrings(final Set<QName> qNames) {
        return qNames.stream().map(QName::toString).collect(Collectors.toSet());
    }

    @Test
    public void test_file_oneNtRefAndDef() throws Exception {
        File p = new File("src/test/resources/filevault/oneNtRefAndDef");
        FileArchive archive = new FileArchive(p);
        FileVaultNameFinder finder = new FileVaultNameFinder();
        Set<String> names = toStrings(finder.search(archive));

        LOGGER.info("names: {}", names);
        assertEquals("name search should return one nodetype reference", Collections.singleton("foo:mix"), names);
    }

    @Test
    public void test_file_onePrivRef() throws Exception {
        File p = new File("src/test/resources/filevault/onePrivRef");
        FileArchive archive = new FileArchive(p);
        FileVaultNameFinder finder = new FileVaultNameFinder();
        Set<String> names = toStrings(finder.search(archive));

        LOGGER.info("names: {}", names);
        assertEquals("name search should return one privilege reference",
                Collections.singleton("foo:canBar"), names);
    }

    @Test
    public void test_file_onePrivRefAndDef() throws Exception {
        File p = new File("src/test/resources/filevault/onePrivRefAndDef");
        FileArchive archive = new FileArchive(p);
        FileVaultNameFinder finder = new FileVaultNameFinder();
        Set<String> names = toStrings(finder.search(archive));

        LOGGER.info("names: {}", names);
        assertEquals("name search should return one privilege reference",
                Collections.singleton("crx:replicate"), names);
    }
}
