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

package net.adamcin.oakpal.core.checks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import org.json.JSONObject;
import org.junit.Test;

public class AcHandlingTest extends ProgressCheckTestBase {

    @Test
    public void testLevelSet() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new AcHandling().newInstance(
                        new JSONObject(
                                "{\"levelSet\":\"no_unsafe\"}"));
                CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
                logViolations("level_set:no_unsafe", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new AcHandling().newInstance(
                        new JSONObject(
                                "{\"levelSet\":\"only_ignore\"}"));
                CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
                logViolations("level_set:only_ignore", report);
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }

    @Test
    public void testAllowedModes() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new AcHandling().newInstance(
                        new JSONObject(
                                "{\"allowedModes\":[\"merge_preserve\"]}"));
                CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
                logViolations("allowedModes:merge_preserve", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new AcHandling().newInstance(
                        new JSONObject(
                                "{\"allowedModes\":[\"ignore\"]}"));
                CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
                logViolations("allowedModes:ignore", report);
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }
}
