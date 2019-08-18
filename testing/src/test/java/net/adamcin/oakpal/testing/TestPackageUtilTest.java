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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;

public class TestPackageUtilTest {

    @Test
    public void testPrepareTestPackage() throws Exception {
        File fullCoverage = TestPackageUtil.prepareTestPackage("fullcoverage.zip");
        assertTrue("fullcoverage.zip should exist", fullCoverage.exists());

        File simple = TestPackageUtil.prepareTestPackage("package_1.0.zip");
        assertTrue("package_1.0.zip should exist", simple.exists());
    }

    @Test
    public void testPrepareTestPackageFromFolder() throws Exception {
        System.out.println(new File("src/test/resources/extracted/simple").getAbsolutePath());
        File simple = TestPackageUtil.prepareTestPackageFromFolder("simple-1.0.zip", new File("src/test/resources/extracted/simple").getAbsoluteFile());
        assertTrue("simple-1.0.zip should exist", simple.exists());

        JarFile simpleJar = new JarFile(simple);
        Set<String> entryNames = new HashSet<>();
        for (Enumeration<JarEntry> entries = simpleJar.entries(); entries.hasMoreElements(); ) {
            String entryName = entries.nextElement().getName();
            entryNames.add(entryName);
        }

        for (String entryName : entryNames) {
            assertFalse("entryName must not begin with a slash: " + entryName, entryName.startsWith("/"));
        }

        assertTrue("must contain jcr_root/apps/oakpal/.content.xml", entryNames.contains("jcr_root/apps/oakpal/.content.xml"));
    }

}
