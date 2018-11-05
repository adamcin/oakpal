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

package net.adamcin.oakpal.maven.component;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class JSONObjectConverterTest {

    @Test
    public void testPlexusPluralStem() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("meetings", "meeting");
        cases.put("caresses", "caress");
        cases.put("flatFiles", "file");
        cases.put("pinkUnicornPonies", "poni");
        cases.put("pony", "poni");
        cases.put("some_people_like_snakes", "snake");
        cases.put("snake", "snake");
        cases.put("some.people.like.dots", "dot");
        cases.put("denyNodeTypes", "type");
        cases.put("denyNodeType", "type");
        cases.put("abyss", "abyss");
        cases.put("why", "whi");
        cases.put("whys", "whi");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Assert.assertEquals("should stem to", entry.getValue(),
                    JSONObjectConverter.plexusPluralStem(entry.getKey()));
        }

    }
}
