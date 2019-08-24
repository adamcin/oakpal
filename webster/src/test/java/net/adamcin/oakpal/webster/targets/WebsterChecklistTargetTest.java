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

package net.adamcin.oakpal.webster.targets;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.checks.Rule;
import net.adamcin.oakpal.webster.TestUtil;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.webster.ChecklistExporter.SelectorType.NODETYPE;
import static net.adamcin.oakpal.webster.ChecklistExporter.SelectorType.PATH;
import static net.adamcin.oakpal.webster.ChecklistExporter.SelectorType.QUERY;
import static net.adamcin.oakpal.webster.targets.WebsterChecklistTarget.selectorsFromConfigCompactForm;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WebsterChecklistTargetTest {
    final File testOutDir = new File("target/test-out/WebsterChecklistTargetTest");

    @Before
    public void setUp() throws Exception {
        testOutDir.mkdirs();
    }

    @Test
    public void testFromJsonTargetFactory() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJsonTargetFactory.json");
        WebsterTarget target = JsonTargetFactory.CHECKLIST.createTarget(targetFile, obj().get());
        assertTrue("target is checklist target: " + target.getClass().getSimpleName(),
                target instanceof WebsterChecklistTarget);
    }

    private void assertJsonFile(final @NotNull File file, final @NotNull Consumer<JsonObject> consumer) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file));
             JsonReader jsonReader = Json.createReader(reader)) {
            final JsonObject json = jsonReader.readObject();
            assertNotNull("json should not be null from file " + file.getPath(), json);
            consumer.accept(json);
        }
    }

    @Test
    public void testSelectorsFromConfigCompactForm() {
        List<WebsterChecklistTarget.Selector> pathSelector =
                selectorsFromConfigCompactForm(key("selectPaths", arr("/foo1", "/foo2")).get());
        assertEquals("expect type", PATH, pathSelector.get(0).getType());
        assertEquals("expect args", Arrays.asList("/foo1", "/foo2"), pathSelector.get(0).getArgs());

        List<WebsterChecklistTarget.Selector> ntSelector =
                selectorsFromConfigCompactForm(key("selectNodeTypes", arr("foo:nt1", "foo:mix1")).get());
        assertEquals("expect type", NODETYPE, ntSelector.get(0).getType());
        assertEquals("expect args", Arrays.asList("foo:nt1", "foo:mix1"), ntSelector.get(0).getArgs());

        List<WebsterChecklistTarget.Selector> querySelector =
                selectorsFromConfigCompactForm(key("selectQuery", "select * from [nt:base]").get());
        assertEquals("expect type", QUERY, querySelector.get(0).getType());
        assertEquals("expect args", Collections.singletonList("select * from [nt:base]"),
                querySelector.get(0).getArgs());
    }

    @Test
    public void testFromJson() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        OakMachine.Builder emptyOak = TestUtil.fromPlan(obj().get());
        WebsterChecklistTarget target = WebsterChecklistTarget.fromJson(targetFile, obj().get());
        emptyOak.build().initAndInspect(target::perform);

        OakMachine.Builder twoRootsOak = TestUtil.fromPlan(obj()
                .key("forcedRoots", arr()
                        .val(new ForcedRoot().withPath("/foo1"))
                        .val(new ForcedRoot().withPath("/foo2"))
                )
                .get());
        WebsterChecklistTarget twoRootsTarget = WebsterChecklistTarget.fromJson(targetFile, obj()
                .key("selectors", arr()
                        .val(key("type", "path").key("args", arr("/foo1", "/foo2")))
                )
                .get());
        twoRootsOak.build().initAndInspect(twoRootsTarget::perform);
        assertJsonFile(targetFile, json -> {
            final Checklist checklist = Checklist.fromJson("testFromJson", null, json);
            assertEquals("expect 2 roots", 2, checklist.getForcedRoots().size());
        });
    }

    @Test
    public void testFromJson_scopePaths() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_scopePaths.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        OakMachine.Builder emptyOak = TestUtil.fromPlan(obj().get());
        WebsterChecklistTarget target = WebsterChecklistTarget.fromJson(targetFile, obj().get());
        emptyOak.build().initAndInspect(target::perform);

        final List<JcrNs> nsList = Arrays.asList(JcrNs.create(OakMachine.NS_PREFIX_OAKPAL, OakMachine.NS_URI_OAKPAL));
        OakMachine.Builder twoRootsOak = TestUtil.fromPlan(obj()
                .key("forcedRoots", arr()
                        .val(new ForcedRoot().withPath("/foo1"))
                        .val(new ForcedRoot().withPath("/foo2"))
                )
                .get());
        WebsterChecklistTarget twoRootsTarget = WebsterChecklistTarget.fromJson(targetFile, obj()
                //.key("jcrNamespaces", nsList)
                .key("selectors", arr()
                        .val(key("type", "path").key("args", arr("/foo1", "/foo2"))))
                .key("scopePaths", arr(new Rule(Rule.RuleType.INCLUDE, Pattern.compile("/foo2"))))
                .key("nodeTypeFilters", arr(new Rule(Rule.RuleType.EXCLUDE, Pattern.compile(OakMachine.NS_PREFIX_OAKPAL + ":.*"))))
                .get());
        twoRootsOak.build().adminInitAndInspect(twoRootsTarget::perform);
        assertJsonFile(targetFile, json -> {
            final Checklist checklist = Checklist.fromJson("testFromJson", null, json);
            assertEquals("expect 1 roots", 1, checklist.getForcedRoots().size());
            assertEquals("expect root path", "/foo2", checklist.getForcedRoots().get(0).getPath());
            assertNull("expect root null primaryType", checklist.getForcedRoots().get(0).getPrimaryType());
        });

        OakMachine.Builder twoTypedRootsOak = TestUtil.fromPlan(obj()
                .key("forcedRoots", arr()
                        .val(new ForcedRoot().withPath("/foo1").withPrimaryType("nt:folder"))
                        .val(new ForcedRoot().withPath("/foo2").withPrimaryType("nt:folder"))
                )
                .get());
        WebsterChecklistTarget twoTypedRootsTarget = WebsterChecklistTarget.fromJson(targetFile, obj()
                .key("jcrNamespaces", nsList)
                .key("selectors", arr()
                        .val(key("type", "path").key("args", arr("/foo1", "/foo2"))))
                .key("scopePaths", arr(new Rule(Rule.RuleType.INCLUDE, Pattern.compile("/foo1"))))
                .key("nodeTypeFilters", arr(new Rule(Rule.RuleType.EXCLUDE, Pattern.compile("oakpaltmp:.*"))))
                .key("updatePolicy", "replace")
                .get());
        twoTypedRootsOak.build().adminInitAndInspect(twoTypedRootsTarget::perform);
        assertJsonFile(targetFile, json -> {
            final Checklist checklist = Checklist.fromJson("testFromJson", null, json);
            assertEquals("expect 1 roots", 2, checklist.getForcedRoots().size());
            assertEquals("expect root path", "/foo1", checklist.getForcedRoots().get(0).getPath());
            assertEquals("expect root primaryType of nt:folder", "nt:folder",
                    checklist.getForcedRoots().get(0).getPrimaryType());
        });

    }

    @Test
    public void testFromJson_nodeTypeFilters() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_nodeTypeFilters.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        final String fooMixin1 = "foo:mixin1";
        final String fooMixin2 = "foo:mixin2";

        final List<JcrNs> nsList = Collections.singletonList(JcrNs.create("foo", "http://foo.com"));
        OakMachine.Builder oak = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", nsList)
                .key("jcrNodetypes", obj()
                        .key("foo:folder", key("extends", arr("nt:folder")))
                        .key(fooMixin1, key("@", arr("mixin")))
                        .key(fooMixin2, key("@", arr("mixin"))))
                .key("forcedRoots", arr()
                        .val(new ForcedRoot().withPath("/foo1").withPrimaryType("nt:folder").withMixinTypes("mix:title", fooMixin1))
                        .val(new ForcedRoot().withPath("/foo2").withPrimaryType("nt:unstructured").withMixinTypes(fooMixin1))
                )
                .get());
        WebsterChecklistTarget target = WebsterChecklistTarget.fromJson(targetFile, obj()
                .key("jcrNamespaces", nsList)
                .key("selectors", arr()
                        .val(key("type", "nodetype").key("args", arr("mix:title")))
                        .val(key("type", "query").key("args", arr("select * from [nt:unstructured] as nun where issamenode([nun], '/foo2')"))))
                .key("nodeTypeFilters", arr()
                        .val(new Rule(Rule.RuleType.INCLUDE, Pattern.compile("foo:mixin.*"))))
                .key("exportNodeTypes", arr(fooMixin2))
                .get());
        oak.build().adminInitAndInspect(target::perform);
        assertJsonFile(targetFile, json -> {
            final Checklist checklist = Checklist.fromJson("testFromJson", null, json);
            assertEquals("expect 2 roots", 2, checklist.getForcedRoots().size());
            assertEquals("expect root path", "/foo1", checklist.getForcedRoots().get(0).getPath());
            assertNull("expect no primary type", checklist.getForcedRoots().get(0).getPrimaryType());
            assertEquals("expect mixins", Collections.singletonList(fooMixin1),
                    checklist.getForcedRoots().get(0).getMixinTypes());
            assertEquals("expect root path", "/foo2", checklist.getForcedRoots().get(1).getPath());
            assertNull("expect no primary type", checklist.getForcedRoots().get(1).getPrimaryType());
            assertEquals("expect mixins", Collections.singletonList(fooMixin1),
                    checklist.getForcedRoots().get(1).getMixinTypes());

            assertEquals("two types were exported", 2, checklist.getJcrNodetypes().size());
            final NamePathResolver resolver = new DefaultNamePathResolver(JsonCnd.toNamespaceMapping(nsList));
            assertEquals("foo:mixin1 node type was exported", uncheck1(resolver::getQName).apply(fooMixin1),
                    checklist.getJcrNodetypes().get(0).getName());
            assertEquals("foo:mixin2 node type was exported", uncheck1(resolver::getQName).apply(fooMixin2),
                    checklist.getJcrNodetypes().get(1).getName());
        });
    }
}