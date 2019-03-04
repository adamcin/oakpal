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

import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.TestUtil;
import net.adamcin.oakpal.core.Violation;
import org.junit.Test;

public class FilterSetsTest extends ProgressCheckTestBase {

    @Test
    public void testRootImport() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(obj().get());
            CheckReport report = scanWithCheck(handler, "testrootimport.zip");
            assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });

        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(key("allowRootFilter", true).get());

            CheckReport report = scanWithCheck(handler, "testrootimport.zip");
            assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testModeMerge() throws Exception {
        TestUtil.testBlock(() -> {
                ProgressCheck handler = new FilterSets().newInstance(obj().get());
                CheckReport report = scanWithCheck(handler, "tmp_mode_merge.zip");

                assertEquals("one violation", 1, report.getViolations().size());
                assertEquals("is severity", Violation.Severity.MINOR,
                        report.getViolations().iterator().next().getSeverity());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
                ProgressCheck handler = new FilterSets().newInstance(key("importModeSeverity", "severe").get());
                CheckReport report = scanWithCheck(handler, "tmp_mode_merge.zip");

                assertEquals("one violation", 1, report.getViolations().size());
                assertEquals("is severity", Violation.Severity.SEVERE,
                        report.getViolations().iterator().next().getSeverity());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testEmptyFilter() throws Exception {
        TestUtil.testBlock(() -> {
                ProgressCheck handler = new FilterSets().newInstance(obj().get());
                CheckReport report = scanWithCheck(handler, "tmp_foo_bar_test_nofilter.zip");

                assertEquals("one violation", 1, report.getViolations().size());
                assertEquals("is severity", Violation.Severity.MAJOR,
                        report.getViolations().iterator().next().getSeverity());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
                ProgressCheck handler = new FilterSets().newInstance(key("allowEmptyFilter", true).get());
                CheckReport report = scanWithCheck(handler, "tmp_foo_bar_test_nofilter.zip");

                assertEquals("no violations", 0, report.getViolations().size());
                assertTrue("all violations have packageIds", report.getViolations().stream()
                        .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }
}
