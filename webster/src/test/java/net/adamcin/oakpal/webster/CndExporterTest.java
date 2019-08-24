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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.Result;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.junit.Test;

public class CndExporterTest {
    final File testBaseDir = new File("target/repos/CndExporterTest");

    @Test
    public void testWriteNodetypesWithBuiltins() throws Exception {
        final File tempDir = new File(testBaseDir, "testWriteNodetypesWithBuiltins");
        final File fromRepoDir = new File(tempDir, "fromRepo/segmentstore");
        final File explicitTypesCnd = new File(tempDir, "explicitTypes.cnd");
        final File allTypesCnd = new File(tempDir, "allTypes.cnd");

        TestUtil.prepareRepo(fromRepoDir, session -> {
            final URL slingNodetypes = getClass().getResource("/sling_nodetypes.cnd");
            TestUtil.installCndFromURL(session, slingNodetypes);
        });

        if (explicitTypesCnd.exists()) {
            explicitTypesCnd.delete();
        }

        if (allTypesCnd.exists()) {
            allTypesCnd.delete();
        }

        TestUtil.withReadOnlyFixture(fromRepoDir, session -> {
            new CndExporter.Builder().withIncludeBuiltins(true).build()
                    .writeNodetypes(explicitTypesCnd, session, Arrays.asList("sling:Folder", "nt:folder"));

            try (Scanner scanner = new Scanner(explicitTypesCnd, StandardCharsets.UTF_8.name())) {
                assertNull("nt:query should not be defined", scanner.findWithinHorizon("\\[nt:query]", 0));
            }
            try (Scanner scanner = new Scanner(explicitTypesCnd, StandardCharsets.UTF_8.name())) {
                assertNull("sling:OrderedFolder should not be defined", scanner.findWithinHorizon("\\[sling:OrderedFolder]", 0));
            }
            try (Scanner scanner = new Scanner(explicitTypesCnd, StandardCharsets.UTF_8.name())) {
                assertNotNull("nt:folder should be defined", scanner.findWithinHorizon("\\[nt:folder]", 0));
            }
            try (Scanner scanner = new Scanner(explicitTypesCnd, StandardCharsets.UTF_8.name())) {
                assertNotNull("sling:Folder should be defined", scanner.findWithinHorizon("\\[sling:Folder]", 0));
            }
        });
    }

    @Test
    public void testWriteNodetypes() throws Exception {
        final File tempDir = new File(testBaseDir, "testWriteNodetypes");
        final File fromRepoDir = new File(tempDir, "fromRepo/segmentstore");
        final File toRepoDir = new File(tempDir, "toRepo/segmentstore");
        final File exportedCnd = new File(tempDir, "exported.cnd");

        TestUtil.prepareRepo(fromRepoDir, session -> {
            final URL slingNodetypes = getClass().getResource("/sling_nodetypes.cnd");
            TestUtil.installCndFromURL(session, slingNodetypes);
        });

        if (exportedCnd.exists()) {
            exportedCnd.delete();
        }

        TestUtil.withReadOnlyFixture(fromRepoDir, session -> {
            final Workspace workspace = session.getWorkspace();
            NodeTypeManager ntManager = workspace.getNodeTypeManager();

            assertTrue("sling:Folder should be imported",
                    ntManager.hasNodeType("sling:Folder"));
            assertTrue("sling:OrderedFolder should be imported",
                    ntManager.hasNodeType("sling:OrderedFolder"));

            new CndExporter.Builder().build()
                    .writeNodetypes(exportedCnd, session, Arrays.asList("sling:Folder", "sling:OrderedFolder"));
        });

        TestUtil.prepareRepo(toRepoDir, session -> {
            final URL exportedUrl = exportedCnd.toURL();
            TestUtil.installCndFromURL(session, exportedUrl);
        });

        TestUtil.withReadOnlyFixture(toRepoDir, session -> {
            final Workspace workspace = session.getWorkspace();
            NodeTypeManager ntManager = workspace.getNodeTypeManager();

            assertTrue("sling:Folder should be imported",
                    ntManager.hasNodeType("sling:Folder"));
            assertTrue("sling:OrderedFolder should be imported",
                    ntManager.hasNodeType("sling:OrderedFolder"));
        });
    }

    @Test
    public void testWriteNodetypesByName() throws Exception {
        final File tempDir = new File(testBaseDir, "testWriteNodetypesByName");
        final File fromRepoDir = new File(tempDir, "fromRepo/segmentstore");
        final File toRepoDir = new File(tempDir, "toRepo/segmentstore");
        final File exportedCnd = new File(tempDir, "exported.cnd");

        TestUtil.prepareRepo(fromRepoDir, session -> {
            final URL slingNodetypes = getClass().getResource("/sling_nodetypes.cnd");
            TestUtil.installCndFromURL(session, slingNodetypes);
        });

        if (exportedCnd.exists()) {
            exportedCnd.delete();
        }

        TestUtil.withReadOnlyFixture(fromRepoDir, session -> {
            final Workspace workspace = session.getWorkspace();
            NodeTypeManager ntManager = workspace.getNodeTypeManager();

            assertTrue("sling:Folder should be imported",
                    ntManager.hasNodeType("sling:Folder"));
            assertTrue("sling:OrderedFolder should be imported",
                    ntManager.hasNodeType("sling:OrderedFolder"));

            new CndExporter.Builder().build()
                    .writeNodetypes(exportedCnd, session, Collections.singletonList("sling:Folder"));
        });

        TestUtil.prepareRepo(toRepoDir, session -> {
            final URL exportedUrl = exportedCnd.toURL();
            TestUtil.installCndFromURL(session, exportedUrl);
        });

        TestUtil.withReadOnlyFixture(toRepoDir, session -> {
            final Workspace workspace = session.getWorkspace();
            NodeTypeManager ntManager = workspace.getNodeTypeManager();

            assertTrue("sling:Folder should be imported",
                    ntManager.hasNodeType("sling:Folder"));
            assertFalse("sling:OrderedFolder should NOT be imported",
                    ntManager.hasNodeType("sling:OrderedFolder"));
        });
    }

    static final class NoStringConstructorException extends Exception {
        public NoStringConstructorException() {
            super("NoStringConstructorException");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCombineCauseMessages_reflectiveError() throws NoStringConstructorException {
        CndExporter.combineCauseMessages(
                Stream.of(
                        Result.failure(new Exception("one")),
                        Result.failure(new Exception("two"))),
                Exception.class,
                NoStringConstructorException.class);
    }

    @Test
    public void testNsRemapName() {
        final NamespaceMapping from = JsonCnd
                .toNamespaceMapping(Collections
                        .singletonList(JcrNs.create("fooFrom", "http://foo.com")));
        final NamespaceMapping to = JsonCnd
                .toNamespaceMapping(Collections
                        .singletonList(JcrNs.create("fooTo", "http://foo.com")));

        final Function<String, String> remapper = ChecklistExporter.nsRemapName(from, to);
        assertEquals("expect new prefix", "fooTo:mixin", remapper.apply("fooFrom:mixin"));
    }
}
