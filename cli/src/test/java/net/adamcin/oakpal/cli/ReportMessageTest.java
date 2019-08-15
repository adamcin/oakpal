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

package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.SimpleReport;
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Collections;

import static net.adamcin.oakpal.core.JavaxJson.key;
import static org.junit.Assert.*;

public class ReportMessageTest {

    @Test
    public void testConstruct() {
        final ReportMessage message = new ReportMessage(new SimpleReport("check", Collections.emptyList()));
        final JsonObject expected = key("checkName", "check").get();
        assertEquals("same json", expected, message.toJson());

        final String expectedString = "report: check";
        assertEquals("same string", expectedString, message.toString());

    }
}