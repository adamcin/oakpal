/*
 * Copyright 2019 Mark Adamcin
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

import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpectPathsTest {

    static ExpectPaths.Check checkFor(final JsonObject config) {
        return (ExpectPaths.Check) new ExpectPaths().newInstance(config);
    }

    @Test
    public void testNewInstance_empty() throws Exception {
        ExpectPaths.Check emptyCheck = checkFor(obj().get());
        assertTrue("empty expectedPaths", emptyCheck.expectedPaths.isEmpty());
        assertTrue("empty notExpectedPaths", emptyCheck.notExpectedPaths.isEmpty());
        assertTrue("empty afterPackageIdRules", emptyCheck.afterPackageIdRules.isEmpty());
        assertEquals("expect name", ExpectPaths.class.getSimpleName(), emptyCheck.getCheckName());
        emptyCheck.afterExtract(PackageId.fromString("hey"), mock(Session.class));
        assertTrue("empty violations", emptyCheck.getReportedViolations().isEmpty());
    }

    @Test
    public void testNewInstance_expectedPaths() {
        ExpectPaths.Check check1 = checkFor(key(ExpectPaths.CONFIG_EXPECTED_PATHS, arr("/foo1", "/foo2")).get());
        assertEquals("expect expectedPaths", Arrays.asList("/foo1", "/foo2"), check1.expectedPaths);
        ExpectPaths.Check check2 = checkFor(key(ExpectPaths.CONFIG_EXPECTED_PATHS, arr("/foo2", "/foo1")).get());
        assertEquals("expect expectedPaths", Arrays.asList("/foo2", "/foo1"), check2.expectedPaths);
    }

    @Test
    public void testNewInstance_notExpectedPaths() {
        ExpectPaths.Check check1 = checkFor(key(ExpectPaths.CONFIG_NOT_EXPECTED_PATHS, arr("/foo1", "/foo2")).get());
        assertEquals("expect notExpectedPaths", Arrays.asList("/foo1", "/foo2"), check1.notExpectedPaths);
        ExpectPaths.Check check2 = checkFor(key(ExpectPaths.CONFIG_NOT_EXPECTED_PATHS, arr("/foo2", "/foo1")).get());
        assertEquals("expect notExpectedPaths", Arrays.asList("/foo2", "/foo1"), check2.notExpectedPaths);
    }

    @Test
    public void testNewInstance_afterPackageIdRules() {
        final List<Rule> expectedRules = Arrays.asList(
                new Rule(Rule.RuleType.INCLUDE, Pattern.compile("whua")),
                new Rule(Rule.RuleType.EXCLUDE, Pattern.compile("heyy")));

        ExpectPaths.Check check1 = checkFor(key(ExpectPaths.CONFIG_AFTER_PACKAGE_ID_RULES, expectedRules).get());
        assertEquals("expect afterPackageIdRules", expectedRules, check1.afterPackageIdRules);
    }

    @Test
    public void testShouldExpectAfterExtract() {
        ExpectPaths.Check check1 = checkFor(obj().get());
        assertTrue("expect true for empty", check1.shouldExpectAfterExtract(PackageId.fromString("foo")));

        ExpectPaths.Check check2 = checkFor(key(ExpectPaths.CONFIG_AFTER_PACKAGE_ID_RULES, arr()
                .val(key("type", "include").key("pattern", "^my_packages:.*"))).get());
        assertFalse("expect false", check2.shouldExpectAfterExtract(PackageId.fromString("adamcin:test:1.0")));
        assertTrue("expect true", check2.shouldExpectAfterExtract(PackageId.fromString("my_packages:test:1.0")));
    }

    @Test
    public void testAfterExtract() throws Exception {
        final String foo1 = "/foo1";
        final String foo2 = "/foo2";
        Session session = mock(Session.class);
        when(session.itemExists(foo1)).thenReturn(true);
        when(session.itemExists(foo2)).thenReturn(false);

        ExpectPaths.Check check1 = checkFor(key("expectedPaths", arr(foo1, foo2)).get());
        check1.afterExtract(PackageId.fromString("some"), session);
        check1.finishedScan();
        Collection<Violation> violations1 = check1.getReportedViolations();
        assertEquals("expect 1 violation", 1, violations1.stream().count());
        final Violation violation1 = violations1.stream().findFirst().get();
        assertTrue("starts with expected: " + violation1.getDescription(), violation1.getDescription().startsWith("expected"));
        assertTrue("ends with /foo2: " + violation1.getDescription(), violation1.getDescription().endsWith(foo2));

        ExpectPaths.Check check2 = checkFor(key("notExpectedPaths", arr(foo1, foo2)).get());
        check2.afterExtract(PackageId.fromString("some"), session);
        check2.finishedScan();
        Collection<Violation> violations2 = check2.getReportedViolations();
        assertEquals("expect 1 violation", 1, violations2.stream().count());
        final Violation violation2 = violations2.stream().findFirst().get();
        assertTrue("starts with unexpected: " + violation2.getDescription(), violation2.getDescription().startsWith("unexpected"));
        assertTrue("ends with /foo1: " + violation2.getDescription(), violation2.getDescription().endsWith(foo1));
    }

    @Test
    public void testStartedScan() throws Exception {
        ExpectPaths.Check check = checkFor(key(ExpectPaths.CONFIG_EXPECTED_PATHS, arr("/foo")).get());
        final PackageId pid = PackageId.fromString("foo");
        final Session session = mock(Session.class);
        check.afterExtract(pid, session);
        check.startedScan();
        check.afterExtract(pid, session);
        check.afterExtract(pid, session);
        check.finishedScan();
        Collection<Violation> violations = check.getReportedViolations();
        assertEquals("expect one violation", 1, violations.stream().count());
        assertEquals("expect two packageIds (same tho)", 2, violations.stream().findFirst().get().getPackages().size());
    }
}