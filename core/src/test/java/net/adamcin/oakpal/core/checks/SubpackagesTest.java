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

public class SubpackagesTest extends ProgressCheckTestBase {

    @Test
    public void testDenyAll() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject(
                                "{\"denyAll\":false}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("denyAll:false", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject(
                                "{\"denyAll\":true}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("denyAll:false", report);
                assertEquals("two violations", 2, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }

    @Test
    public void testPatterns() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject(
                                "{\"rules\":[]}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("testPatterns:[]", report);
                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject("{\"rules\":[{\"type\":\"deny\",\"pattern\":\"my_packages:sub_.*\"}]}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("testPatterns:sub_.*", report);
                assertEquals("two violations", 2, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject("{\"rules\":[{\"type\":\"deny\",\"pattern\":\"my_packages:sub_.*\"},{\"type\":\"allow\",\"pattern\":\"my_packages:sub_a\"}]}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("testPatterns:sub_.* - sub_a", report);
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject("{\"rules\":[{\"type\":\"deny\",\"pattern\":\"my_packages:sub_a\"}]}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("testPatterns:sub_a", report);
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject("{\"rules\":[{\"type\":\"deny\",\"pattern\":\"my_packages:sub_b\"}]}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("testPatterns:sub_b", report);
                assertEquals("one violation", 1, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                ProgressCheck check = new Subpackages().newInstance(
                        new JSONObject("{\"rules\":[{\"type\":\"deny\",\"pattern\":\"my_packages:sub_c\"}]}"));
                CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
                logViolations("testPatterns:sub_c", report);
                assertEquals("one violation", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
            }
        });
    }
}
