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

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.RuleType;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.OakpalPlan;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.json.JsonObject;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
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
        Assert.assertEquals("expect name", ExpectPaths.class.getSimpleName(), emptyCheck.getCheckName());
        emptyCheck.afterExtract(PackageId.fromString("hey"), mock(Session.class));
        assertTrue("empty violations", emptyCheck.getReportedViolations().isEmpty());
    }

    @Test
    public void testNewInstance_expectedPaths() {
        ExpectPaths.Check check1 = checkFor(key(ExpectPaths.keys().expectedPaths(), arr("/foo1", "/foo2")).get());
        Assert.assertEquals("expect expectedPaths", Arrays.asList("/foo1", "/foo2"), check1.expectedPaths);
        ExpectPaths.Check check2 = checkFor(key(ExpectPaths.keys().expectedPaths(), arr("/foo2", "/foo1")).get());
        Assert.assertEquals("expect expectedPaths", Arrays.asList("/foo2", "/foo1"), check2.expectedPaths);
    }

    @Test
    public void testNewInstance_notExpectedPaths() {
        ExpectPaths.Check check1 = checkFor(key(ExpectPaths.keys().notExpectedPaths(), arr("/foo1", "/foo2")).get());
        Assert.assertEquals("expect notExpectedPaths", Arrays.asList("/foo1", "/foo2"), check1.notExpectedPaths);
        ExpectPaths.Check check2 = checkFor(key(ExpectPaths.keys().notExpectedPaths(), arr("/foo2", "/foo1")).get());
        Assert.assertEquals("expect notExpectedPaths", Arrays.asList("/foo2", "/foo1"), check2.notExpectedPaths);
    }

    @Test
    public void testNewInstance_afterPackageIdRules() {
        final List<Rule> expectedRules = Arrays.asList(
                new Rule(RuleType.INCLUDE, Pattern.compile("whua")),
                new Rule(RuleType.EXCLUDE, Pattern.compile("heyy")));

        ExpectPaths.Check check1 = checkFor(key(ExpectPaths.keys().afterPackageIdRules(), expectedRules).get());
        Assert.assertEquals("expect afterPackageIdRules", expectedRules, check1.afterPackageIdRules);
    }

    @Test
    public void testNewInstance_severity() {
        ExpectPaths.Check defaultMajorCheck = checkFor(obj()
                .key("expectedPaths", arr("/foo"))
                .get());
        Assert.assertEquals("expect major (default)", Severity.MAJOR, defaultMajorCheck.severity);
        ExpectPaths.Check minorCheck = checkFor(obj()
                .key("severity", "minor")
                .key("expectedPaths", arr("/foo"))
                .get());
        Assert.assertEquals("expect minor", Severity.MINOR, minorCheck.severity);
        ExpectPaths.Check majorCheck = checkFor(obj()
                .key("severity", "major")
                .key("expectedPaths", arr("/foo"))
                .get());
        Assert.assertEquals("expect major", Severity.MAJOR, majorCheck.severity);
        ExpectPaths.Check severeCheck = checkFor(obj()
                .key("severity", "severe")
                .key("expectedPaths", arr("/foo"))
                .get());
        Assert.assertEquals("expect severe", Severity.SEVERE, severeCheck.severity);
    }

    @Test
    public void testShouldExpectAfterExtract() {
        ExpectPaths.Check check1 = checkFor(obj().get());
        assertTrue("expect true for empty", check1.shouldExpectAfterExtract(PackageId.fromString("foo")));

        ExpectPaths.Check check2 = checkFor(key(ExpectPaths.keys().afterPackageIdRules(), arr()
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
        ExpectPaths.Check check = checkFor(key(ExpectPaths.keys().expectedPaths(), arr("/foo")).get());
        final PackageId pid = PackageId.fromString("foo");
        final Session session = mock(Session.class);
        check.afterExtract(pid, session);
        check.startedScan();
        check.afterExtract(pid, session);
        check.afterExtract(pid, session);
        check.finishedScan();
        Collection<Violation> violations = check.getReportedViolations();
        assertEquals("expect one violation", 1, violations.stream().count());
        assertEquals("expect one packageId", 1, violations.stream().findFirst().get().getPackages().size());
    }

    @Test
    public void testPackageGraph() {
        final PackageId container1 = PackageId.fromString("container1");
        final PackageId cont1sub1 = PackageId.fromString("cont1sub1");
        final PackageId cont1emb1 = PackageId.fromString("cont1emb1");
        final EmbeddedPackageInstallable installable1 = new EmbeddedPackageInstallable(container1, "", cont1emb1);
        final PackageId container2 = PackageId.fromString("container2");
        final PackageId cont2sub1 = PackageId.fromString("cont2sub1");
        final PackageId cont2emb1 = PackageId.fromString("cont2emb1");
        final EmbeddedPackageInstallable installable2 = new EmbeddedPackageInstallable(container2, "", cont2emb1);
        final Set<PackageId> scan1 = Stream.of(container1, cont1sub1, cont1emb1).collect(Collectors.toSet());
        final Set<PackageId> scan2 = Stream.of(container2, cont2sub1, cont2emb1).collect(Collectors.toSet());

        ExpectPaths.Check check = checkFor(obj().get());
        PackageGraph graph = check.getGraph();
        for (PackageId id : scan1) {
            assertFalse("expect not identified " + id, graph.isIdentified(id));
        }
        for (PackageId id : scan2) {
            assertFalse("expect not identified " + id, graph.isIdentified(id));
        }
        check.identifyPackage(container1, null);
        assertTrue("expected identified " + container1, graph.isIdentified(container1));
        assertTrue("expect root " + container1, graph.isRoot(container1));
        assertFalse("expect not identified " + cont1sub1, graph.isIdentified(cont1sub1));
        assertTrue("expect root " + cont1sub1, graph.isRoot(cont1sub1));
        check.identifySubpackage(cont1sub1, container1);
        assertTrue("expected identified " + cont1sub1, graph.isIdentified(cont1sub1));
        assertTrue("expect root " + container1, graph.isRoot(container1));
        assertFalse("expect not root " + cont1sub1, graph.isRoot(cont1sub1));
        assertFalse("expect not identified " + cont1emb1, graph.isIdentified(cont1emb1));
        check.identifyEmbeddedPackage(cont1emb1, container1, installable1);
        assertTrue("expected identified " + cont1emb1, graph.isIdentified(cont1emb1));
        assertTrue("expect root " + container1, graph.isRoot(container1));
        assertFalse("expect not root " + cont1sub1, graph.isRoot(cont1sub1));
        assertFalse("expect not root " + cont1emb1, graph.isRoot(cont1sub1));
        assertEquals("expect equal to scan", scan1, new HashSet<>(graph.getSelfAndDescendants(container1)));

        for (PackageId id : scan2) {
            assertFalse("expect not identified " + id, graph.isIdentified(id));
        }
        check.identifyPackage(container2, null);
        assertTrue("expected identified " + container2, graph.isIdentified(container2));
        assertTrue("expect root " + container2, graph.isRoot(container2));
        assertFalse("expect not identified " + cont2sub1, graph.isIdentified(cont2sub1));
        assertTrue("expect root " + cont2sub1, graph.isRoot(cont2sub1));
        check.identifySubpackage(cont2sub1, container2);
        assertTrue("expected identified " + cont2sub1, graph.isIdentified(cont2sub1));
        assertTrue("expect root " + container2, graph.isRoot(container2));
        assertFalse("expect not root " + cont2sub1, graph.isRoot(cont2sub1));
        assertFalse("expect not identified " + cont2emb1, graph.isIdentified(cont2emb1));
        check.identifyEmbeddedPackage(cont2emb1, container2, installable2);
        assertTrue("expected identified " + cont2emb1, graph.isIdentified(cont2emb1));
        assertTrue("expect root " + container2, graph.isRoot(container2));
        assertFalse("expect not root " + cont2sub1, graph.isRoot(cont2sub1));
        assertFalse("expect not root " + cont2emb1, graph.isRoot(cont2sub1));

        assertEquals("expect equal to scan", scan1, new HashSet<>(graph.getSelfAndDescendants(container1)));
        assertEquals("expect equal to scan", scan2, new HashSet<>(graph.getSelfAndDescendants(container2)));
    }

    @Test
    public void testSuppressAfterExtractViolationIfExpectationSatisfiedAfterScanPackage() throws Exception {
        final String foo1 = "/foo1";
        final String foo2 = "/foo2";
        final PackageId someId = PackageId.fromString("some");
        final PackageId someEmbedId = PackageId.fromString("other");
        final EmbeddedPackageInstallable installable = new EmbeddedPackageInstallable(someId, "", someEmbedId);

        final ExpectPaths.Check check1 = checkFor(obj()
                .key("expectedPaths", arr(foo1, foo2))
                .key("ignoreNestedPackages", true)
                .get());
        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check1.startedScan();
                    check1.afterExtract(someId, session);
                    check1.beforeSlingInstall(someId, installable, session);
                    check1.identifyEmbeddedPackage(someEmbedId, someId, installable);
                    check1.afterExtract(someEmbedId, session);
                    check1.afterScanPackage(someId, session);
                    check1.finishedScan();
                });

        assertEquals("expect 1 expected violation", 1, check1.getReportedViolations().stream().count());
        final Violation violation1 = check1.getReportedViolations().stream().findFirst().get();
        assertTrue("starts with expected: " + violation1.getDescription(), violation1.getDescription().startsWith("expected"));
        assertTrue("ends with /foo2: " + violation1.getDescription(), violation1.getDescription().endsWith(foo2));

        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check1.startedScan();
                    check1.afterExtract(someId, session);
                    check1.beforeSlingInstall(someId, installable, session);
                    check1.identifyEmbeddedPackage(someEmbedId, someId, installable);
                    JcrUtils.getOrCreateByPath(foo2, "nt:folder", session);
                    check1.afterExtract(someEmbedId, session);
                    check1.afterScanPackage(someId, session);
                    check1.finishedScan();
                });

        assertEquals("expect 0 expected violations", 0,
                check1.getReportedViolations().stream().count());

        final ExpectPaths.Check check2 = checkFor(obj()
                .key("notExpectedPaths", arr(foo1, foo2))
                .key("ignoreNestedPackages", true)
                .get());
        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check2.startedScan();
                    check2.afterExtract(someId, session);
                    check2.beforeSlingInstall(someId, installable, session);
                    check2.identifyEmbeddedPackage(someEmbedId, someId, installable);
                    check2.afterExtract(someEmbedId, session);
                    check2.afterScanPackage(someId, session);
                    check2.finishedScan();
                });

        assertEquals("expect 1 unexpected violation", 1, check2.getReportedViolations().stream().count());
        final Violation violation2 = check2.getReportedViolations().stream().findFirst().get();
        assertTrue("starts with unexpected: " + violation2.getDescription(), violation2.getDescription().startsWith("unexpected"));
        assertTrue("ends with /foo1: " + violation2.getDescription(), violation2.getDescription().endsWith(foo1));

        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check2.startedScan();
                    check2.afterExtract(someId, session);
                    check2.beforeSlingInstall(someId, installable, session);
                    check2.identifyEmbeddedPackage(someEmbedId, someId, installable);
                    check2.afterExtract(someEmbedId, session);
                    session.getNode(foo1).remove();
                    check2.afterScanPackage(someId, session);
                    check2.finishedScan();
                });

        assertEquals("expect 0 unexpected violation", 0, check2.getReportedViolations().stream().count());
    }

    @Test
    public void testSuppressAfterExtractViolationIfExpectationSatisfiedAfterSlingInstall_multipleViolators()
            throws Exception {
        final String foo1 = "/foo1";
        final String foo2 = "/foo2";

        final PackageId container = PackageId.fromString("container");
        final PackageId embed1 = PackageId.fromString("embed1");
        SlingInstallable installable1 = mock(SlingInstallable.class);
        when(installable1.getParentId()).thenReturn(embed1);
        EmbeddedPackageInstallable epInstallable1 = new EmbeddedPackageInstallable(container, "", embed1);
        final PackageId embed2 = PackageId.fromString("embed2");
        SlingInstallable installable2 = mock(SlingInstallable.class);
        when(installable2.getParentId()).thenReturn(embed2);
        EmbeddedPackageInstallable epInstallable2 = new EmbeddedPackageInstallable(container, "", embed2);
        final PackageId embed3 = PackageId.fromString("embed3");
        SlingInstallable installable3 = mock(SlingInstallable.class);
        when(installable3.getParentId()).thenReturn(embed3);
        EmbeddedPackageInstallable epInstallable3 = new EmbeddedPackageInstallable(container, "", embed3);

        final ExpectPaths.Check check1 = checkFor(obj()
                .key("expectedPaths", arr(foo1, foo2))
                .key(ExpectPaths.keys().afterPackageIdRules(), arr()
                        .val(new Rule(RuleType.EXCLUDE, Pattern.compile(":container"))))
                .get());
        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check1.startedScan();
                    check1.identifyPackage(container, null);
                    check1.afterExtract(container, session);
                    check1.beforeSlingInstall(container, epInstallable1, session);
                    check1.identifyEmbeddedPackage(embed1, container, epInstallable1);
                    check1.afterExtract(embed1, session);
                    check1.beforeSlingInstall(container, epInstallable2, session);
                    check1.identifyEmbeddedPackage(embed2, container, epInstallable2);
                    check1.afterExtract(embed2, session);
                    check1.beforeSlingInstall(container, epInstallable3, session);
                    check1.identifyEmbeddedPackage(embed3, container, epInstallable3);
                    check1.afterExtract(embed3, session);
                    check1.beforeSlingInstall(container, installable1, session);
                    check1.beforeSlingInstall(container, installable2, session);
                    check1.beforeSlingInstall(container, installable3, session);
                    check1.afterScanPackage(container, session);
                    check1.finishedScan();
                });

        assertEquals("expect 1 expected violation", 1, check1.getReportedViolations().stream().count());
        final Violation exViolation1 = check1.getReportedViolations().stream().findFirst().get();
        assertTrue("starts with expected: " + exViolation1.getDescription(), exViolation1.getDescription().startsWith("expected"));
        assertTrue("ends with /foo2: " + exViolation1.getDescription(), exViolation1.getDescription().endsWith(foo2));
        assertEquals("expected violators",
                Stream.of(embed1, container).collect(Collectors.toSet()),
                new HashSet<>(exViolation1.getPackages()));

        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check1.startedScan();
                    check1.identifyPackage(container, null);
                    check1.afterExtract(container, session);
                    check1.beforeSlingInstall(container, epInstallable1, session);
                    check1.identifyEmbeddedPackage(embed1, container, epInstallable1);
                    JcrUtils.getOrCreateByPath(foo2, "nt:folder", session);
                    check1.afterExtract(embed1, session);
                    check1.beforeSlingInstall(container, epInstallable2, session);
                    check1.identifyEmbeddedPackage(embed2, container, epInstallable2);
                    check1.afterExtract(embed2, session);
                    check1.beforeSlingInstall(container, epInstallable3, session);
                    check1.identifyEmbeddedPackage(embed3, container, epInstallable3);
                    session.getNode(foo2).remove();
                    check1.afterExtract(embed3, session);
                    check1.beforeSlingInstall(container, installable1, session);
                    check1.beforeSlingInstall(container, installable2, session);
                    check1.beforeSlingInstall(container, installable3, session);
                    check1.afterScanPackage(container, session);
                    check1.finishedScan();
                });

        assertEquals("expect 1 expected violation", 1, check1.getReportedViolations().stream().count());
        final Violation exViolation2 = check1.getReportedViolations().stream().findFirst().get();
        assertTrue("starts with expected: " + exViolation2.getDescription(), exViolation2.getDescription().startsWith("expected"));
        assertTrue("ends with /foo2: " + exViolation2.getDescription(), exViolation2.getDescription().endsWith(foo2));
        assertEquals("expected violators",
                Stream.of(container, embed3).collect(Collectors.toSet()),
                new HashSet<>(exViolation2.getPackages()));

        final ExpectPaths.Check check2 = checkFor(obj()
                .key("notExpectedPaths", arr(foo1, foo2))
                //.key(ExpectPaths.keys().afterPackageIdRules(), arr()
                //        .val(new Rule(RuleType.EXCLUDE, Pattern.compile(":container"))))
                .get());
        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check2.startedScan();
                    check2.identifyPackage(container, null);
                    check2.afterExtract(container, session);
                    check2.beforeSlingInstall(embed1, epInstallable1, session);
                    check2.identifyEmbeddedPackage(embed1, container, epInstallable1);
                    check2.afterExtract(embed1, session);
                    check2.beforeSlingInstall(embed2, epInstallable2, session);
                    check2.identifyEmbeddedPackage(embed2, container, epInstallable2);
                    check2.afterExtract(embed2, session);
                    check2.beforeSlingInstall(embed3, epInstallable3, session);
                    check2.identifyEmbeddedPackage(embed3, container, epInstallable3);
                    check2.afterExtract(embed3, session);
                    check2.beforeSlingInstall(container, installable1, session);
                    check2.beforeSlingInstall(container, installable2, session);
                    check2.beforeSlingInstall(container, installable3, session);
                    check2.afterScanPackage(container, session);
                    check2.finishedScan();
                });

        assertEquals("expect 1 unexpected violation", 1, check2.getReportedViolations().stream().count());
        final Violation unViolation1 = check2.getReportedViolations().stream().findFirst().get();
        assertTrue("starts with unexpected: " + unViolation1.getDescription(), unViolation1.getDescription().startsWith("unexpected"));
        assertTrue("ends with /foo1: " + unViolation1.getDescription(), unViolation1.getDescription().endsWith(foo1));
        assertEquals("expected violators",
                Stream.of(container).collect(Collectors.toSet()),
                new HashSet<>(unViolation1.getPackages()));

        new OakpalPlan.Builder(null, null)
                .withRepoInits(Collections.singletonList(String.format("create path %s", foo1)))
                .build().toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .adminInitAndInspect(session -> {
                    check2.startedScan();
                    check2.identifyPackage(container, null);
                    session.getNode(foo1).remove();
                    check2.afterExtract(container, session);
                    check2.beforeSlingInstall(embed1, epInstallable1, session);
                    check2.identifyEmbeddedPackage(embed1, container, epInstallable1);
                    JcrUtils.getOrCreateByPath(foo1, "nt:folder", session);
                    check2.afterExtract(embed1, session);
                    check2.beforeSlingInstall(embed2, epInstallable2, session);
                    check2.identifyEmbeddedPackage(embed2, container, epInstallable2);
                    check2.afterExtract(embed2, session);
                    check2.beforeSlingInstall(embed3, epInstallable3, session);
                    check2.identifyEmbeddedPackage(embed3, container, epInstallable3);
                    session.getNode(foo1).remove();
                    check2.afterExtract(embed3, session);
                    check2.beforeSlingInstall(container, installable1, session);
                    check2.beforeSlingInstall(container, installable2, session);
                    JcrUtils.getOrCreateByPath(foo2, "nt:folder", session);
                    check2.appliedRepoInitScripts(container, Collections.emptyList(), installable2, session);
                    check2.beforeSlingInstall(container, installable3, session);
                    check2.afterScanPackage(container, session);
                    check2.finishedScan();
                });
        assertEquals("expect 2 unexpected violations", 2, check2.getReportedViolations().size());
        final Collection<Violation> unViolations = check2.getReportedViolations();
        assertTrue("all start with unexpected " + unViolations, unViolations.stream()
                .allMatch(vio -> vio.getDescription().startsWith("unexpected")));
        assertEquals("one ends with /foo1: " + unViolations, 1, unViolations.stream()
                .filter(vio -> vio.getDescription().endsWith("/foo1")).count());
        assertEquals("one ends with /foo2: " + unViolations, 1, unViolations.stream()
                .filter(vio -> vio.getDescription().endsWith("/foo2")).count());
        assertEquals("expected violators of /foo1",
                Stream.of(embed1).collect(Collectors.toSet()), unViolations.stream()
                        .filter(vio -> vio.getDescription().endsWith("/foo1"))
                        .flatMap(Fun.compose1(Violation::getPackages, Collection::stream))
                        .collect(Collectors.toSet()));
        assertEquals("expected violators of /foo2",
                Stream.of(embed2, container).collect(Collectors.toSet()), unViolations.stream()
                        .filter(vio -> vio.getDescription().endsWith("/foo2"))
                        .flatMap(Fun.compose1(Violation::getPackages, Collection::stream))
                        .collect(Collectors.toSet()));

    }

}