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
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.testing.TestPackageUtil;
import net.adamcin.oakpal.testing.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JcrPropertiesTest extends ProgressCheckTestBase {

    @Test
    public void testDenyIfAbsent() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_man").key("denyIfAbsent", false)))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            logViolations("double_man, no scope", report);
            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_man").key("denyIfAbsent", true)))
                    .key("scopePaths", arr()
                            .and(key("pattern", "/tmp/jcr:content").key("type", "allow")))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("one violation", 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_man").key("denyIfAbsent", false)))
                    .key("scopePaths", arr()
                            .and(key("pattern", "/tmp/jcr:content").key("type", "allow")))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            logViolations("double_man, with scope", report);
            Assert.assertEquals("no violations", 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testDenyIfPresent() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("denyIfPresent", false)))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            logViolations("double_nan, no scope", report);
            Assert.assertEquals("no violations: " + report.getViolations(), 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("denyIfPresent", true)))
                    .key("scopePaths", arr()
                            .and(key("pattern", "/tmp/jcr:content").key("type", "allow")))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("one violation: " + report.getViolations(), 1, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("denyIfPresent", false)))
                    .key("scopePaths", arr()
                            .and(key("pattern", "/tmp/jcr:content").key("type", "allow")))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            logViolations("double_nan, with scope", report);
            Assert.assertEquals("no violations: " + report.getViolations(), 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testRequireType() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("requireType", "Double"))
                            .and(key("name", "double_neg_inf").key("requireType", "Double"))
                    )
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("no violations: " + report.getViolations(), 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("requireType", "String"))
                            .and(key("name", "double_neg_inf").key("requireType", "String"))
                    )
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("two violations: " + report.getViolations(), 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testDenyNodeTypes() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("denyNodeTypes", arr("sling:Folder"))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("no violations: " + report.getViolations(), 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("denyNodeTypes", arr("nt:unstructured"))
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("two violations: " + report.getViolations(), 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testScopeNodeTypes() throws Exception {
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("requireType", "String"))
                            .and(key("name", "double_neg_inf").key("requireType", "String"))
                    )
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("two violations: " + report.getViolations(), 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("scopeNodeTypes", arr("nt:unstructured"))
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("requireType", "String"))
                            .and(key("name", "double_neg_inf").key("requireType", "String"))
                    )
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("two violations: " + report.getViolations(), 2, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("scopeNodeTypes", arr("sling:Folder"))
                    .key("properties", arr()
                            .and(key("name", "double_nan").key("requireType", "String"))
                            .and(key("name", "double_neg_inf").key("requireType", "String"))
                    )
                    .get());
            CheckReport report = scanWithCheck(check, "double_properties.zip");
            Assert.assertEquals("no violations: " + report.getViolations(), 0, report.getViolations().size());
            assertTrue("all violations have packageIds", report.getViolations().stream()
                    .allMatch(viol -> !viol.getPackages().isEmpty()));
        });
    }

    @Test
    public void testPropertyValueDeny() throws Exception {
        File playground = TestPackageUtil.prepareTestPackageFromFolder("playground.zip",
                new File("src/test/resources/jcr_prop_constraints_playground"));
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    // intend to only check /apps/acme by ensuring scopePaths excludes /apps/aperture, which has the
                    // same jcr:title value
                    .key("scopePaths", arr(new Rule(RuleType.INCLUDE, Pattern.compile("/apps/acme"))))
                    .key("properties", arr()
                            .val(obj()
                                    .key("name", "jcr:title")
                                    .key("valueRules", arr(new Rule(RuleType.DENY, Pattern.compile("Acme"))))
                            )
                    )
                    .get());
            CheckReport report = scanWithCheck(check, playground);
            Assert.assertEquals("one violation: " + report.getViolations(), 1, report.getViolations().size());
        });
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .val(obj()
                                    .key("name", "multiString")
                                    .key("valueRules", arr(new Rule(RuleType.DENY, Pattern.compile("two"))))
                            )
                    )
                    .get());
            CheckReport report = scanWithCheck(check, playground);
            Assert.assertEquals("two violation: " + report.getViolations(), 2, report.getViolations().size());
        });
    }

    @Test
    public void testDenyIfMultivalued() throws Exception {
        File playground = TestPackageUtil.prepareTestPackageFromFolder("playground.zip",
                new File("src/test/resources/jcr_prop_constraints_playground"));
        TestUtil.testBlock(() -> {
            ProgressCheck check = new JcrProperties().newInstance(obj()
                    .key("properties", arr()
                            .val(obj()
                                    .key("name", "singleString")
                                    .key("denyIfMultivalued", true)
                            )
                    )
                    .get());
            CheckReport report = scanWithCheck(check, playground);
            // only /apps/acme has the incorrect singleString property
            Assert.assertEquals("one violation: " + report.getViolations(), 1, report.getViolations().size());
        });
    }

    @Test
    public void testSetResourceBundle() {
        final JcrProperties.ResourceBundleHolder resourceBundleHolder =
                new JcrProperties.ResourceBundleHolder();
        JcrPropertyConstraints constraints = new JcrPropertyConstraints("prop", false,
                false, false, "nt:base", emptyList(), Severity.MAJOR,
                resourceBundleHolder::getResourceBundle);
        JcrProperties.Check check = new JcrProperties.Check(emptyList(), emptyList(), emptyList(),
                Collections.singletonList(constraints), resourceBundleHolder);

        final ResourceBundle fromHolder = resourceBundleHolder.getResourceBundle();
        final ResourceBundle fromCheck = check.getResourceBundle();
        assertSame("expect same resource bundle due to internal caching", fromHolder, fromCheck);

        final ResourceBundle toSetForTest = ResourceBundle.getBundle(getClass().getName());
        check.setResourceBundle(toSetForTest);
        final ResourceBundle fromHolderAfterSet = resourceBundleHolder.getResourceBundle();
        final ResourceBundle fromCheckAfterSet = check.getResourceBundle();
        assertSame("expect same resource bundle after set", fromHolderAfterSet, fromCheckAfterSet);

        final String milliString = String.valueOf(System.currentTimeMillis());
        assertSame("same key returned from constraints when not in bundle",
                milliString, constraints.getString(milliString));
    }
}
