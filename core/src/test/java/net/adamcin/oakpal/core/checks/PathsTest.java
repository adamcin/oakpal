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
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.RuleType;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.testing.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathsTest extends ProgressCheckTestBase {

    private static final Rule denyEtc = new Rule(RuleType.DENY, Pattern.compile("/etc(/.*)?"));

    @Test
    public void testDefaultSeverity() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Paths().newInstance(key("rules", arr(denyEtc)).get());
            CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
            logViolations("level_set:no_unsafe", report);
            Assert.assertEquals("violations", 6, report.getViolations().size());
            assertTrue("all violations are MAJOR", report.getViolations().stream()
                    .allMatch(viol -> viol.getSeverity().equals(Severity.MAJOR)));
        });
    }

    @Test
    public void testCustomSeverity() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Paths().newInstance(key("severity", "SEVERE").key("rules", arr(denyEtc)).get());
            CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
            logViolations("level_set:no_unsafe", report);
            Assert.assertEquals("violations", 6, report.getViolations().size());
            assertTrue("all violations are SEVERE", report.getViolations().stream()
                    .allMatch(viol -> viol.getSeverity().equals(Severity.SEVERE)));
        });
    }

    @Test
    public void testCustomSeverityMixedCase() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new Paths().newInstance(key("severity", "minor").key("rules", arr(denyEtc)).get());
            CheckReport report = scanWithCheck(check, "test-package-with-etc.zip");
            logViolations("level_set:no_unsafe", report);
            Assert.assertEquals("violations", 6, report.getViolations().size());
            assertTrue("all violations are MINOR", report.getViolations().stream()
                    .allMatch(viol -> viol.getSeverity().equals(Severity.MINOR)));
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

    @Test
    public void testDeletedPath() throws Exception {
        Paths.Check allDeletesCheck = new Paths.Check(Collections.emptyList(), true, Paths.DEFAULT_SEVERITY);
        assertTrue("reported violations should be empty before deletedPath",
                allDeletesCheck.getReportedViolations().isEmpty());
        allDeletesCheck.deletedPath(null, "/foo", null);
        assertFalse("reported violations should not be empty after deletedPath",
                allDeletesCheck.getReportedViolations().isEmpty());
        Paths.Check allDeletesCheckByConfig = (Paths.Check) new Paths()
                .newInstance(key(Paths.keys().denyAllDeletes(), true).get());
        assertTrue("reported violations should be empty before deletedPath",
                allDeletesCheckByConfig.getReportedViolations().isEmpty());
        allDeletesCheckByConfig.deletedPath(null, "/foo", null);
        assertFalse("reported violations should not be empty after deletedPath",
                allDeletesCheckByConfig.getReportedViolations().isEmpty());

        Paths.Check deletesByRuleCheck = new Paths.Check(
                singletonList(new Rule(RuleType.ALLOW, Pattern.compile("/foo"))),
                false, Paths.DEFAULT_SEVERITY);

        assertTrue("reported violations should be empty before deletedPath",
                deletesByRuleCheck.getReportedViolations().isEmpty());


        deletesByRuleCheck.deletedPath(null, "/foo", null);

        assertTrue("reported violations should be empty after deletedPath for /foo: " +
                        deletesByRuleCheck.getReportedViolations(),
                deletesByRuleCheck.getReportedViolations().isEmpty());

        deletesByRuleCheck.deletedPath(null, "/bar", null);

        assertFalse("reported violations should not be empty after deletedPath for /bar",
                deletesByRuleCheck.getReportedViolations().isEmpty());
    }

    @Test
    public void testDeletedPath_de() throws Exception {
        Paths.Check allDeletesCheck = new Paths.Check(Collections.emptyList(), true, Paths.DEFAULT_SEVERITY);
        allDeletesCheck.setResourceBundle(ResourceBundle.getBundle(allDeletesCheck.getResourceBundleBaseName(), Locale.GERMAN));

        allDeletesCheck.deletedPath(null, "/foo", null);
        assertFalse("reported violations should not be empty after deletedPath",
                allDeletesCheck.getReportedViolations().isEmpty());
        Violation violation = allDeletesCheck.getReportedViolations().iterator().next();
        assertTrue("violation description should be in german: " + violation.getDescription(),
                violation.getDescription().contains("gelöschte"));

    }
}
