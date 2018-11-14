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

import java.util.ArrayList;

import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import org.json.JSONObject;
import org.junit.Test;

public class JcrPropertiesTest extends ProgressCheckTestBase {

    @Test
    public void testDenyIfAbsent() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_man\",\"denyIfAbsent\":false}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                logViolations("double_man, no scope", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_man\",\"denyIfAbsent\":true}],\"scopePaths\":[{\"pattern\":\"/tmp/jcr:content\",\"type\":\"allow\"}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_man\",\"denyIfAbsent\":false}],\"scopePaths\":[{\"pattern\":\"/tmp/jcr:content\",\"type\":\"allow\"}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                logViolations("double_man, with scope", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }

    @Test
    public void testDenyIfPresent() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_nan\",\"denyIfPresent\":false}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                logViolations("double_nan, no scope", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_nan\",\"denyIfPresent\":true}],\"scopePaths\":[{\"pattern\":\"/tmp/jcr:content\",\"type\":\"allow\"}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_nan\",\"denyIfPresent\":false}],\"scopePaths\":[{\"pattern\":\"/tmp/jcr:content\",\"type\":\"allow\"}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                logViolations("double_nan, with scope", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }

    @Test
    public void testRequireType() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_nan\",\"requireType\":\"Double\"},{\"name\":\"double_neg_inf\",\"requireType\":\"Double\"}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new JcrProperties().newInstance(
                        new JSONObject(
                                "{\"properties\":[{\"name\":\"double_nan\",\"requireType\":\"String\"},{\"name\":\"double_neg_inf\",\"requireType\":\"String\"}]}"));
                CheckReport report = scanWithCheck(check, "double_properties.zip");
                LOGGER.info("violations: {}", new ArrayList<>(report.getViolations()));
                assertEquals("two violations", 2, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }
}
