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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.Manifest;

import org.junit.Test;

public class UtilTest {

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
}
