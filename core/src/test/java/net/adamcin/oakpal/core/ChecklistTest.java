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
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import net.adamcin.commons.testing.junit.TestBody;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

public class ChecklistTest {

    @Test
    public void testFromJSON() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                try (InputStream is = getClass().getResourceAsStream("/simpleChecklist.json")) {
                    Checklist checklist = Checklist.fromJSON("core-tests", null,
                            new JSONObject(new JSONTokener(is)));

                    assertNotNull("checklist should not be null", checklist);
                    assertEquals("checklist moduleName", "core-tests", checklist.getModuleName());
                    assertEquals("checklist name", "simpleChecklist", checklist.getName());
                    assertEquals("checklist jcr prefix", "oakpal", checklist.getJcrNamespaces().get(0).getPrefix());
                }
            }
        });


    }
}
