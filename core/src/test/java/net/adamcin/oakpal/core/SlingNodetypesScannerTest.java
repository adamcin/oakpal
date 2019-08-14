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

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static org.junit.Assert.assertEquals;

public class SlingNodetypesScannerTest {

    final File baseDir = new File("src/test/resources/SlingNodetypesScannerTest");
    final File testTarget = new File("target/test-output/SlingNodetypesScannerTest");

    final File alphaSrc = new File(baseDir, "has_a_src");
    final File bravoSrc = new File(baseDir, "has_b_src");
    final File charlieSrc = new File(baseDir, "has_c_src");
    final File deltaSrc = new File(baseDir, "has_d_src");
    final File echoSrc = new File(baseDir, "has_e_src");
    final File foxtrotSrc = new File(baseDir, "has_f_src");
    final File yankeeSrc = new File(baseDir, "has_y_src");
    final File zuluSrc = new File(baseDir, "has_z_src");

    final File alphaTgt = new File(testTarget, "has_a.jar");
    final File bravoTgt = new File(testTarget, "has_b.jar");
    final File charlieTgt = new File(testTarget, "has_c.jar");
    final File deltaTgt = new File(testTarget, "has_d.jar");
    final File echoTgt = new File(testTarget, "has_e.jar");
    final File foxtrotTgt = new File(testTarget, "has_f.jar");
    final File yankeeTgt = new File(testTarget, "has_y.jar");
    final File zuluTgt = new File(testTarget, "has_z.jar");

    private Stream<File> getDirs() {
        return Stream.of(alphaSrc, bravoSrc, charlieSrc, deltaSrc,
                echoSrc, foxtrotSrc, yankeeSrc, zuluSrc);
    }

    private Stream<File> getJars() {
        return Stream.of(alphaTgt, bravoTgt, charlieTgt, deltaTgt,
                echoTgt, foxtrotTgt, yankeeTgt, zuluTgt);
    }

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteDirectory(testTarget);
        testTarget.mkdirs();
        TestPackageUtil.buildJarFromDir(alphaSrc, alphaTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(bravoSrc, bravoTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(charlieSrc, charlieTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(deltaSrc, deltaTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(echoSrc, echoTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(foxtrotSrc, foxtrotTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(yankeeSrc, yankeeTgt, Collections.emptyMap());
        TestPackageUtil.buildJarFromDir(zuluSrc, zuluTgt, Collections.emptyMap());
    }

    @Test
    public void testFindNodeTypeDefinitions_classLoader() throws Exception {
        final ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    new URLClassLoader(getJars().map(compose(File::toURI, uncheck1(URI::toURL))).toArray(URL[]::new),
                            null));
            List<URL> urls = SlingNodetypesScanner.findNodeTypeDefinitions();
            assertEquals("urls size should be: " + urls, 8, urls.size());

            Map<String, URL> namedUrls = SlingNodetypesScanner
                    .resolveNodeTypeDefinitions(Arrays.asList("b.cnd", "c.cnd", "z.cnd"));
            assertEquals("should be same set of names",
                    new HashSet<>(Arrays.asList("b.cnd", "c.cnd", "z.cnd")), namedUrls.keySet());
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    @Test
    public void testFindNodeTypeDefinitions_zips() throws Exception {
        final List<URL> jarUrls = SlingNodetypesScanner
                .findNodeTypeDefinitions(getJars().collect(Collectors.toList()));
        assertEquals("jar urls size should be: " + jarUrls, 8, jarUrls.size());
        final List<URL> dirUrls = SlingNodetypesScanner
                .findNodeTypeDefinitions(getDirs().collect(Collectors.toList()));
        assertEquals("dir urls size should be: " + dirUrls, 8, dirUrls.size());
    }
}