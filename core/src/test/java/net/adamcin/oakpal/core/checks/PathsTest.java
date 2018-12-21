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

import java.util.regex.Pattern;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.TestUtil;
import net.adamcin.oakpal.core.Violation;
import org.junit.Test;

public class PathsTest extends ProgressCheckTestBase {

    private static final Rule denyEtc = new Rule(Rule.RuleType.DENY, Pattern.compile("/etc(/.*)?"));

    @Test
    public void testDefaultSeverity() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Paths().newInstance(key("rules", arr(denyEtc)).get());
            CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
            logViolations("level_set:no_unsafe", report);
            assertEquals("violations", 6, report.getViolations().size());
            assertTrue("all violations are MAJOR", report.getViolations().stream()
                    .allMatch(viol -> viol.getSeverity().equals(Violation.Severity.MAJOR)));
        });
    }

    @Test
    public void testCustomSeverity() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Paths().newInstance(key("severity", "SEVERE").key("rules", arr(denyEtc)).get());
            CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
            logViolations("level_set:no_unsafe", report);
            assertEquals("violations", 6, report.getViolations().size());
            assertTrue("all violations are SEVERE", report.getViolations().stream()
                    .allMatch(viol -> viol.getSeverity().equals(Violation.Severity.SEVERE)));
        });
    }

    @Test
    public void testCustomSeverityMixedCase() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Paths().newInstance(key("severity", "minor").key("rules", arr(denyEtc)).get());
            CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
            logViolations("level_set:no_unsafe", report);
            assertEquals("violations", 6, report.getViolations().size());
            assertTrue("all violations are MINOR", report.getViolations().stream()
                    .allMatch(viol -> viol.getSeverity().equals(Violation.Severity.MINOR)));
        });
    }

    @Test
    public void testBadSeverity() throws Exception {
        TestUtil.testBlock(() -> {
            boolean threw = false;
            try {
                ProgressCheck check = new Paths().newInstance(key("severity", "whoops").key("rules", arr(denyEtc)).get());
            } catch (IllegalArgumentException e) {
                threw = true;
            }
            assertTrue("bad severity should cause exception on construction.", threw);
        });
    }
}
