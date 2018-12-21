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

import static net.adamcin.oakpal.core.OrgJson.arr;
import static net.adamcin.oakpal.core.OrgJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.Violation;
import org.json.JSONObject;
import org.junit.Test;

public class PathsTest extends ProgressCheckTestBase {

    @Test
    public void testDefaultSeverity() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                JSONObject config = obj()
                        .key("rules", arr()
                                .and(obj().key("type", "deny").key("pattern", "/etc(/.*)?")))
                        .get();
                ProgressCheck check = new Paths().newInstance(config);
                CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
                logViolations("level_set:no_unsafe", report);
                assertEquals("violations", 6, report.getViolations().size());
                assertTrue("all violations are MAJOR", report.getViolations().stream()
                        .allMatch(viol -> viol.getSeverity().equals(Violation.Severity.MAJOR)));
            }
        });
    }

    @Test
    public void testCustomSeverity() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                JSONObject config = obj()
                        .key("rules", arr()
                                .and(obj().key("type", "deny").key("pattern", "/etc(/.*)?")))
                        .key("severity", "SEVERE")
                        .get();
                ProgressCheck check = new Paths().newInstance(config);
                CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
                logViolations("level_set:no_unsafe", report);
                assertEquals("violations", 6, report.getViolations().size());
                assertTrue("all violations are SEVERE", report.getViolations().stream()
                        .allMatch(viol -> viol.getSeverity().equals(Violation.Severity.SEVERE)));
            }
        });
    }

    @Test
    public void testCustomSeverityMixedCase() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                JSONObject config = obj()
                        .key("rules", arr()
                                .and(obj().key("type", "deny").key("pattern", "/etc(/.*)?")))
                        .key("severity", "minor")
                        .get();
                ProgressCheck check = new Paths().newInstance(config);
                CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
                logViolations("level_set:no_unsafe", report);
                assertEquals("violations", 6, report.getViolations().size());
                assertTrue("all violations are MINOR", report.getViolations().stream()
                        .allMatch(viol -> viol.getSeverity().equals(Violation.Severity.MINOR)));
            }
        });
    }

    @Test
    public void testBadSeverity() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                boolean threw = false;
                try {
                    JSONObject config = obj()
                            .key("rules", arr()
                                    .and(obj().key("type", "deny").key("pattern", "/etc(/.*)?")))
                            .key("severity", "whoops")
                            .get();
                    ProgressCheck check = new Paths().newInstance(config);
                } catch (IllegalArgumentException e) {
                    threw = true;
                }
                assertTrue("bad severity should cause exception on construction.", threw);
            }
        });
    }
}
