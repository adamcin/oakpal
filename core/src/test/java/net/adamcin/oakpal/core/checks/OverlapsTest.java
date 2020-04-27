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
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.testing.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertTrue;

public class OverlapsTest extends ProgressCheckTestBase {
    @Test
    public void testOverlaps() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Overlaps().newInstance(obj().get());
            CheckReport report = scanWithCheck(check, "test_a-1.0.zip", "test_b-1.0.zip");
            logViolations("testOverlaps:none", report);
            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Overlaps().newInstance(obj().get());
            CheckReport report = scanWithCheck(check, "tmp_foo.zip", "tmp_foo_bar.zip", "tmp_foo_bar_test.zip");
            logViolations("testOverlaps:[foo, foo_bar, foo_bar_test]", report);
            Assert.assertEquals("two violations", 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Overlaps().newInstance(obj().get());
            CheckReport report = scanWithCheck(check, "tmp_foo_bar_test.zip", "tmp_foo_bar.zip", "tmp_foo.zip");
            logViolations("testOverlaps:[foo_bar_test, foo_bar, foo]", report);
            Assert.assertEquals("two violations", 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Overlaps().newInstance(obj().key(Overlaps.keys().reportAllOverlaps(), true).get());
            CheckReport report = scanWithCheck(check, "tmp_foo_bar_test.zip", "tmp_foo_bar.zip", "tmp_foo.zip");
            logViolations("testOverlaps:[foo_bar_test, foo_bar, foo]:reportAllOverlaps", report);
            Assert.assertEquals("three violations", 3, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }
}
