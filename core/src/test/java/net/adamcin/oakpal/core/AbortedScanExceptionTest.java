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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.function.Function;
import javax.jcr.Node;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Test;

public class AbortedScanExceptionTest {

    @Test
    public void testConstruct() throws Exception {
        assertFalse("simple construct has non-present package id",
                new AbortedScanException(new NullPointerException()).getCurrentPackageFile().isPresent());
        assertFalse("simple construct has non-present package id",
                new AbortedScanException(new NullPointerException()).getCurrentPackageUrl().isPresent());
        assertFalse("simple construct has non-present package id",
                new AbortedScanException(new NullPointerException()).getCurrentPackageNode().isPresent());

        File file = TestPackageUtil.prepareTestPackage("package_1.0.zip");

        assertTrue("construct with package file",
                new AbortedScanException(new NullPointerException(), file).getCurrentPackageFile().isPresent());
        assertFalse("construct with package file",
                new AbortedScanException(new NullPointerException(), file).getCurrentPackageUrl().isPresent());
        assertFalse("construct with package file",
                new AbortedScanException(new NullPointerException(), file).getCurrentPackageNode().isPresent());

        assertTrue("construct with package url",
                new AbortedScanException(new NullPointerException(),
                        file.toURI().toURL()).getCurrentPackageUrl().isPresent());
        assertFalse("construct with package url",
                new AbortedScanException(new NullPointerException(),
                        file.toURI().toURL()).getCurrentPackageFile().isPresent());
        assertFalse("construct with package url",
                new AbortedScanException(new NullPointerException(),
                        file.toURI().toURL()).getCurrentPackageNode().isPresent());

        Node node = mock(Node.class);
        when(node.getPath()).thenReturn("/correct/path");

        assertTrue("construct with package node",
                new AbortedScanException(new NullPointerException(),
                        node).getCurrentPackageNode().isPresent());
        assertFalse("construct with package node",
                new AbortedScanException(new NullPointerException(),
                        node).getCurrentPackageFile().isPresent());
        assertFalse("construct with package node",
                new AbortedScanException(new NullPointerException(),
                        node).getCurrentPackageUrl().isPresent());
    }

    @Test
    public void testGetMessage() throws Exception {
        final String baseMessage = "baseMessage";
        final Exception baseError = new Exception(baseMessage);

        final Function<String, String> expectMessage = prefix ->
                prefix + baseError.getClass().getName() + ": " + baseError.getMessage();

        AbortedScanException aseNoPackage = new AbortedScanException(baseError);
        assertEquals("no package has base message", expectMessage.apply(""), aseNoPackage.getMessage());
        File file = TestPackageUtil.prepareTestPackage("package_1.0.zip");
        AbortedScanException aseFile = new AbortedScanException(baseError, file);
        assertEquals("file package package has prefix",
                expectMessage.apply("(Failed package: " + file.getAbsolutePath() + ") "),
                aseFile.getMessage());
        final URL url = file.toURI().toURL();
        AbortedScanException aseUrl = new AbortedScanException(baseError, url);
        assertEquals("url package package has prefix",
                expectMessage.apply("(Failed package: " + url.toString() + ") "),
                aseUrl.getMessage());

        Node node = mock(Node.class);
        final String path = "/etc/packages/correct/path";
        when(node.getPath()).thenReturn(path);
        AbortedScanException aseNode = new AbortedScanException(baseError, node);
        assertEquals("node package package has prefix",
                expectMessage.apply("(Failed package: " + path + ") "),
                aseNode.getMessage());
    }
}
