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

package net.adamcin.oakpal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

public class UtilTest {

    @Before
    public void setUp() throws Exception {
        assertTrue("make sure that we have bound the expected logback-classic slf4j impl. actual: " +
                        StaticLoggerBinder.getSingleton().getLoggerFactoryClassStr(),
                StaticLoggerBinder.getSingleton().getLoggerFactory() instanceof LoggerContext);
    }

    LoggerContext getLoggerFactory() {
        return (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
    }

    @Test
    public void testGetManifestHeaderValues() throws IOException {
        final Manifest manifest;
        try (FileInputStream inputStream = new FileInputStream(new File("src/test/resources/manifestWithManyHeaders.mf"))) {
            manifest = new Manifest(inputStream);

            List<String> numbers = Util.getManifestHeaderValues(manifest, "Numbers");

            assertTrue("numbers contains one", numbers.contains("one"));
            assertTrue("numbers contains two", numbers.contains("two"));
            assertTrue("numbers contains three", numbers.contains("three"));

            List<String> paths = Util.getManifestHeaderValues(manifest, "Paths");

            assertTrue("paths contains /test/one", paths.contains("/test/one"));
            assertTrue("paths contains /test/two", paths.contains("/test/two"));
            assertTrue("paths contains /test/three", paths.contains("/test/three"));
        }
    }

    @Test
    public void testResolveManifestResources() throws Exception {
        final File mfFile = new File("src/test/resources/utiljar/META-INF/MANIFEST.MF");
        final URL mfUrl = mfFile.toURI().toURL();
        final Manifest manifest;
        final Logger logger = getLoggerFactory().getLogger(Util.class);
        final Level oldLevel = logger.getLevel();
        try (InputStream inputStream = mfUrl.openStream()) {
            manifest = new Manifest(inputStream);
            logger.setLevel(Level.DEBUG);
            List<String> goodRelPaths = Util.getManifestHeaderValues(manifest, "Good-RelPaths");
            assertEquals("should be same length for Good-RelPaths",
                    goodRelPaths.size(), Util.resolveManifestResources(mfUrl, goodRelPaths).size());

            List<String> badRelPaths = Util.getManifestHeaderValues(manifest, "Bad-RelPaths");
            assertNotEquals("should be different length for Bad-RelPaths",
                    badRelPaths.size(), Util.resolveManifestResources(mfUrl, badRelPaths).size());
        } finally {
            logger.setLevel(oldLevel);
        }
    }

    @Test
    public void testMapManifestHeaderResources() throws Exception {
        final Logger logger = getLoggerFactory().getLogger(Util.class);
        final Level oldLevel = logger.getLevel();
        try {
            final File mfFile = new File("src/test/resources/utiljar");
            Map<URL, List<URL>> mapped = Util.mapManifestHeaderResources("Good-RelPaths",
                    Collections.singletonList(mfFile));
            assertEquals("Expect two good paths: " + mapped, 2,
                    mapped.values().iterator().next().size());
        } finally {
            logger.setLevel(oldLevel);
        }
    }

    @Test
    public void testDebugFilter() {
        LoggerContext context = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger logger = context.getLogger("testDebugFilter");
        logger.setLevel(Level.DEBUG);
        assertEquals("debugFilter should not interfere with stream", Arrays.asList("one", "two", "three"),
                Stream.of("one", "two", "three")
                        .filter(Util.debugFilter(logger, "counting {}"))
                        .collect(Collectors.toList()));
    }

    @Test
    public void testTraceFilter() {
        LoggerContext context = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger logger = context.getLogger("testTraceFilter");
        logger.setLevel(Level.TRACE);
        assertEquals("traceFilter should not interfere with stream", Arrays.asList("one", "two", "three"),
                Stream.of("one", "two", "three")
                        .filter(Util.traceFilter(logger, "counting {}"))
                        .collect(Collectors.toList()));
    }

    @Test
    public void testComposeTry() {
        final AtomicInteger counter = new AtomicInteger(0);

        assertEquals("skip divide by 0 with no side effects",
                Arrays.asList(3, 4, 6, 12),
                Stream.of(4, 3, 2, 1, 0)
                        .flatMap(Util.composeTry(
                                Stream::of,
                                Stream::empty,
                                (value) -> 12 / value, null))
                        .collect(Collectors.toList()));

        assertEquals("skip divide by 0 with counter update as side effect",
                Arrays.asList(3, 4, 6, 12),
                Stream.of(4, 3, 2, 1, 0)
                        .flatMap(Util.composeTry(
                                Stream::of,
                                Stream::empty,
                                (value) -> 12 / value,
                                (element, error) -> counter.incrementAndGet()))
                        .collect(Collectors.toList()));

        assertEquals("counter should have incremented to 1", 1, counter.get());
    }

    @Test
    public void testGetDefaultClassLoader() throws Exception {
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader newCl = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            Thread.currentThread().setContextClassLoader(newCl);
            assertSame("default classLoader should be same as tccl when set",
                    newCl, Util.getDefaultClassLoader());
            Thread.currentThread().setContextClassLoader(null);
            assertSame("default classLoader should be same as Util.class.getClassLoader() when tccl is null",
                    Util.class.getClassLoader(), Util.getDefaultClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }
}
