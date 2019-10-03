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
import org.jetbrains.annotations.NotNull;
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

import static net.adamcin.oakpal.api.Fun.compose;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.Fun.uncheckVoid1;
import static org.junit.Assert.assertEquals;

public class SlingNodetypesScannerTest {

    final File baseDir = new File("src/test/resources/SlingNodetypesScannerTest");
    final File testTarget = new File("target/test-output/SlingNodetypesScannerTest");

    final String alphaSrc = "has_a_src";
    final String bravoSrc = "has_b_src";
    final String charlieSrc = "has_c_src";
    final String deltaSrc = "has_d_src";
    final String echoSrc = "has_e_src";
    final String foxtrotSrc = "has_f_src";
    final String yankeeSrc = "has_y_src";
    final String zuluSrc = "has_z_src";

    private Stream<String> getSrcNames() {
        return Stream.of(alphaSrc, bravoSrc, charlieSrc, deltaSrc,
                echoSrc, foxtrotSrc, yankeeSrc, zuluSrc);
    }
    
    private String getJarName(final @NotNull String srcName) {
        return srcName.replace("_src", ".jar");
    }

    @Before
    public void setUp() throws Exception {
        testTarget.mkdirs();

    }
    
    private void buildAllJars(final @NotNull File targetBaseDir) throws Exception {
        getSrcNames().forEachOrdered(uncheckVoid1(name ->
            TestPackageUtil.buildJarFromDir(new File(baseDir, name), new File(targetBaseDir, getJarName(name)), Collections.emptyMap())
        ));
    }

    private Stream<File> getJars(final @NotNull File targetBaseDir) {
        return getSrcNames().map(name -> new File(targetBaseDir, getJarName(name)));
    }

    private Stream<File> getDirs() {
        return getSrcNames().map(name -> new File(baseDir, name));
    }

    @Test
    public void testFindNodeTypeDefinitions_classLoader() throws Exception {
        final File testBaseDir = new File(testTarget, "testFindNodeTypeDefinitions_classLoader");
        FileUtils.deleteDirectory(testBaseDir);
        testBaseDir.mkdirs();
        buildAllJars(testBaseDir);
        final ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    new URLClassLoader(getJars(testBaseDir).map(compose(File::toURI, uncheck1(URI::toURL))).toArray(URL[]::new),
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
        final File testBaseDir = new File(testTarget, "testFindNodeTypeDefinitions_zips");
        FileUtils.deleteDirectory(testBaseDir);
        testBaseDir.mkdirs();
        buildAllJars(testBaseDir);
        final List<URL> jarUrls = SlingNodetypesScanner
                .findNodeTypeDefinitions(getJars(testBaseDir).collect(Collectors.toList()));
        assertEquals("jar urls size should be: " + jarUrls, 8, jarUrls.size());
        final List<URL> dirUrls = SlingNodetypesScanner
                .findNodeTypeDefinitions(getDirs().collect(Collectors.toList()));
        assertEquals("dir urls size should be: " + dirUrls, 8, dirUrls.size());
    }
}