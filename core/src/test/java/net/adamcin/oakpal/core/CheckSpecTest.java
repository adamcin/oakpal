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

import net.adamcin.oakpal.api.ProgressCheck;
import org.junit.Test;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CheckSpecTest {

    @Test
    public void testMustInherit() {
        CheckSpec hasTemplate = new CheckSpec();
        assertFalse("!mustInherit if template is null", hasTemplate.mustInherit());
        hasTemplate.setTemplate(" ");
        assertFalse("!mustInherit if template is blank", hasTemplate.mustInherit());
        hasTemplate.setTemplate("some/other/check");
        assertTrue("mustInherit if template is non-empty", hasTemplate.mustInherit());
    }

    @Test
    public void testOverrides() {
        CheckSpec left = new CheckSpec();
        CheckSpec right = new CheckSpec();
        assertFalse("left should not override right if either is unnamed", left.overrides(right));
        assertFalse("right should not override left if either is unnamed", left.isOverriddenBy(right));
        left.setName("base");
        right.setName("base");
        left.setTemplate("inheriting");
        assertFalse("left and right should not override each other when left must inherit", left.overrides(right));
        assertFalse("right and left should not override each other when left must inherit", left.isOverriddenBy(right));
        left.setTemplate(null);
        right.setTemplate("inheriting");
        assertFalse("left and right should not override each other when right must inherit", left.overrides(right));
        assertFalse("right and left should not override each other when right must inherit", left.isOverriddenBy(right));
        right.setTemplate(null);

        assertTrue("left and right should override each other with same name", left.overrides(right));
        assertTrue("right and left should override each other with same name", left.isOverriddenBy(right));
        left.setName("main/base");
        assertFalse("left should not override right if left name is not same or suffix of right name", left.overrides(right));
        assertTrue("right should override left if right is suffix of left name", left.isOverriddenBy(right));
        right.setName("core/base");
        assertFalse("right should not override left if right name is not same or suffix of left name", left.isOverriddenBy(right));
    }


    @Test
    public void testBaseCompositeOver() {
        CheckSpec left = new CheckSpec();
        CheckSpec right = new CheckSpec();
        CheckSpec emptyComposite = left.baseCompositeOver(right);
        assertNull("getName is null", emptyComposite.getName());
        assertNull("getImpl is null", emptyComposite.getImpl());
        assertNull("getTemplate is null", emptyComposite.getTemplate());
        assertNull("getInlineScript is null", emptyComposite.getInlineScript());
        assertNull("getInlineEngine is null", emptyComposite.getInlineEngine());
        assertFalse("isSkip is false", emptyComposite.isSkip());
        assertEquals("getConfig is empty, not null", JsonValue.EMPTY_JSON_OBJECT, emptyComposite.getConfig());

        right.setName("right_name");
        right.setImpl("right_impl");
        right.setTemplate("right_template");
        right.setInlineScript("right_inlineScript");
        right.setInlineEngine("right_inlineEngine");
        right.setConfig(key("side", "right").key("onlyRight", "onlyRight").get());
        right.setSkip(true);

        final CheckSpec allRight = left.baseCompositeOver(right);
        assertEquals("getName is right_name", "right_name", allRight.getName());
        assertEquals("getImpl is right_impl", "right_impl", allRight.getImpl());
        assertEquals("getTemplate is right_template", "right_template", allRight.getTemplate());
        assertEquals("getInlineScript is right_inlineScript", "right_inlineScript", allRight.getInlineScript());
        assertEquals("getInlineEngine is right_inlineEngine", "right_inlineEngine", allRight.getInlineEngine());
        assertTrue("isSkip is true", allRight.isSkip());
        assertEquals("getConfig side is right", "right", allRight.getConfig().getString("side"));
        assertEquals("getConfig onlyRight is onlyRight", "right", allRight.getConfig().getString("side"));

        left.setConfig(key("side", "left").key("onlyLeft", "onlyLeft").get());
        final CheckSpec leftConfig = left.baseCompositeOver(right);

        assertEquals("getConfig side is left", "left", leftConfig.getConfig().getString("side"));
        assertEquals("getConfig onlyRight is onlyRight", "onlyRight", leftConfig.getConfig().getString("onlyRight"));
        assertEquals("getConfig onlyLeft is onlyLeft", "onlyLeft", leftConfig.getConfig().getString("onlyLeft"));

        left.setImpl("left_impl");
        final CheckSpec leftImpl = left.baseCompositeOver(right);
        assertEquals("getImpl is left_impl", "left_impl", leftImpl.getImpl());
        assertNull("getInlineScript is null", leftImpl.getInlineScript());
        assertNull("getInlineEngine is null", leftImpl.getInlineEngine());


        left.setInlineScript("left_inlineScript");
        final CheckSpec leftScript = left.baseCompositeOver(right);
        assertNull("getImpl is left_impl", leftScript.getImpl());
        assertEquals("getInlineScript is left_inlineScript", "left_inlineScript", leftScript.getInlineScript());
        assertNull("getInlineEngine is null", leftScript.getInlineEngine());

        left.setInlineEngine("left_inlineEngine");
        final CheckSpec leftEngine = left.baseCompositeOver(right);
        assertNull("getImpl is left_impl", leftEngine.getImpl());
        assertEquals("getInlineScript is left_inlineScript", "left_inlineScript", leftEngine.getInlineScript());
        assertEquals("getInlineEngine is left_inlineEngine", "left_inlineEngine", leftEngine.getInlineEngine());
    }

    @Test
    public void testOverlay() {
        CheckSpec left = new CheckSpec();
        CheckSpec right = new CheckSpec();
        CheckSpec emptyComposite = left.overlay(right);
        assertNull("getName is null", emptyComposite.getName());
        assertNull("getImpl is null", emptyComposite.getImpl());
        assertNull("getTemplate is null", emptyComposite.getTemplate());
        assertNull("getInlineScript is null", emptyComposite.getInlineScript());
        assertNull("getInlineEngine is null", emptyComposite.getInlineEngine());
        assertFalse("isSkip is false", emptyComposite.isSkip());
        assertEquals("getConfig is empty, not null", JsonValue.EMPTY_JSON_OBJECT, emptyComposite.getConfig());

        right.setName("right_name");
        right.setImpl("right_impl");
        right.setTemplate("right_template");
        right.setInlineScript("right_inlineScript");
        right.setInlineEngine("right_inlineEngine");
        right.setConfig(key("side", "right").key("onlyRight", "onlyRight").get());
        right.setSkip(true);

        final CheckSpec allRight = left.overlay(right);
        assertEquals("getName is right_name", "right_name", allRight.getName());
        assertEquals("getImpl is right_impl", "right_impl", allRight.getImpl());
        assertEquals("getTemplate is right_template", "right_template", allRight.getTemplate());
        assertEquals("getInlineScript is right_inlineScript", "right_inlineScript", allRight.getInlineScript());
        assertEquals("getInlineEngine is right_inlineEngine", "right_inlineEngine", allRight.getInlineEngine());
        assertTrue("isSkip is true", allRight.isSkip());
        assertEquals("getConfig side is right", "right", allRight.getConfig().getString("side"));
        assertEquals("getConfig onlyRight is onlyRight", "right", allRight.getConfig().getString("side"));


        left.setConfig(key("side", "left").key("onlyLeft", "onlyLeft").get());
        final CheckSpec leftConfig = left.overlay(right);

        assertEquals("getConfig side is left", "left", leftConfig.getConfig().getString("side"));
        assertEquals("getConfig onlyRight is onlyRight", "onlyRight", leftConfig.getConfig().getString("onlyRight"));
        assertEquals("getConfig onlyLeft is onlyLeft", "onlyLeft", leftConfig.getConfig().getString("onlyLeft"));

        left.setImpl("left_impl");
        final CheckSpec leftImpl = left.overlay(right);
        assertEquals("getImpl is left_impl", "left_impl", leftImpl.getImpl());
        assertNull("getInlineScript is null", leftImpl.getInlineScript());
        assertNull("getInlineEngine is null", leftImpl.getInlineEngine());


        left.setInlineScript("left_inlineScript");
        final CheckSpec leftScript = left.overlay(right);
        assertNull("getImpl is left_impl", leftScript.getImpl());
        assertEquals("getInlineScript is left_inlineScript", "left_inlineScript", leftScript.getInlineScript());
        assertNull("getInlineEngine is null", leftScript.getInlineEngine());

        left.setInlineEngine("left_inlineEngine");
        final CheckSpec leftEngine = left.overlay(right);
        assertNull("getImpl is left_impl", leftEngine.getImpl());
        assertEquals("getInlineScript is left_inlineScript", "left_inlineScript", leftEngine.getInlineScript());
        assertEquals("getInlineEngine is left_inlineEngine", "left_inlineEngine", leftEngine.getInlineEngine());

        left.setName("left_name");
        final CheckSpec leftName = left.overlay(right);
        assertEquals("getName is right_name", "right_name", leftName.getName());

        left.setSkip(true);
        final CheckSpec leftSkip = left.overlay(right);
        assertTrue("isSkip is true (left is skipped)", leftSkip.isSkip());

        right.setSkip(false);
        final CheckSpec rightNoSkip = left.overlay(right);
        assertTrue("isSkip is true (left yes, right no)", rightNoSkip.isSkip());

        left.setSkip(false);
        final CheckSpec noSkip = left.overlay(right);
        assertFalse("isSkip is false", noSkip.isSkip());

    }

    @Test
    public void testInherit() {
        CheckSpec left = new CheckSpec();
        CheckSpec right = new CheckSpec();
        CheckSpec emptyComposite = left.inherit(right);
        assertNull("getName is null", emptyComposite.getName());
        assertNull("getImpl is null", emptyComposite.getImpl());
        assertNull("getTemplate is null", emptyComposite.getTemplate());
        assertNull("getInlineScript is null", emptyComposite.getInlineScript());
        assertNull("getInlineEngine is null", emptyComposite.getInlineEngine());
        assertFalse("isSkip is false", emptyComposite.isSkip());
        assertEquals("getConfig is empty, not null", JsonValue.EMPTY_JSON_OBJECT, emptyComposite.getConfig());

        right.setName("right_name");
        right.setImpl("right_impl");
        right.setTemplate("right_template");
        right.setInlineScript("right_inlineScript");
        right.setInlineEngine("right_inlineEngine");
        right.setConfig(key("side", "right").key("onlyRight", "onlyRight").get());
        right.setSkip(true);

        final CheckSpec allRight = left.inherit(right);
        assertEquals("getName is right_name", "right_name", allRight.getName());
        assertEquals("getImpl is right_impl", "right_impl", allRight.getImpl());
        assertEquals("getTemplate is right_template", "right_template", allRight.getTemplate());
        assertEquals("getInlineScript is right_inlineScript", "right_inlineScript", allRight.getInlineScript());
        assertEquals("getInlineEngine is right_inlineEngine", "right_inlineEngine", allRight.getInlineEngine());
        assertFalse("isSkip is false (left is not skipped)", allRight.isSkip());
        assertEquals("getConfig side is right", "right", allRight.getConfig().getString("side"));
        assertEquals("getConfig onlyRight is onlyRight", "right", allRight.getConfig().getString("side"));

        left.setConfig(key("side", "left").key("onlyLeft", "onlyLeft").get());
        final CheckSpec leftConfig = left.inherit(right);

        assertEquals("getConfig side is left", "left", leftConfig.getConfig().getString("side"));
        assertEquals("getConfig onlyRight is onlyRight", "onlyRight", leftConfig.getConfig().getString("onlyRight"));
        assertEquals("getConfig onlyLeft is onlyLeft", "onlyLeft", leftConfig.getConfig().getString("onlyLeft"));

        left.setImpl("left_impl");
        final CheckSpec leftImpl = left.inherit(right);
        assertEquals("getImpl is left_impl", "left_impl", leftImpl.getImpl());
        assertNull("getInlineScript is null", leftImpl.getInlineScript());
        assertNull("getInlineEngine is null", leftImpl.getInlineEngine());


        left.setInlineScript("left_inlineScript");
        final CheckSpec leftScript = left.inherit(right);
        assertNull("getImpl is left_impl", leftScript.getImpl());
        assertEquals("getInlineScript is left_inlineScript", "left_inlineScript", leftScript.getInlineScript());
        assertNull("getInlineEngine is null", leftScript.getInlineEngine());

        left.setInlineEngine("left_inlineEngine");
        final CheckSpec leftEngine = left.inherit(right);
        assertNull("getImpl is left_impl", leftEngine.getImpl());
        assertEquals("getInlineScript is left_inlineScript", "left_inlineScript", leftEngine.getInlineScript());
        assertEquals("getInlineEngine is left_inlineEngine", "left_inlineEngine", leftEngine.getInlineEngine());

        left.setName("left_name");
        final CheckSpec leftName = left.inherit(right);
        assertEquals("getName is left_name", "left_name", leftName.getName());

        left.setSkip(true);
        final CheckSpec leftSkip = left.inherit(right);
        assertTrue("isSkip is true (left is skipped)", leftSkip.isSkip());
    }

    @Test
    public void testInherits() {
        CheckSpec left = new CheckSpec();
        CheckSpec right = new CheckSpec();
        assertFalse("null left should not inherit null right", left.inherits(right));
        assertFalse("null right should not inherit null left", left.isInheritedBy(right));
        left.setName("left");
        left.setTemplate("");
        right.setName("");
        assertFalse("empty left should not inherit empty right", left.inherits(right));
        assertFalse("empty right should not inherit empty left", left.isInheritedBy(right));
        right.setName("right");
        assertFalse("empty left should not inherit empty right", left.inherits(right));
        assertFalse("empty right should not inherit empty left", left.isInheritedBy(right));
        left.setTemplate("notright");
        assertFalse("left with other template should not inherit right", left.inherits(right));
        assertFalse("right should not inherit left with other template", left.isInheritedBy(right));
        left.setTemplate("right");
        assertTrue("left with match template should inherit right", left.inherits(right));
        assertFalse("right should not inherit left with match template", left.isInheritedBy(right));
        right.setName("main/right");
        assertTrue("left with suffix-matching template should inherit right", left.inherits(right));
        assertFalse("right should not inherit left with suffix matching template", left.isInheritedBy(right));
    }

    @Test
    public void testMerge() {
        CheckSpec base = CheckSpec.fromJson(key("name", "acHandling").key("config", key("levelSet", "only_add")).get());
        CheckSpec overlay = CheckSpec.fromJson(key("name", "acHandling").key("config", key("levelSet", "no_unsafe")).get());
        CheckSpec merged = overlay.overlay(base);
        assertEquals("should be no_unsafe", "no_unsafe", merged.getConfig().getString("levelSet"));
    }

    @Test
    public void testInlineScript() throws Exception {
        CheckSpec inlineEmpty = CheckSpec.fromJson(key("inlineScript", null).get());
        assertTrue("Null inlineScript with no impl should be isAbstract", inlineEmpty.isAbstract());

        CheckSpec inline = CheckSpec.fromJson(key("inlineScript", "function importedPath(packageId, path) { print(path); }").get());
        List<ProgressCheck> checks = Locator.loadFromCheckSpecs(Collections.singletonList(inline));

        checks.get(0).importedPath(null, "/foo", null);
    }

    @Test
    public void testGetters() {
        final CheckSpec spec = new CheckSpec();
        assertNull("getName is null", spec.getName());
        assertNull("getImpl is null", spec.getImpl());
        assertNull("getTemplate is null", spec.getTemplate());
        assertNull("getConfig is null", spec.getConfig());
        assertNull("getInlineScript is null", spec.getInlineScript());
        assertNull("getInlineEngine is null", spec.getInlineEngine());
        assertFalse("isSkip is false", spec.isSkip());
        assertTrue("notSkipped is true", spec.notSkipped());

        spec.setName("name");
        assertEquals("setName getName same", "name", spec.getName());
        spec.setImpl("impl");
        assertEquals("setImpl getImpl same", "impl", spec.getImpl());
        spec.setTemplate("template");
        assertEquals("setTemplate getTemplate same", "template", spec.getTemplate());
        spec.setInlineScript("inlineScript");
        assertEquals("setInlineScript getInlineScript same", "inlineScript", spec.getInlineScript());
        spec.setInlineEngine("inlineEngine");
        assertEquals("setInlineEngine getInlineEngine same", "inlineEngine", spec.getInlineEngine());
        final JsonObject config = obj().get();
        spec.setConfig(config);
        assertSame("setConfig getConfig same", config, spec.getConfig());
        spec.setSkip(true);
        assertTrue("isSkip is true", spec.isSkip());
        assertFalse("notSkipped is false", spec.notSkipped());
    }

    @Test
    public void testFromJson() {
        final CheckSpec.JsonKeys keys = CheckSpec.keys();
        final JsonObject specJson = obj()
                .key(keys.name(), keys.name())
                .key(keys.impl(), keys.impl())
                .key(keys.inlineScript(), keys.inlineScript())
                .key(keys.inlineEngine(), keys.inlineEngine())
                .key(keys.config(), key("foo", "bar"))
                .key(keys.template(), keys.template())
                .key(keys.skip(), true)
                .get();
        final CheckSpec spec = CheckSpec.fromJson(specJson);
        assertEquals("fromJson getName same", keys.name(), spec.getName());
        assertEquals("fromJson getImpl same", keys.impl(), spec.getImpl());
        assertEquals("fromJson getTemplate same", keys.template(), spec.getTemplate());
        assertEquals("fromJson getInlineScript same", keys.inlineScript(), spec.getInlineScript());
        assertEquals("fromJson getInlineEngine same", keys.inlineEngine(), spec.getInlineEngine());
        assertEquals("fromJson getConfig same", key("foo", "bar").get(), spec.getConfig());
        assertTrue("fromJson isSkip true", spec.isSkip());
        assertEquals("json should be equal", specJson, spec.toJson());
        assertEquals("toString should equal toJson().toString",
                specJson.toString(), spec.toString());
    }

    @Test
    public void testCopyOf() {
        final CheckSpec.JsonKeys keys = CheckSpec.keys();
        final JsonObject specJson = obj()
                .key(keys.name(), keys.name())
                .key(keys.impl(), keys.impl())
                .key(keys.inlineScript(), keys.inlineScript())
                .key(keys.inlineEngine(), keys.inlineEngine())
                .key(keys.config(), key("foo", "bar"))
                .key(keys.template(), keys.template())
                .key(keys.skip(), true)
                .get();
        final CheckSpec spec = CheckSpec.fromJson(specJson);
        assertEquals("copy should equal copied", spec, CheckSpec.copyOf(spec));
        assertEquals("immutable copy should equal copied", spec, CheckSpec.immutableCopyOf(spec));
        assertEquals("copied should equal copy", CheckSpec.copyOf(spec), spec);
        assertEquals("copied should equal immutable copy", CheckSpec.immutableCopyOf(spec), spec);
        final Set<CheckSpec> specSet = new HashSet<>();
        specSet.add(spec);
        specSet.add(CheckSpec.copyOf(spec));
        assertEquals("hashSet should still only have one.", 1, specSet.size());
        specSet.add(CheckSpec.immutableCopyOf(spec));
        assertEquals("hashSet should still only have one.", 1, specSet.size());
        final CheckSpec diffed = CheckSpec.copyOf(spec);
        diffed.setName("eman");
        specSet.add(diffed);
        assertEquals("hashSet should now have two.", 2, specSet.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetName() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setName("name");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetImpl() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setImpl("impl");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetTemplate() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setTemplate("template");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetInlineScript() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setInlineScript("inlineScript");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetInlineEngine() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setInlineEngine("inlineEngine");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetConfig() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setConfig(obj().get());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableSetSkip() {
        CheckSpec.immutableCopyOf(new CheckSpec()).setSkip(true);
    }
}
