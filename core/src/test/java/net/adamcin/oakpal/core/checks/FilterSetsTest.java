/*
 * Copyright 2020 Mark Adamcin
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

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.testing.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertTrue;

public class FilterSetsTest extends ProgressCheckTestBase {

    @Test
    public void testRootImport() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(obj().get());
            CheckReport report = scanWithCheck(handler, "testrootimport.zip");
            Assert.assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });

        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(key("allowRootFilter", true).get());

            CheckReport report = scanWithCheck(handler, "testrootimport.zip");
            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testModeMerge() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(obj().get());
            CheckReport report = scanWithCheck(handler, "tmp_mode_merge.zip");

            Assert.assertEquals("one violation", 1, report.getViolations().size());
            Assert.assertEquals("is severity", Severity.MINOR,
                    report.getViolations().iterator().next().getSeverity());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(key("importModeSeverity", "severe").get());
            CheckReport report = scanWithCheck(handler, "tmp_mode_merge.zip");

            Assert.assertEquals("one violation", 1, report.getViolations().size());
            Assert.assertEquals("is severity", Severity.SEVERE,
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

            Assert.assertEquals("one violation", 1, report.getViolations().size());
            Assert.assertEquals("is severity", Severity.MAJOR,
                    report.getViolations().iterator().next().getSeverity());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck handler = new FilterSets().newInstance(key("allowEmptyFilter", true).get());
            CheckReport report = scanWithCheck(handler, "tmp_foo_bar_test_nofilter.zip");

            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }
}
