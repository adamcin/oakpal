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

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AcHandlingTest extends ProgressCheckTestBase {

    @Test
    public void testLevelSet() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new AcHandling().newInstance(obj().key("levelSet", "no_unsafe").get());
            CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
            logViolations("level_set:no_unsafe", report);
            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new AcHandling().newInstance(obj().key("levelSet", "only_ignore").get());
            CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
            logViolations("level_set:only_ignore", report);
            Assert.assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testAllowedModes() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check =
                    new AcHandling().newInstance(obj().key("allowedModes", arr("merge_preserve")).get());
            CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
            logViolations("allowedModes:merge_preserve", report);
            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check =
                    new AcHandling().newInstance(obj().key("allowedModes", arr("ignore")).get());
            CheckReport report = scanWithCheck(check, "test_childnodeorder.zip");
            logViolations("allowedModes:ignore", report);
            Assert.assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testEmptyConfig() throws Exception {
        AcHandling.Check check = (AcHandling.Check) new AcHandling().newInstance(obj().get());
        assertNotNull("check should not be null", check);
        Assert.assertEquals("default levelSet should be " + AcHandling.DEFAULT_LEVEL_SET,
                check.levelSet, AcHandling.DEFAULT_LEVEL_SET);
        assertTrue("default allowedModes should be empty: " + check.allowedModes,
                check.allowedModes.isEmpty());
    }

    @Test
    public void testNullLevelSet() throws Exception {
        AcHandling.Check check = new AcHandling.Check(null, null);
        // expect no failure
        check.beforeExtract(null, null, null, null, null);
    }
}
