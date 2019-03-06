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

import static net.adamcin.oakpal.core.JavaxJson.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

public class CheckSpecTest {

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
}
