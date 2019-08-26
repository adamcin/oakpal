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

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.apache.jackrabbit.util.ISO9075;
import org.junit.Test;

public class ChecklistExporterTest {
    final File testBaseDir = new File("target/repos/ChecklistExporterTest");

    @Test(expected = IllegalArgumentException.class)
    public void testSelectorType_byName_unknown() {
        ChecklistExporter.SelectorType.byName("unknown");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForcedRootUpdatePolicy_byName_unknown() {
        ChecklistExporter.ForcedRootUpdatePolicy.byName("unknown");
    }

    @Test
    public void testOpToString() {
        final ChecklistExporter.Op op =
                new ChecklistExporter.Op(ChecklistExporter.SelectorType.NODETYPE, "+nt:base");
        assertEquals("op.toString = ", "NODETYPE: [+nt:base]", op.toString());
    }

    @Test
    public void testPreferDifferent() {
        final String value = "original";
        final BinaryOperator<String> operator = ChecklistExporter.preferDifferent(value);
        assertEquals("left if unequal", "left", operator.apply("left", "right"));
        assertEquals("right if left equal", "right", operator.apply(value, "right"));
    }

    @Test
    public void testAddToLeftCombiner() {
        final Set<String> abc = new HashSet<>(Arrays.asList("a", "b", "c"));
        final Set<String> oneTwoThree = new HashSet<>(Arrays.asList("1", "2", "3"));
        final BinaryOperator<Set<String>> operator = ChecklistExporter.addToLeft();
        final Set<String> result = operator.apply(abc, oneTwoThree);
        assertSame("result is same as left", abc, result);
        final Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "1", "2", "3"));
        assertEquals("result is expected", expected, result);
    }

    @Test
    public void testEnsureNamespaces() throws Exception {
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(Arrays.asList(
                JcrNs.create("", ""),
                JcrNs.create("foo", "http://foo.com"),
                JcrNs.create("bar", "http://bar.com")));

        new OakMachine.Builder()
                .withInitStage(
                        new InitStage.Builder()
                                .withNs("bar", "http://bar.com")
                                .build())
                .build()
                .adminInitAndInspect(session -> {
                    final NamespaceRegistry registry = session.getWorkspace()
                            .getNamespaceRegistry();
                    assertEquals("bar already registered", "bar", registry.getPrefix("http://bar.com"));
                    assertFalse("foo not registered", Arrays.asList(registry.getPrefixes()).contains("foo"));
                    ChecklistExporter.ensureNamespaces(session, mapping);
                    assertTrue("foo registered now", Arrays.asList(registry.getPrefixes()).contains("foo"));
                    assertEquals("bar still registered", "bar", registry.getPrefix("http://bar.com"));
                    assertEquals("foo uri is", "http://foo.com", registry.getURI("foo"));
                });

    }

    @Test
    public void testUpdateChecklist() throws Exception {
        final String pathPrefix = "/test";
        final File tempDir = new File(testBaseDir, "testUpdateChecklist");
        final File pass1Dir = new File(tempDir, "pass1/segmentstore");
        final File pass1Checklist = new File(tempDir, "pass1.chk.json");
        final File fullRepoDir = new File(tempDir, "fullRepo/segmentstore");
        final File diffRepoDir = new File(tempDir, "diffRepo/segmentstore");
        final File fullPassChecklist = new File(tempDir, "fullPass.chk.json");
        final File mergePassChecklist = new File(tempDir, "mergePass.chk.json");
        final File replacePassChecklist = new File(tempDir, "replacePass.chk.json");
        final File truncatePassChecklist = new File(tempDir, "truncatePass.chk.json");

        final int sample = 5;
        List<String> orderedPaths = IntStream.range(0, sample).mapToObj(val -> pathPrefix + "/ordered" + val).collect(Collectors.toList());
        List<String> unorderedPaths = IntStream.range(0, sample).mapToObj(val -> pathPrefix + "/unordered" + val).collect(Collectors.toList());
        assertEquals("should generate n paths: ", sample, orderedPaths.size());

        final List<String> allPaths = new ArrayList<>(orderedPaths);
        allPaths.addAll(unorderedPaths);
        allPaths.add(pathPrefix);

        TestUtil.prepareRepo(pass1Dir, session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));
            orderedPaths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:OrderedFolder", session, true)));
            unorderedPaths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:Folder", session, true)));
        });

        TestUtil.prepareRepo(fullRepoDir, session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));
            allPaths.forEach(Fun.uncheckVoid1(path -> {
                Node node = JcrUtils.getOrCreateByPath(path, "sling:Folder", session);
                node.addMixin("mix:title");
                node.addMixin("sling:Resource");
            }));
            session.save();
        });

        TestUtil.prepareRepo(diffRepoDir, session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));
            allPaths.stream().filter(path -> path.matches(".*[02468]$"))
                    .forEachOrdered(Fun.uncheckVoid1(path -> {
                        Node node = JcrUtils.getOrCreateByPath(path, "sling:Folder", session);
                        node.addMixin("mix:title");
                        node.addMixin("sling:Resource");
                    }));
            session.save();
        });

        TestUtil.withReadOnlyFixture(pass1Dir, session -> {
            ChecklistExporter pathExporter = new ChecklistExporter.Builder().byPath(allPaths.toArray(new String[0])).build();

            pathExporter.updateChecklist(() -> new OutputStreamWriter(
                            new FileOutputStream(pass1Checklist), StandardCharsets.UTF_8),
                    session, Checklist.fromJson("", null, obj()
                            .key(Checklist.KEY_JCR_NAMESPACES, Collections
                                    .singletonList(JcrNs.create("sling",
                                            "http://sling.apache.org/jcr/sling/1.0")))
                            .key(Checklist.KEY_JCR_PRIVILEGES, obj()
                                    .key("sling:doesAll", obj()
                                            .key("contains", arr("sling:doOne", "sling:doTwo")))
                                    .get())
                            .get()), null);

            try (JsonReader reader = Json.createReader(new FileInputStream(pass1Checklist))) {
                JsonObject checklist = reader.readObject();
                assertTrue("checklist object should contain the forcedRoots key",
                        checklist.containsKey(Checklist.KEY_FORCED_ROOTS));
                assertTrue("checklist object should contain the privileges key",
                        checklist.containsKey(Checklist.KEY_JCR_PRIVILEGES));

                JsonArray forcedRoots = checklist.getJsonArray(Checklist.KEY_FORCED_ROOTS);
                assertEquals("forcedRoots should be array with expected number of elements", allPaths.size(),
                        forcedRoots.size());

                List<ForcedRoot> readRoots = JavaxJson.mapArrayOfObjects(forcedRoots, ForcedRoot::fromJson);
                for (String path : unorderedPaths) {
                    ForcedRoot root0 = readRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                    assertNotNull(String.format("[pass1] root for path %s should not be null", path), root0);
                    assertEquals(String.format("[pass1] root primaryType for path %s should be sling:Folder", path),
                            "sling:Folder", root0.getPrimaryType());
                }
                for (String path : orderedPaths) {
                    ForcedRoot root0 = readRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                    assertNotNull(String.format("[pass1] root for path %s should not be null", path), root0);
                    assertEquals(String.format("[pass1] root primaryType for path %s should be sling:OrderedFolder", path),
                            "sling:OrderedFolder", root0.getPrimaryType());
                }
            }
        });


        TestUtil.withReadOnlyFixture(fullRepoDir, session -> {
            ChecklistExporter pathExporter = new ChecklistExporter.Builder().byPath(allPaths.toArray(new String[0])).build();

            try (JsonReader reader = Json.createReader(new FileInputStream(pass1Checklist))) {

                pathExporter.updateChecklist(() -> new OutputStreamWriter(
                                new FileOutputStream(fullPassChecklist), StandardCharsets.UTF_8),
                        session, Checklist.fromJson("", null, reader.readObject()), null);
            }

            try (JsonReader reader = Json.createReader(new FileInputStream(fullPassChecklist))) {
                JsonObject checklist = reader.readObject();
                assertTrue("checklist object should contain the forcedRoots key",
                        checklist.containsKey(Checklist.KEY_FORCED_ROOTS));
                assertTrue("checklist object should contain the privileges key",
                        checklist.containsKey(Checklist.KEY_JCR_PRIVILEGES));

                JsonArray forcedRoots = checklist.getJsonArray(Checklist.KEY_FORCED_ROOTS);
                assertEquals("forcedRoots should be array with expected number of elements", allPaths.size(),
                        forcedRoots.size());

                List<ForcedRoot> readRoots = JavaxJson.mapArrayOfObjects(forcedRoots, ForcedRoot::fromJson);
                for (String path : allPaths) {
                    ForcedRoot root0 = readRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                    assertNotNull(String.format("[pass2] root for path %s should not be null", path), root0);
                    assertEquals(String.format("[pass2] root primaryType for path %s should be sling:Folder", path),
                            "sling:Folder", root0.getPrimaryType());
                    assertTrue(String.format("[pass2] root mixinTypes for path %s should contain mix:title", path),
                            root0.getMixinTypes().contains("mix:title"));
                }
            }
        });

        TestUtil.withReadOnlyFixture(diffRepoDir, session -> {
            ChecklistExporter diffExporter = new ChecklistExporter.Builder()
                    .byNodeType("sling:Folder")
                    .withScopePaths(Rule.fromJsonArray(arr(key("type", "include").key("pattern", pathPrefix + "/ordered.*")).get()))
                    .withNodeTypeFilters(Rule.fromJsonArray(arr(key("type", "exclude").key("pattern", "sling:.*")).get()))
                    .build();

            try (JsonReader reader = Json.createReader(new FileInputStream(fullPassChecklist))) {

                diffExporter.updateChecklist(() -> new OutputStreamWriter(
                                new FileOutputStream(mergePassChecklist), StandardCharsets.UTF_8),
                        session, Checklist.fromJson("", null, reader.readObject()),
                        ChecklistExporter.ForcedRootUpdatePolicy.MERGE);
            }

            try (JsonReader reader = Json.createReader(new FileInputStream(fullPassChecklist))) {

                diffExporter.updateChecklist(() -> new OutputStreamWriter(
                                new FileOutputStream(replacePassChecklist), StandardCharsets.UTF_8),
                        session, Checklist.fromJson("", null, reader.readObject()),
                        ChecklistExporter.ForcedRootUpdatePolicy.REPLACE);
            }

            try (JsonReader reader = Json.createReader(new FileInputStream(fullPassChecklist))) {

                diffExporter.updateChecklist(() -> new OutputStreamWriter(
                                new FileOutputStream(truncatePassChecklist), StandardCharsets.UTF_8),
                        session, Checklist.fromJson("", null, reader.readObject()),
                        ChecklistExporter.ForcedRootUpdatePolicy.TRUNCATE);
            }

            try (JsonReader reader = Json.createReader(new FileInputStream(mergePassChecklist))) {
                JsonObject checklist = reader.readObject();
                assertTrue("checklist object should contain the forcedRoots key",
                        checklist.containsKey(Checklist.KEY_FORCED_ROOTS));

                JsonArray forcedRoots = checklist.getJsonArray(Checklist.KEY_FORCED_ROOTS);
                assertEquals("[mergePass] forcedRoots should be array with expected number of elements", allPaths.size(),
                        forcedRoots.size());

                List<ForcedRoot> readRoots = JavaxJson.mapArrayOfObjects(forcedRoots, ForcedRoot::fromJson);
                for (String path : allPaths) {
                    ForcedRoot root0 = readRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                    if (path.matches(".*/ordered[02468]$")) {
                        assertNotNull(String.format("[mergePass] root for path %s should not be null", path), root0);
                        assertNull(String.format("[mergePass] root primaryType for path %s should be sling:Folder", path),
                                root0.getPrimaryType());
                        assertTrue(String.format("[mergePass] root mixinTypes for path %s should contain mix:title", path),
                                root0.getMixinTypes().contains("mix:title"));
                        assertFalse(String.format("[mergePass] root mixinTypes for path %s should NOT contain sling:Resource", path),
                                root0.getMixinTypes().contains("sling:Resource"));
                    } else {
                        assertNotNull(String.format("[mergePass] root for path %s should not be null", path), root0);
                        assertEquals(String.format("[mergePass] root primaryType for path %s should be sling:Folder", path),
                                "sling:Folder", root0.getPrimaryType());
                        assertTrue(String.format("[mergePass] root mixinTypes for path %s should contain mix:title", path),
                                root0.getMixinTypes().contains("mix:title"));
                        assertTrue(String.format("[mergePass] root mixinTypes for path %s should contain sling:Resource", path),
                                root0.getMixinTypes().contains("sling:Resource"));
                    }
                }
            }

            try (JsonReader reader = Json.createReader(new FileInputStream(replacePassChecklist))) {
                JsonObject checklist = reader.readObject();
                assertTrue("checklist object should contain the forcedRoots key",
                        checklist.containsKey(Checklist.KEY_FORCED_ROOTS));

                JsonArray forcedRoots = checklist.getJsonArray(Checklist.KEY_FORCED_ROOTS);
                assertEquals("[replacePass] forcedRoots should be array with expected number of elements",
                        1 + unorderedPaths.size() + orderedPaths.stream().filter(path -> path.matches(".*[02468]$")).count(),
                        forcedRoots.size());

                List<ForcedRoot> readRoots = JavaxJson.mapArrayOfObjects(forcedRoots, ForcedRoot::fromJson);
                for (String path : allPaths) {
                    ForcedRoot root0 = readRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                    if (path.matches(".*/ordered[02468]$")) {
                        assertNotNull(String.format("[replacePass] root for path %s should not be null", path), root0);
                        assertNull(String.format("[replacePass] root primaryType for path %s should be sling:Folder", path),
                                root0.getPrimaryType());
                        assertTrue(String.format("[replacePass] root mixinTypes for path %s should contain mix:title", path),
                                root0.getMixinTypes().contains("mix:title"));
                        assertFalse(String.format("[replacePass] root mixinTypes for path %s should NOT contain sling:Resource", path),
                                root0.getMixinTypes().contains("sling:Resource"));
                    } else if (path.equals(pathPrefix) || path.matches(".*/unordered.*")) {
                        assertNotNull(String.format("[replacePass] root for path %s should not be null", path), root0);
                        assertEquals(String.format("[replacePass] root primaryType for path %s should be sling:Folder", path),
                                "sling:Folder", root0.getPrimaryType());
                        assertTrue(String.format("[replacePass] root mixinTypes for path %s should contain mix:title", path),
                                root0.getMixinTypes().contains("mix:title"));
                        assertTrue(String.format("[replacePass] root mixinTypes for path %s should contain sling:Resource", path),
                                root0.getMixinTypes().contains("sling:Resource"));
                    } else {
                        assertNull(String.format("[replacePass] root for path %s should be null", path), root0);
                    }
                }
            }

            try (JsonReader reader = Json.createReader(new FileInputStream(truncatePassChecklist))) {
                JsonObject checklist = reader.readObject();
                assertTrue("checklist object should contain the forcedRoots key",
                        checklist.containsKey(Checklist.KEY_FORCED_ROOTS));

                JsonArray forcedRoots = checklist.getJsonArray(Checklist.KEY_FORCED_ROOTS);
                assertEquals("[truncatePass] forcedRoots should be array with expected number of elements",
                        orderedPaths.stream().filter(path -> path.matches(".*[02468]$")).count(), forcedRoots.size());

                List<ForcedRoot> readRoots = JavaxJson.mapArrayOfObjects(forcedRoots, ForcedRoot::fromJson);
                for (String path : allPaths) {
                    ForcedRoot root0 = readRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                    if (path.matches(".*/ordered[02468]$")) {
                        assertNotNull(String.format("[truncatePass] root for path %s should not be null", path), root0);
                        assertNull(String.format("[truncatePass] root primaryType for path %s should be sling:Folder", path),
                                root0.getPrimaryType());
                        assertTrue(String.format("[truncatePass] root mixinTypes for path %s should contain mix:title", path),
                                root0.getMixinTypes().contains("mix:title"));
                        assertFalse(String.format("[truncatePass] root mixinTypes for path %s should NOT contain sling:Resource", path),
                                root0.getMixinTypes().contains("sling:Resource"));
                    } else {
                        assertNull(String.format("[truncatePass] root for path %s should not be null", path), root0);
                    }
                }
            }
        });
    }

    @Test
    public void testFindRoots() throws Exception {
        final String pathPrefix = "/test";
        final File tempDir = new File(testBaseDir, "testFindRoots");
        final File repoDir = new File(tempDir, "repo/segmentstore");

        final int sample = 5;
        List<String> orderedPaths = IntStream.range(0, sample).mapToObj(val -> pathPrefix + "/ordered" + val).collect(Collectors.toList());
        List<String> unorderedPaths = IntStream.range(0, sample).mapToObj(val -> pathPrefix + "/unordered" + val).collect(Collectors.toList());
        assertEquals("should generate n paths: ", sample, orderedPaths.size());

        TestUtil.prepareRepo(repoDir, session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));
            orderedPaths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:OrderedFolder", session, true)));
            unorderedPaths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:Folder", session, true)));
        });

        List<String> allPaths = new ArrayList<>(orderedPaths);
        allPaths.addAll(unorderedPaths);
        allPaths.add(pathPrefix);

        TestUtil.withReadOnlyFixture(repoDir, session -> {
            ChecklistExporter pathExporter = new ChecklistExporter.Builder().byPath(allPaths.toArray(new String[0])).build();
            List<ForcedRoot> byPath = pathExporter.findRoots(session);

            for (String path : unorderedPaths) {
                ForcedRoot root0 = byPath.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[path] root for path %s should not be null", path), root0);
                assertEquals(String.format("[path] root primaryType for path %s should be sling:Folder", path),
                        "sling:Folder", root0.getPrimaryType());
            }
            for (String path : orderedPaths) {
                ForcedRoot root0 = byPath.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[path] root for path %s should not be null", path), root0);
                assertEquals(String.format("[path] root primaryType for path %s should be sling:OrderedFolder", path),
                        "sling:OrderedFolder", root0.getPrimaryType());
            }

            final String queryPrefix = ISO9075.encodePath("/jcr:root" + pathPrefix);
            ChecklistExporter queryExporter = new ChecklistExporter.Builder().byQuery(queryPrefix + "//element(*, sling:Folder)").build();
            List<ForcedRoot> byQuery = queryExporter.findRoots(session);
            for (String path : unorderedPaths) {
                ForcedRoot root0 = byQuery.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[query] root for path %s should not be null", path), root0);
                assertEquals(String.format("[query] root primaryType for path %s should be sling:Folder", path),
                        "sling:Folder", root0.getPrimaryType());
            }
            for (String path : orderedPaths) {
                ForcedRoot root0 = byQuery.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[query] root for path %s should not be null", path), root0);
                assertEquals(String.format("[query] root primaryType for path %s should be sling:OrderedFolder", path),
                        "sling:OrderedFolder", root0.getPrimaryType());
            }

            ChecklistExporter ntExporter = new ChecklistExporter.Builder().byNodeType("nt:folder", "+sling:Folder").build();
            List<ForcedRoot> byNt = ntExporter.findRoots(session);
            for (String path : unorderedPaths) {
                ForcedRoot root0 = byNt.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[nodetype] root for path %s should not be null", path), root0);
                assertEquals(String.format("[nodetype] root primaryType for path %s should be sling:Folder", path),
                        "sling:Folder", root0.getPrimaryType());
            }
            for (String path : orderedPaths) {
                ForcedRoot root0 = byNt.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[nodetype] root for path %s should not be null", path), root0);
                assertEquals(String.format("[nodetype] root primaryType for path %s should be sling:OrderedFolder", path),
                        "sling:OrderedFolder", root0.getPrimaryType());
            }
            for (String path : Collections.singletonList(pathPrefix)) {
                ForcedRoot root0 = byNt.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("[nodetype] root for path %s should not be null", path), root0);
                assertEquals(String.format("[nodetype] root primaryType for path %s should be nt:folder", path),
                        "nt:folder", root0.getPrimaryType());
            }

        });
    }

    @Test
    public void testNodeToRootSimple() throws Exception {
        TestUtil.withInMemoryRepo(session -> {
            final NamespaceMapping mapping = new NamespaceMapping(new SessionNamespaceResolver(session));
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));

            ChecklistExporter exporter = new ChecklistExporter.Builder().build();

            Node node1 = JcrUtils.getOrCreateByPath("/test/node1", "nt:folder", "sling:Folder", session, true);
            ForcedRoot root1 = exporter.nodeToRoot(node1, mapping).orElse(null);

            assertNotNull("root1 should be present", root1);
            assertEquals("root1 primary type should be sling:Folder", "sling:Folder", root1.getPrimaryType());
            assertTrue("root1 mixin types should be empty", root1.getMixinTypes().isEmpty());

            Node node2 = JcrUtils.getOrCreateByPath("/test/node2", "nt:folder", "sling:OrderedFolder", session, true);
            node2.addMixin(String.format("{%s}title", NamespaceHelper.MIX));
            ForcedRoot root2 = exporter.nodeToRoot(node2, mapping).orElse(null);
            assertNotNull("root2 should be present", root2);
            assertEquals("root2 primary type should be sling:OrderedFolder", "sling:OrderedFolder", root2.getPrimaryType());
            assertTrue("root2 mixin types should contain mix:title", root2.getMixinTypes().contains("mix:title"));
        });
    }

    @Test
    public void testNodeToRootPathScoped() throws Exception {
        TestUtil.withInMemoryRepo(session -> {
            final NamespaceMapping mapping = new NamespaceMapping(new SessionNamespaceResolver(session));
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));

            Node node1 = JcrUtils.getOrCreateByPath("/test_include/node1", "nt:folder", session);
            Node node2 = JcrUtils.getOrCreateByPath("/test_exclude/node2", "nt:folder", session);

            ChecklistExporter exporter = new ChecklistExporter.Builder()
                    .withScopePaths(Rule
                            .fromJsonArray(arr(key("type", "include").key("pattern", "/test_include(/.*)?"))
                                    .get())).build();

            ForcedRoot root1 = exporter.nodeToRoot(node1, mapping).orElse(null);

            assertNotNull("root1 should not be null", root1);

            ForcedRoot root2 = exporter.nodeToRoot(node2, mapping).orElse(null);

            assertNull("root2 should be null", root2);
        });
    }

    @Test
    public void testNodeToRootNodeTypeScoped() throws Exception {
        TestUtil.withInMemoryRepo(session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));
            final NamespaceMapping mapping = new NamespaceMapping(new SessionNamespaceResolver(session));

            Node node1 = JcrUtils.getOrCreateByPath("/test/node1", "nt:folder", session);
            node1.addMixin("mix:title");
            node1.addMixin("sling:Resource");
            node1.addMixin("sling:ResourceSuperType");

            ChecklistExporter mixExporter = new ChecklistExporter.Builder().withNodeTypeFilters(
                    Rule.fromJsonArray(arr(key("type", "include").key("pattern", "mix:.*"))
                            .get())).build();

            ChecklistExporter slingExporter = new ChecklistExporter.Builder().withNodeTypeFilters(
                    Rule.fromJsonArray(arr(key("type", "include").key("pattern", "sling:.*"))
                            .get())).build();

            ChecklistExporter rtExporter = new ChecklistExporter.Builder().withNodeTypeFilters(
                    Rule.fromJsonArray(arr(key("type", "include").key("pattern", "sling:Resource"))
                            .get())).build();

            ForcedRoot mixRoot = mixExporter.nodeToRoot(node1, mapping).orElse(null);
            assertNotNull("mixRoot should not be null", mixRoot);
            assertTrue("mixRoot should contain mix:title mixin", mixRoot.getMixinTypes().contains("mix:title"));
            assertFalse("mixRoot should not contain sling:Resource mixin", mixRoot.getMixinTypes().contains("sling:Resource"));
            assertFalse("mixRoot should not contain sling:ResourceSuperType mixin", mixRoot.getMixinTypes().contains("sling:ResourceSuperType"));

            ForcedRoot slingRoot = slingExporter.nodeToRoot(node1, mapping).orElse(null);
            assertNotNull("slingRoot should not be null", slingRoot);
            assertFalse("slingRoot should not contain mix:title mixin", slingRoot.getMixinTypes().contains("mix:title"));
            assertTrue("slingRoot should contain sling:Resource mixin", slingRoot.getMixinTypes().contains("sling:Resource"));
            assertTrue("slingRoot should contain sling:ResourceSuperType mixin", slingRoot.getMixinTypes().contains("sling:ResourceSuperType"));

            ForcedRoot rtRoot = rtExporter.nodeToRoot(node1, mapping).orElse(null);
            assertNotNull("rtRoot should not be null", rtRoot);
            assertNull("rtRoot primaryType should be null", rtRoot.getPrimaryType());
            assertFalse("rtRoot should not contain mix:title mixin", rtRoot.getMixinTypes().contains("mix:title"));
            assertTrue("rtRoot should contain sling:Resource mixin", rtRoot.getMixinTypes().contains("sling:Resource"));
            assertFalse("rtRoot should not contain sling:ResourceSuperType mixin", rtRoot.getMixinTypes().contains("sling:ResourceSuperType"));
        });
    }

    @Test
    public void testTraverse() throws Exception {
        TestUtil.withInMemoryRepo(session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));

            for (int i = 0; i < 5; i++) {
                JcrUtils.getOrCreateByPath("/test/node" + i, "nt:folder", session);
            }

            final ChecklistExporter exporter = new ChecklistExporter.Builder().build();

            List<ForcedRoot> roots = exporter.traverse(session,
                    Arrays.asList("/test/node0", "/test/node2", "/test/node4", "/test/node6"));

            ForcedRoot root0 = roots.stream().filter(root -> "/test/node0".equals(root.getPath())).findFirst().orElse(null);
            assertNotNull("root0 should not be null", root0);
            assertEquals("root0 primaryType should be nt:folder", "nt:folder", root0.getPrimaryType());
            ForcedRoot root1 = roots.stream().filter(root -> "/test/node1".equals(root.getPath())).findFirst().orElse(null);
            assertNull("root1 should be null", root1);
            ForcedRoot root2 = roots.stream().filter(root -> "/test/node2".equals(root.getPath())).findFirst().orElse(null);
            assertNotNull("root2 should not be null", root2);
            assertEquals("root2 primaryType should be nt:folder", "nt:folder", root2.getPrimaryType());
            ForcedRoot root6 = roots.stream().filter(root -> "/test/node6".equals(root.getPath())).findFirst().orElse(null);
            assertNull("root6 should be null", root6);
        });
    }

    @Test
    public void testQuery() throws Exception {
        TestUtil.withInMemoryRepo(session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));

            final int sample = 5;
            List<String> paths = IntStream.range(0, sample).mapToObj(val -> "/test/node" + val).collect(Collectors.toList());
            assertEquals("should generate n paths: ", sample, paths.size());
            paths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:Folder", session, true)));

            final ChecklistExporter exporter = new ChecklistExporter.Builder().build();

            List<ForcedRoot> byXPath = exporter.query(session, "//element(*, sling:Folder)");

            for (String path : paths) {
                ForcedRoot root0 = byXPath.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("root for path %s should not be null", path), root0);
                assertEquals(String.format("root primaryType for path %s should be sling:Folder", path),
                        "sling:Folder", root0.getPrimaryType());
            }

            List<ForcedRoot> bySQL2 = exporter.query(session, "select * from [sling:Folder]");
            for (String path : paths) {
                ForcedRoot root0 = bySQL2.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("root for path %s should not be null", path), root0);
                assertEquals(String.format("root primaryType for path %s should be sling:Folder", path),
                        "sling:Folder", root0.getPrimaryType());
            }
        });
    }

    @Test
    public void testNtStatement() throws Exception {
        TestUtil.withInMemoryRepo(session -> {
            TestUtil.installCndFromURL(session, getClass().getResource("/sling_nodetypes.cnd"));
            final int sample = 5;
            List<String> orderedPaths = IntStream.range(0, sample).mapToObj(val -> "/test/ordered" + val).collect(Collectors.toList());
            List<String> unorderedPaths = IntStream.range(0, sample).mapToObj(val -> "/test/unordered" + val).collect(Collectors.toList());
            assertEquals("should generate n paths: ", sample, orderedPaths.size());
            orderedPaths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:OrderedFolder", session, true)));
            unorderedPaths.forEach(Fun.tryOrVoid1(path ->
                    JcrUtils.getOrCreateByPath(path, "nt:folder",
                            "sling:Folder", session, true)));

            final ChecklistExporter exporter = new ChecklistExporter.Builder().build();

            final String statement = exporter.ntStatement(session, Arrays.asList("nt:folder", "+sling:Folder"));
            assertTrue("statement should contain 'FROM [sling:Folder]'", statement.contains("FROM [sling:Folder]"));
            assertTrue("statement should contain ' UNION '", statement.contains(" UNION "));
            assertTrue("statement should contain 'FROM [nt:base]'", statement.contains("FROM [nt:base]"));
            assertTrue("statement should contain '[jcr:primaryType] = 'nt:folder'", statement.contains("[jcr:primaryType] = 'nt:folder'"));

            List<ForcedRoot> bySQL2 = exporter.query(session, statement);
            for (String path : unorderedPaths) {
                ForcedRoot root0 = bySQL2.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("root for path %s should not be null", path), root0);
                assertEquals(String.format("root primaryType for path %s should be sling:Folder", path),
                        "sling:Folder", root0.getPrimaryType());
            }
            for (String path : orderedPaths) {
                ForcedRoot root0 = bySQL2.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("root for path %s should not be null", path), root0);
                assertEquals(String.format("root primaryType for path %s should be sling:OrderedFolder", path),
                        "sling:OrderedFolder", root0.getPrimaryType());
            }

            for (String path : Collections.singletonList("/test")) {
                ForcedRoot root0 = bySQL2.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("root for path %s should not be null", path), root0);
                assertEquals(String.format("root primaryType for path %s should be nt:folder", path),
                        "nt:folder", root0.getPrimaryType());
            }

            final String simpler = exporter.ntStatement(session, Collections.singletonList("sling:OrderedFolder"));
            List<ForcedRoot> orderedRoots = exporter.query(session, simpler);
            for (String path : orderedPaths) {
                ForcedRoot root0 = orderedRoots.stream().filter(root -> path.equals(root.getPath())).findFirst().orElse(null);
                assertNotNull(String.format("root for path %s should not be null", path), root0);
                assertEquals(String.format("root primaryType for path %s should be sling:OrderedFolder", path),
                        "sling:OrderedFolder", root0.getPrimaryType());
            }

        });
    }

}
