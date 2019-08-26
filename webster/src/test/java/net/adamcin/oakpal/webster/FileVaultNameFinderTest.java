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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.fs.io.FileArchive;
import org.apache.jackrabbit.vault.fs.io.ZipArchive;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.NamespaceRegistry;

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

    private static FileVaultNameFinder finder() {
        return new FileVaultNameFinder();
    }

    private static FileVaultNameFinder.Handler handler() {
        return finder().new Handler();
    }

    @Test(expected = SAXException.class)
    public void testHandler_setMapping_nullPrefix() throws Exception {
        handler().setMapping(null, "http://foo.com");
    }

    @Test(expected = SAXException.class)
    public void testHandler_startPrefixMapping_nullUri() throws Exception {
        handler().setMapping("nt", null);
    }

    @Test
    public void testHandler_startPrefixMapping_remapOld() throws Exception {
        final FileVaultNameFinder.Handler handler = handler();
        // push nt
        handler.startPrefixMapping("nt", NamespaceRegistry.NAMESPACE_NT);
        handler.expectStartElement = false;
        assertEquals("expect mapping", "nt",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
        // push jcr (shadow nt)
        handler.startPrefixMapping("jcr", NamespaceRegistry.NAMESPACE_NT);
        handler.expectStartElement = false;
        assertEquals("expect mapping", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
        // push jcr
        handler.startPrefixMapping("jcr", NamespaceRegistry.NAMESPACE_JCR);
        handler.expectStartElement = false;
        assertEquals("expect mapping", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_JCR));
        assertEquals("expect jcr prefix for nt uri", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
        assertEquals("expect nt uri for nt prefix", NamespaceRegistry.NAMESPACE_NT,
                handler.getURI("nt"));
        // push nt1 (shadow nt)
        handler.startPrefixMapping("nt1", NamespaceRegistry.NAMESPACE_NT);
        handler.expectStartElement = false;
        assertEquals("expect mapping", "nt1",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));

        // push nt2 (shadow nt1)
        handler.startPrefixMapping("nt2", NamespaceRegistry.NAMESPACE_NT);
        handler.expectStartElement = false;
        assertEquals("expect mapping", "nt2",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
        assertEquals("expect jcr mapping", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_JCR));

        // pop nt2 (reveal nt1) -> active: nt1, jcr
        handler.endPrefixMapping("nt2");
        handler.expectEndPrefixMapping = true;
        assertEquals("expect mapping after end nt2", "nt1",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
        assertEquals("expect jcr mapping", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_JCR));

        // pop nt1 (reveal
        handler.endPrefixMapping("nt1");
        handler.expectEndPrefixMapping = true;
        assertEquals("expect nt uri for nt prefix", NamespaceRegistry.NAMESPACE_NT,
                handler.getURI("nt"));
        assertEquals("expect jcr prefix for nt uri", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
        assertEquals("expect jcr mapping", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_JCR));
        // pop jcr
        handler.endPrefixMapping("jcr");
        handler.expectEndPrefixMapping = true;
        assertEquals("expect nt uri for nt prefix", NamespaceRegistry.NAMESPACE_NT,
                handler.getURI("nt"));
        assertEquals("expect nt uri for jcr prefix", NamespaceRegistry.NAMESPACE_NT,
                handler.getURI("jcr"));
        assertEquals("expect jcr prefix for nt uri", "jcr",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));

        handler.endPrefixMapping("jcr");
        handler.expectEndPrefixMapping = true;
        assertFalse("expect no registered jcr prefix", handler.mapping.hasPrefix("jcr"));
        assertEquals("expect nt uri for nt prefix", NamespaceRegistry.NAMESPACE_NT,
                handler.getURI("nt"));
        assertEquals("expect nt prefix for nt uri", "nt",
                handler.getPrefix(NamespaceRegistry.NAMESPACE_NT));
    }

    @Test(expected = IllegalStateException.class)
    public void testHandler_endPrefixMapping_illegalState() throws Exception {
        FileVaultNameFinder.Handler handler = handler();
        handler.expectEndPrefixMapping = true;
        handler.endPrefixMapping("foo");
    }

    @Test(expected = SAXException.class)
    public void testHandler_createNode() throws Exception {
        final AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(NamespaceRegistry.NAMESPACE_JCR, "primaryType", "jcr:primaryType", "CDATA", "nt:folder");
        handler().createNode("name", "label", attributes);
    }

    @Test
    public void testHandler_startElement_notDocViewReturnsSuccessfully() throws Exception {
        FileVaultNameFinder.Handler handler = handler();
        handler.isDocView = false;
        handler.startElement(null, null, null, null);
    }

    @Test
    public void testHandler_nameFromLabel() {
        assertEquals("foo name is", "foo", handler().nameFromLabel("foo"));
        assertEquals("'' name is", "", handler().nameFromLabel(""));
        assertEquals("foo[] name is", "foo", handler().nameFromLabel("foo[]"));
        assertEquals("foo[0] name is", "foo", handler().nameFromLabel("foo[0]"));
        assertEquals("foo[anystringofchars] name is", "foo", handler().nameFromLabel("foo[anystringofchars]"));
    }
}
