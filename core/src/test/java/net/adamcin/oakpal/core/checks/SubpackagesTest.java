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
import static net.adamcin.oakpal.core.OrgJson.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.TestUtil;
import org.junit.Test;

public class SubpackagesTest extends ProgressCheckTestBase {

    @Test
    public void testDenyAll() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(key("denyAll", false).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("denyAll:false", report);
            assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(key("denyAll", true).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("denyAll:false", report);
            assertEquals("two violations", 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testPatterns() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(key("rules", arr()).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("testPatterns:[]", report);
            assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(
                    key("rules", arr(key("type", "deny").key("pattern", "my_packages:sub_.*"))).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("testPatterns:sub_.*", report);
            assertEquals("two violations", 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(
                    key("rules", arr()
                            .val(key("type", "deny").key("pattern", "my_packages:sub_.*"))
                            .val(key("type", "allow").key("pattern", "my_packages:sub_a"))
                    ).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("testPatterns:sub_.* - sub_a", report);
            assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(
                    key("rules", arr(key("type", "deny").key("pattern", "my_packages:sub_a"))).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("testPatterns:sub_a", report);
            assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(
                    key("rules", arr(key("type", "deny").key("pattern", "my_packages:sub_b"))).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("testPatterns:sub_b", report);
            assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Subpackages().newInstance(
                    key("rules", arr(key("type", "deny").key("pattern", "my_packages:sub_c"))).get());
            CheckReport report = scanWithCheck(check, "subtest_with_content.zip");
            logViolations("testPatterns:sub_c", report);
            assertEquals("one violation", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }
}
