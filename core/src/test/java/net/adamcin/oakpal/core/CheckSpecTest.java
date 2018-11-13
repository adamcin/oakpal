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

import org.json.JSONObject;
import org.junit.Test;

public class CheckSpecTest {

    @Test
    public void testMerge() {
        CheckSpec base = CheckSpec.fromJSON(new JSONObject("{\"name\":\"acHandling\",\"config\":{\"levelSet\":\"only_add\"}}"));
        CheckSpec overlay = CheckSpec.fromJSON(new JSONObject("{\"name\":\"acHandling\",\"config\":{\"levelSet\":\"no_unsafe\"}}"));
        CheckSpec merged = overlay.overlay(base);
        assertEquals("should be no_unsafe", "no_unsafe", merged.getConfig().getString("levelSet"));
    }
}
