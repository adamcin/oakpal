/*
 * Copyright 2018 Mark Adamcin
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

package net.adamcin.oakpal.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

public class TestPackageUtilTest {
    final File baseDir = new File("target/test-out/TestPackageUtilTest");

    @Before
    public void setUp() throws Exception {
        baseDir.mkdirs();
    }

    @Test
    public void testPrepareTestPackage() throws Exception {
        File fullCoverage = TestPackageUtil.prepareTestPackage("fullcoverage.zip");
        assertTrue("fullcoverage.zip should exist", fullCoverage.exists());

        File simple = TestPackageUtil.prepareTestPackage("package_1.0.zip");
        assertTrue("package_1.0.zip should exist", simple.exists());
    }

    @Test
    public void testPrepareTestPackageFromFolder() throws Exception {
        File simple = TestPackageUtil.prepareTestPackageFromFolder("simple-1.0.zip", new File("src/test/resources/extracted/simple").getAbsoluteFile());
        assertTrue("simple-1.0.zip should exist", simple.exists());

        Set<String> entryNames = getJarEntrySet(simple);
        for (String entryName : entryNames) {
            assertFalse("entryName must not begin with a slash: " + entryName, entryName.startsWith("/"));
        }

        assertTrue("must contain jcr_root/apps/oakpal/.content.xml", entryNames.contains("jcr_root/apps/oakpal/.content.xml"));
    }

    static Set<String> getJarEntrySet(final @NotNull File file) throws Exception {
        final Set<String> entryNames = new LinkedHashSet<>();
        try (final JarFile jarFile = new JarFile(file)) {
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                String entryName = entries.nextElement().getName();
                entryNames.add(entryName);
            }
        }
        return entryNames;
    }

    @Test(expected = IOException.class)
    public void testPrepareTestPackageFromFolder_notDirectory() throws Exception {
        final File notDirectory = new File(baseDir, "notDirectory.txt");
        FileUtils.touch(notDirectory);

        TestPackageUtil.prepareTestPackageFromFolder("simple-1.0.zip", notDirectory.getAbsoluteFile());
    }

    @Test
    public void testLoadPropertiesFromResource() {
        final Properties props = new Properties();
        TestPackageUtil.loadPropertiesFromResource(props, "/no/such/resource.properties");
        assertTrue("props should still be empty", props.isEmpty());

        TestPackageUtil.loadPropertiesFromResource(props, "/test-packages.properties");
        assertEquals("keys are",
                new HashSet<>(Arrays.asList(TestPackageUtil.PN_TEST_PACKAGES_ROOT, TestPackageUtil.PN_TEST_PACKAGES_SRC)),
                props.keySet());
    }

    @Test
    public void testIncludedEntryFilter() {
        final File testOut = new File(baseDir, "testIncludedEntryFilter");
        assertTrue("some file is accepted",
                TestPackageUtil.includedEntry.accept(new File(testOut, "someFile.jar")));
        assertTrue("some file under META-INF",
                TestPackageUtil.includedEntry.accept(new File(testOut, "META-INF"), "someFile.jar"));
        assertFalse("META-INF/MANIFEST.MF is not accepted",
                TestPackageUtil.includedEntry.accept(new File(testOut, "META-INF/MANIFEST.MF")));
        assertFalse("manifest is not accepted under META-INF dir",
                TestPackageUtil.includedEntry.accept(new File(testOut, "META-INF"), "MANIFEST.MF"));
    }

    static <T> Set<T> setOf(final @NotNull T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    @Test
    public void testBuildJarFromDir() throws Exception {
        final File testOut = new File(baseDir, "testBuildJarFromDir");
        if (testOut.exists()) {
            FileUtils.deleteDirectory(testOut);
        }
        testOut.mkdirs();
        final File embeddedJar = new File(testOut, "embedded.jar");
        final File outJar = new File(testOut, "simple.jar");

        TestPackageUtil.buildJarFromDir(new File("src/test/resources/embedded"),
                embeddedJar, Collections.emptyMap());

        assertEquals("embedded jar has entries",
                setOf("META-INF/", "META-INF/MANIFEST.MF", "someScript.js"), getJarEntrySet(embeddedJar));

        assertEquals("replace works", "jcr_root/apps/oakpal/install/",
                "jcr_root/apps/oakpal/install/".replaceFirst("/?$", "/"));
        Map<String, File> addEntries = new LinkedHashMap<>();
        addEntries.put("", embeddedJar.getParentFile());
        addEntries.put("jcr_root/apps/oakpal/install/", embeddedJar.getParentFile());
        addEntries.put("jcr_root/apps/oakpal/install/embedded.jar/", embeddedJar);
        TestPackageUtil.buildJarFromDir(new File("src/test/resources/extracted/simple"),
                outJar, addEntries);

        assertEquals("simple jar has entries",
                setOf(
                        "META-INF/",
                        "META-INF/vault/",
                        "META-INF/vault/filter.xml",
                        "META-INF/vault/properties.xml",
                        "jcr_root/",
                        "jcr_root/apps/",
                        "jcr_root/apps/oakpal/",
                        "jcr_root/apps/oakpal/.content.xml",
                        "jcr_root/apps/oakpal/install/",
                        "jcr_root/apps/oakpal/install/embedded.jar"
                ),
                getJarEntrySet(outJar));
    }

    @Test(expected = IOException.class)
    public void testBuildJarFromDir_parentDirIsFile() throws Exception {
        final File testOut = new File(baseDir, "testBuildJarFromDir_parentDirIsFile");
        if (testOut.exists()) {
            FileUtils.deleteDirectory(testOut);
        }
        testOut.mkdirs();
        final File parentDir = new File(testOut, "parentDir");
        final File outJar = new File(parentDir, "out.jar");
        FileUtils.touch(outJar.getParentFile());
        TestPackageUtil.buildJarFromDir(new File("src/test/resources/extracted/simple"),
                outJar, Collections.emptyMap());
    }
}
