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

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.adamcin.oakpal.api.JavaxJson;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.junit.Before;
import org.junit.Test;

public class ChecklistTest {

    private URL manifestUrl;
    private URL cndAUrl;
    private URL cndBUrl;

    @Before
    public void setUp() throws Exception {
        File manifestFile = new File("src/test/resources/test_module/META-INF/MANIFEST.MF");
        manifestUrl = manifestFile.toURI().toURL();
        File cndAFile = new File("src/test/resources/test_module/a.cnd");
        cndAUrl = cndAFile.toURI().toURL();
        File cndBFile = new File("src/test/resources/test_module/b.cnd");
        cndBUrl = cndBFile.toURI().toURL();
    }

    @Test
    public void testFromJSON() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/simpleChecklist.json");
             JsonReader reader = Json.createReader(is)) {
            final JsonObject json = reader.readObject();
            Checklist checklist = Checklist.fromJson("core-tests", manifestUrl, json);

            assertNotNull("checklist should not be null", checklist);
            assertEquals("checklist moduleName", "core-tests", checklist.getModuleName());
            assertEquals("checklist name", "simpleChecklist", checklist.getName());
            assertEquals("checklist jcr prefix", "lapkao", checklist.getJcrNamespaces().get(0).getPrefix());
            assertEquals("checklist json is identical to original", json, checklist.toJson());
        }
    }

    @Test
    public void testChecklistKeyComparator() {
        final Checklist.JsonKeys keys = Checklist.keys();
        final String[] inputValues1 = keys.orderedKeys().toArray(new String[0]);
        Arrays.sort(inputValues1, Checklist.checklistKeyComparator.reversed());
        Arrays.sort(inputValues1, Checklist.checklistKeyComparator);
        assertArrayEquals("sort reverse sort should be stable",
                keys.orderedKeys().toArray(new String[0]), inputValues1);

        final String[] inputValues2 = new String[]{"foo", keys.name()};
        final String[] expectValues2 = new String[]{keys.name(), "foo"};
        Arrays.sort(inputValues2, Checklist.checklistKeyComparator);
        assertArrayEquals("sort unknown keys after known keys",
                expectValues2, inputValues2);

        final String[] inputValues2b = new String[]{keys.name(), "foo"};
        final String[] expectValues2b = new String[]{keys.name(), "foo"};
        Arrays.sort(inputValues2b, Checklist.checklistKeyComparator);
        assertArrayEquals("sort unknown keys after known keys",
                expectValues2b, inputValues2b);

        final String[] inputValues3 = new String[]{"b", "a"};
        final String[] expectValues3 = new String[]{"a", "b"};
        Arrays.sort(inputValues3, Checklist.checklistKeyComparator);
        assertArrayEquals("sort unknown keys using String.compareTo",
                expectValues3, inputValues3);
    }

    @Test
    public void testComparingJsonKeys() {
        final Checklist.JsonKeys keys = Checklist.keys();
        final JsonObject obj1 = key("id", keys.checks()).get();
        final JsonObject obj2 = key("id", keys.cndNames()).get();
        final JsonObject obj3 = key("id", keys.cndUrls()).get();
        final JsonObject[] inputValues1 = new JsonObject[]{obj2, obj3, obj1};
        final JsonObject[] expectValues1 = new JsonObject[]{obj1, obj2, obj3};
        Arrays.sort(inputValues1, Checklist.comparingJsonKeys(obj -> obj.getString("id")));
        assertArrayEquals("sort keys using json extractor",
                expectValues1, inputValues1);
    }

    @Test
    public void testBuilderNoName() {
        Checklist.Builder builderNoName = new Checklist.Builder("test");
        assertEquals("module name", "test", builderNoName.build().getModuleName());
        assertEquals("module as name", "test", builderNoName.build().getName());
    }

    @Test
    public void testBuilderWithName() {
        Checklist.Builder builderWithName = new Checklist.Builder("test");
        builderWithName.withName("foo");
        assertEquals("module name", "test", builderWithName.build().getModuleName());
        assertEquals("name pass through", "foo", builderWithName.build().getName());
    }

    @Test
    public void testBuilderWithCndUrls() throws Exception {
        Checklist.Builder builderWithCndUrls = new Checklist.Builder("test");
        final URL cndA = new URL("file:/some/file/a.cnd");
        final URL cndB = new URL("file:/some/file/b.cnd");
        builderWithCndUrls.withCndUrls(Arrays.asList(cndA, cndB));
        assertEquals("cnd urls pass thru",
                Arrays.asList(cndA, cndB), builderWithCndUrls.build().getCndUrls());
    }

    @Test
    public void testBuilderWithNamespaces() {
        final List<JcrNs> jcrNamespaces = Arrays.asList(
                JcrNs.create("foo", "http://foo.com"),
                JcrNs.create("bar", "http://bar.com"));
        Checklist.Builder builderWithJcrNamespaces = new Checklist.Builder("test");
        builderWithJcrNamespaces.withJcrNamespaces(jcrNamespaces);
        assertEquals("jcr namespaces pass thru",
                jcrNamespaces, builderWithJcrNamespaces.build().getJcrNamespaces());
    }

    @Test
    public void testBuilderWithNodetypes() {
        final List<JcrNs> jcrNamespaces = Arrays.asList(
                JcrNs.create("foo", "http://foo.com"),
                JcrNs.create("bar", "http://bar.com"));
        Checklist.Builder builderWithNodetypes = new Checklist.Builder("test");
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNamespaces);
        final List<QNodeTypeDefinition> nodetypes = JsonCnd.getQTypesFromJson(obj()
                .key("foo:primaryType").val(key("extends", arr().val("nt:base")))
                .key("foo:mixinType").val(key("@", arr().val("mixin")))
                .key("bar:primaryType").val(key("extends", arr().val("nt:base")))
                .key("bar:mixinType").val(key("@", arr().val("mixin"))).get(), mapping);
        builderWithNodetypes.withJcrNodetypes(nodetypes);
        assertEquals("jcr nodetypes pass thru",
                nodetypes, builderWithNodetypes.build().getJcrNodetypes());
    }

    @Test
    public void testBuilderWithPrivileges() {
        final List<JcrNs> jcrNamespaces = Arrays.asList(
                JcrNs.create("foo", "http://foo.com"),
                JcrNs.create("bar", "http://bar.com"));
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNamespaces);
        Checklist.Builder builderWithPrivileges = new Checklist.Builder("test");
        builderWithPrivileges.withJcrNamespaces(jcrNamespaces);
        final List<String> privilegeNames = Arrays.asList("foo:canDo", "bar:canBe");
        final List<PrivilegeDefinition> privileges =
                JsonCnd.getPrivilegesFromJson(JavaxJson.wrap(privilegeNames), mapping);
        builderWithPrivileges.withJcrPrivileges(privileges);
        assertEquals("jcr privilege names pass thru",
                privilegeNames, builderWithPrivileges.build().getJcrPrivilegeNames());
        assertEquals("jcr privileges pass thru",
                privileges, builderWithPrivileges.build().getJcrPrivileges());
    }

    @Test
    public void testBuilderWithForcedRoots() {
        Checklist.Builder builderWithForcedRoots = new Checklist.Builder("test");
        final List<ForcedRoot> forcedRoots = Arrays.asList(
                ForcedRoot.fromJson(obj()
                        .key(ForcedRoot.keys().path(), "/test/foo")
                        .key(ForcedRoot.keys().primaryType(), "foo:primaryType")
                        .get()),
                ForcedRoot.fromJson(obj()
                        .key(ForcedRoot.keys().path(), "/test/bar")
                        .key(ForcedRoot.keys().primaryType(), "bar:primaryType")
                        .get()));
        builderWithForcedRoots.withForcedRoots(forcedRoots);
        assertEquals("forced roots pass thru",
                forcedRoots, builderWithForcedRoots.build().getForcedRoots());
    }

    @Test
    public void testBuilderIsValidCheckSpec() {
        Checklist.Builder builder = new Checklist.Builder("test");
        CheckSpec validSpec = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid")
                .key(CheckSpec.keys().impl(), "impl").get());
        assertTrue("valid spec isValid: " + validSpec.getName(),
                builder.isValidCheckspec(validSpec));
        CheckSpec abstractSpec = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid").get());
        assertFalse("abstract spec not isValid: " + abstractSpec.getName(),
                builder.isValidCheckspec(abstractSpec));
        CheckSpec nullNameSpec = CheckSpec.fromJson(key(CheckSpec.keys().name(), null)
                .key(CheckSpec.keys().impl(), "impl").get());
        assertFalse("nullName spec not isValid: " + nullNameSpec.getName(),
                builder.isValidCheckspec(nullNameSpec));
        CheckSpec emptyNameSpec = CheckSpec.fromJson(key(CheckSpec.keys().name(), "")
                .key(CheckSpec.keys().impl(), "impl").get());
        assertFalse("emptyName spec not isValid: " + emptyNameSpec.getName(),
                builder.isValidCheckspec(emptyNameSpec));
        CheckSpec slashNameSpec = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid/notValid")
                .key(CheckSpec.keys().impl(), "impl").get());
        assertFalse("slashName spec not isValid: " + slashNameSpec.getName(),
                builder.isValidCheckspec(slashNameSpec));
    }

    @Test
    public void testBuilderWithChecks() {
        final Checklist.Builder builder = new Checklist.Builder("test");
        CheckSpec validSpec1 = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid1")
                .key(CheckSpec.keys().impl(), "impl").get());
        CheckSpec validSpec2 = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid2")
                .key(CheckSpec.keys().impl(), "impl").get());
        CheckSpec slashNameSpec = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid/notValid")
                .key(CheckSpec.keys().impl(), "impl").get());
        assertFalse("slashName spec not isValid: " + slashNameSpec.getName(),
                builder.isValidCheckspec(slashNameSpec));
        final String prefix = "test/";
        final List<CheckSpec> validSpecs = Stream.of(validSpec1, validSpec2).map(spec -> {
            final CheckSpec copy = CheckSpec.copyOf(spec);
            copy.setName(prefix + spec.getName());
            return copy;
        }).collect(Collectors.toList());
        final List<CheckSpec> inputSpecs = Arrays.asList(validSpec1, slashNameSpec, validSpec2);

        builder.withChecks(inputSpecs);
        assertEquals("expect only valid specs: " + builder.build().getChecks(),
                validSpecs, builder.build().getChecks());
    }

    @Test
    public void testAsInitStage() throws Exception {
        Checklist.Builder builder = new Checklist.Builder("test");
        final URL cndA = cndAUrl;
        final URL cndB = cndBUrl;
        builder.withCndUrls(Arrays.asList(cndA, cndB));
        final String NS_PREFIX_FOO = "foo";
        final String NS_URI_FOO = "http://foo.com";
        final String NS_PREFIX_BAR = "bar";
        final String NS_URI_BAR = "http://bar.com";
        final List<JcrNs> jcrNamespaces = Arrays.asList(
                JcrNs.create(NS_PREFIX_FOO, NS_URI_FOO),
                JcrNs.create(NS_PREFIX_BAR, NS_URI_BAR));
        builder.withJcrNamespaces(jcrNamespaces);
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNamespaces);
        final List<QNodeTypeDefinition> nodetypes = JsonCnd.getQTypesFromJson(obj()
                .key("foo:primaryType").val(key("extends", arr().val("nt:base")))
                .key("foo:mixinType").val(key("@", arr().val("mixin")))
                .key("bar:primaryType").val(key("extends", arr().val("nt:base")))
                .key("bar:mixinType").val(key("@", arr().val("mixin"))).get(), mapping);
        builder.withJcrNodetypes(nodetypes);
        final List<PrivilegeDefinition> privileges =
                JsonCnd.getPrivilegesFromJson(arr("foo:canDo", "bar:canBe").get(), mapping);
        builder.withJcrPrivileges(privileges);
        final List<ForcedRoot> forcedRoots = Arrays.asList(
                ForcedRoot.fromJson(obj()
                        .key(ForcedRoot.keys().path(), "/test/foo")
                        .key(ForcedRoot.keys().primaryType(), "foo:primaryType")
                        .key(ForcedRoot.keys().mixinTypes(), arr().val("foo:mixinType"))
                        .get()),
                ForcedRoot.fromJson(obj()
                        .key(ForcedRoot.keys().path(), "/test/bar")
                        .key(ForcedRoot.keys().primaryType(), "bar:primaryType")
                        .key(ForcedRoot.keys().mixinTypes(), arr().val("bar:mixinType"))
                        .get()));
        builder.withForcedRoots(forcedRoots);
        InitStage initStage = builder.build().asInitStage();
        final OakMachine machine = new OakMachine.Builder().withInitStage(initStage).build();
        machine.adminInitAndInspect(session -> {
            assertEquals("foo namespace should be registered",
                    NS_URI_FOO, session.getNamespaceURI(NS_PREFIX_FOO));
            assertEquals("bar namespace should be registered",
                    NS_URI_BAR, session.getNamespaceURI(NS_PREFIX_BAR));
            assertTrue("/test/foo path exists", session.nodeExists("/test/foo"));
            assertTrue("/test/foo is a foo:primaryType",
                    session.getNode("/test/foo").isNodeType("foo:primaryType"));
            assertTrue("/test/foo is a foo:mixinType",
                    session.getNode("/test/foo").isNodeType("foo:mixinType"));
            assertTrue("/test/bar path exists", session.nodeExists("/test/bar"));
            assertTrue("/test/bar is a bar:primaryType",
                    session.getNode("/test/bar").isNodeType("bar:primaryType"));
            assertTrue("/test/bar is a bar:mixinType",
                    session.getNode("/test/bar").isNodeType("bar:mixinType"));
        });
    }

    @Test
    public void testToJsonFromBuilder() {
        Checklist.Builder builder = new Checklist.Builder("test");
        builder.withName("name");
        final URL cndA = cndAUrl;
        final URL cndB = cndBUrl;
        final List<URL> cndUrls = Arrays.asList(cndA, cndB);
        final JsonArray cndUrlsJson = JavaxJson.wrap(cndUrls).asJsonArray();
        builder.withCndUrls(cndUrls);
        final String NS_PREFIX_FOO = "foo";
        final String NS_URI_FOO = "http://foo.com";
        final String NS_PREFIX_BAR = "bar";
        final String NS_URI_BAR = "http://bar.com";
        final List<JcrNs> jcrNamespaces = Arrays.asList(
                JcrNs.create(NS_PREFIX_FOO, NS_URI_FOO),
                JcrNs.create(NS_PREFIX_BAR, NS_URI_BAR));
        final JsonArray jcrNamespacesJson = JavaxJson.wrap(jcrNamespaces).asJsonArray();
        builder.withJcrNamespaces(jcrNamespaces);
        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(jcrNamespaces);
        final List<QNodeTypeDefinition> nodetypes = JsonCnd.getQTypesFromJson(obj()
                .key("foo:primaryType").val(key("extends", arr().val("nt:base")))
                .key("foo:mixinType").val(key("@", arr().val("mixin")))
                .key("bar:primaryType").val(key("extends", arr().val("nt:base")))
                .key("bar:mixinType").val(key("@", arr().val("mixin"))).get(), mapping);
        final JsonObject nodetypesJson = JsonCnd.toJson(nodetypes, mapping);
        builder.withJcrNodetypes(nodetypes);
        final JsonArray privilegesJson = arr("bar:canBe", "foo:canDo").get();
        final List<PrivilegeDefinition> privileges =
                JsonCnd.getPrivilegesFromJson(privilegesJson, mapping);
        builder.withJcrPrivileges(privileges);
        final List<ForcedRoot> forcedRoots = Arrays.asList(
                ForcedRoot.fromJson(obj()
                        .key(ForcedRoot.keys().path(), "/test/foo")
                        .key(ForcedRoot.keys().primaryType(), "foo:primaryType")
                        .key(ForcedRoot.keys().mixinTypes(), arr().val("foo:mixinType"))
                        .get()),
                ForcedRoot.fromJson(obj()
                        .key(ForcedRoot.keys().path(), "/test/bar")
                        .key(ForcedRoot.keys().primaryType(), "bar:primaryType")
                        .key(ForcedRoot.keys().mixinTypes(), arr().val("bar:mixinType"))
                        .get()));
        final JsonArray forcedRootsJson = JavaxJson.wrap(forcedRoots).asJsonArray();
        builder.withForcedRoots(forcedRoots);
        CheckSpec validSpec1 = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid1")
                .key(CheckSpec.keys().impl(), "impl").get());
        CheckSpec validSpec2 = CheckSpec.fromJson(key(CheckSpec.keys().name(), "valid2")
                .key(CheckSpec.keys().impl(), "impl").get());
        final List<CheckSpec> checks = Arrays.asList(validSpec1, validSpec2);
        final JsonArray checksJson = JavaxJson.wrap(checks).asJsonArray();
        builder.withChecks(checks);
        final Checklist checklist = builder.build();
        final Checklist.JsonKeys keys = Checklist.keys();
        final JsonObject expectJson = obj()
                .key(keys.name(), "name")
                .key(keys.checks(), checksJson)
                .key(keys.forcedRoots(), forcedRootsJson)
                .key(keys.cndUrls(), cndUrlsJson)
                .key(keys.jcrNodetypes(), nodetypesJson)
                .key(keys.jcrPrivileges(), privilegesJson)
                .key(keys.jcrNamespaces(), jcrNamespacesJson)
                .get();
        final JsonObject json = checklist.toJson();
        assertEquals("toJson should match", expectJson, json);
    }

    @Test
    public void testToString() {
        final Checklist checklist = Checklist.fromJson("test", null, obj().get());
        final String expectString = "Checklist{moduleName='test', json='{}'}";
        assertEquals("empty json checklist toString should be", expectString, checklist.toString());

    }
}
