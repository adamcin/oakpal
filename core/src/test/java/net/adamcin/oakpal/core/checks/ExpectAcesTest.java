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

import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.json.JsonObject;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpectAcesTest {
    private final NamePathResolver resolver = new DefaultNamePathResolver(JsonCnd.BUILTIN_MAPPINGS);

    static ExpectAces.Check checkFor(final JsonObject config) throws Exception {
        return (ExpectAces.Check) new ExpectAces().newInstance(config);
    }

    @Test(expected = Exception.class)
    public void testNewInstance_missingPrincipal() throws Exception {
        checkFor(key("principal", "").key("expectedAces", arr("type=allow;path=/;privileges=jcr:read")).get());
    }

    @Test(expected = Exception.class)
    public void testNewInstance_missingPrincipals() throws Exception {
        checkFor(key("principal", "").key("principals", arr("", ""))
                .key("expectedAces", arr("type=allow;path=/;privileges=jcr:read")).get());
    }

    @Test(expected = Exception.class)
    public void testNewInstance_principalAceFail() throws Exception {
        checkFor(key("expectedAces", arr("principal=nouser;type=foobar;path=/;privileges=jcr:read")).get());
    }

    @Test
    public void testNewInstance_principalAce() throws Exception {
        checkFor(key("expectedAces", arr("principal=nouser;type=allow;path=/;privileges=jcr:read")).get());
    }

    @Test
    public void testNewInstance_multiPrincipal() throws Exception {
        ExpectAces.Check svCheck = checkFor(obj()
                .key("principal", " ").key("principals", arr(" ", "foo"))
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect 1 ace", 1, svCheck.expectedAces.size());
        ExpectAces.Check mvCheck = checkFor(obj()
                .key("principal", " ").key("principals", arr("bar", "foo"))
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect 2 ace", 2, mvCheck.expectedAces.size());
        ExpectAces.Check jpCheck = checkFor(obj()
                .key("principal", "coo").key("principals", arr("bar", "foo"))
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect 1 ace", 1, jpCheck.expectedAces.size());
    }

    @Test
    public void testNewInstance_severity() throws Exception {
        ExpectAces.Check defaultMajorCheck = checkFor(obj()
                .key("principal", "foo")
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect major (default)", Violation.Severity.MAJOR, defaultMajorCheck.severity);
        ExpectAces.Check minorCheck = checkFor(obj()
                .key("principal", "foo").key("severity", "minor")
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect minor", Violation.Severity.MINOR, minorCheck.severity);
        ExpectAces.Check majorCheck = checkFor(obj()
                .key("principal", "foo").key("severity", "major")
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect major", Violation.Severity.MAJOR, majorCheck.severity);
        ExpectAces.Check severeCheck = checkFor(obj()
                .key("principal", "foo").key("severity", "severe")
                .key("expectedAces", arr("type=allow;path=/foo;privileges=jcr:read"))
                .get());
        assertEquals("expect severe", Violation.Severity.SEVERE, severeCheck.severity);
    }

    @Test
    public void testNewInstance_empty() throws Exception {
        ExpectAces.Check emptyCheck = checkFor(key("principal", "nouser").get());
        assertTrue("empty expectedAces", emptyCheck.expectedAces.isEmpty());
        assertTrue("empty notExpectedPaths", emptyCheck.notExpectedAces.isEmpty());
        assertTrue("empty afterPackageIdRules", emptyCheck.afterPackageIdRules.isEmpty());
        assertEquals("expect name", ExpectAces.class.getSimpleName(), emptyCheck.getCheckName());
        emptyCheck.afterExtract(PackageId.fromString("hey"), mock(Session.class));
        assertTrue("empty violations", emptyCheck.getReportedViolations().isEmpty());
    }

    @Test
    public void testNewInstance_afterPackageIdRules() throws Exception {
        final List<Rule> expectedRules = Arrays.asList(
                new Rule(Rule.RuleType.INCLUDE, Pattern.compile("whua")),
                new Rule(Rule.RuleType.EXCLUDE, Pattern.compile("heyy")));

        ExpectAces.Check check1 = checkFor(key("principal", "nouser")
                .key(ExpectPaths.CONFIG_AFTER_PACKAGE_ID_RULES, expectedRules).get());
        assertEquals("expect afterPackageIdRules", expectedRules, check1.afterPackageIdRules);
    }

    @Test
    public void testNewInstance_expectedAces() throws Exception {
        final String principal = "nouser";
        final String spec = "type=allow;path=/foo1;privileges=jcr:read";
        final List<ExpectAces.AceCriteria> expectCriterias = Collections.singletonList(
                ExpectAces.AceCriteria.parse(principal, spec).getOrDefault(null)
        );
        ExpectAces.Check check1 = checkFor(key("principal", principal)
                .key(ExpectAces.CONFIG_EXPECTED_ACES, arr(spec)).get());
        assertEquals("expect expectedAces", expectCriterias, check1.expectedAces);
    }

    @Test
    public void testNewInstance_notExpectedAces() throws Exception {
        final String principal = "nouser";
        final String spec = "type=allow;path=/foo1;privileges=jcr:read";
        final List<ExpectAces.AceCriteria> expectCriterias = Collections.singletonList(
                ExpectAces.AceCriteria.parse(principal, spec).getOrDefault(null)
        );
        ExpectAces.Check check1 = checkFor(key("principal", principal)
                .key(ExpectAces.CONFIG_NOT_EXPECTED_ACES, arr(spec)).get());
        assertEquals("expect notExpectedAces", expectCriterias, check1.notExpectedAces);
    }

    @Test(expected = Exception.class)
    public void testParseAceCriteria_throws() throws Exception {
        ExpectAces.parseAceCriteria(key("stuff", arr("type=allow")).get(), new String[]{"nouser"}, "stuff");
    }

    @Test
    public void testCheck_startedScan() throws Exception {
        ExpectAces.Check check = checkFor(obj()
                .key(ExpectAces.CONFIG_PRINCIPAL, "nouser")
                .key(ExpectAces.CONFIG_EXPECTED_ACES, arr()
                        .val("type=allow;path=/foo1;privileges=jcr:read")
                        .val("type=allow;path=/foo1;privileges=rep:write")
                )
                .key(ExpectAces.CONFIG_NOT_EXPECTED_ACES, arr()
                        .val("type=allow;path=/foo2;privileges=jcr:read")
                        .val("type=allow;path=/foo2;privileges=rep:write")
                        // foo3 is not created. a non-existent path should satisfy not-expected aces
                        .val("type=allow;path=/foo3;privileges=rep:write")
                )
                .get());

        final Principal principal = new PrincipalImpl("nouser");
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            final JackrabbitAccessControlManager accessControlManager =
                    (JackrabbitAccessControlManager) session.getAccessControlManager();
            final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            final Privilege jcrRead = privilegeManager.getPrivilege("jcr:read");

            final Node foo1 = session.getRootNode().addNode("foo1", resolver.getJCRName(NameConstants.NT_FOLDER));
            foo1.addMixin("rep:AccessControllable");
            final Node foo2 = session.getRootNode().addNode("foo2", resolver.getJCRName(NameConstants.NT_FOLDER));
            foo2.addMixin("rep:AccessControllable");
            session.save();

            for (String path : new String[]{"/foo1", "/foo2"}) {
                for (AccessControlPolicyIterator policyIt = accessControlManager.getApplicablePolicies(path); policyIt.hasNext(); ) {
                    AccessControlPolicy policy = policyIt.nextAccessControlPolicy();
                    if (policy instanceof JackrabbitAccessControlList) {
                        JackrabbitAccessControlList acl = (JackrabbitAccessControlList) policy;
                        acl.addEntry(principal, new Privilege[]{jcrRead}, true);
                        accessControlManager.setPolicy(path, acl);
                    }
                }
            }

            check.afterExtract(PackageId.fromString("foo"), session);
            check.startedScan();
            check.afterExtract(PackageId.fromString("foo"), session);
            check.afterExtract(PackageId.fromString("foo"), session);
            check.finishedScan();
        });

        assertEquals("expected violation count", 1, check.getReportedViolations().stream().filter(viol -> viol.getDescription().startsWith("expected: ")).count());
        assertEquals("expected violated spec ends with rep:write", 1,
                check.getReportedViolations().stream()
                        .filter(viol -> viol.getDescription().startsWith("expected: ")
                                && viol.getDescription().endsWith("rep:write")).count());
        assertEquals("unexpected violation count", 1, check.getReportedViolations().stream().filter(viol -> viol.getDescription().startsWith("unexpected: ")).count());
        assertEquals("unexpected violated spec ends with jcr:read", 1,
                check.getReportedViolations().stream()
                        .filter(viol -> viol.getDescription().startsWith("unexpected: ")
                                && viol.getDescription().endsWith("jcr:read")).count());
    }

    @Test
    public void testCheck_getViolatorListForExpectedCriteria() {
        final Map<ExpectAces.AceCriteria, List<PackageId>> violators = new HashMap<>();
        final ExpectAces.AceCriteria criteria = ExpectAces.AceCriteria.parse("nouser", "type=allow;path=;privileges=jcr:read").getOrDefault(null);
        PackageId pid = PackageId.fromString("foo");
        ExpectAces.Check.getViolatorListForExpectedCriteria(violators, criteria).add(pid);
        assertTrue("expect contains", ExpectAces.Check.getViolatorListForExpectedCriteria(violators, criteria).contains(pid));
    }

    @Test
    public void testShouldExpectAfterExtract() throws Exception {
        ExpectAces.Check check1 = checkFor(key("principal", "nouser").get());
        assertTrue("expect true for empty", check1.shouldExpectAfterExtract(PackageId.fromString("foo")));

        ExpectAces.Check check2 = checkFor(key("principal", "nouser")
                .key(ExpectAces.CONFIG_AFTER_PACKAGE_ID_RULES,
                        arr().val(key("type", "include").key("pattern", "^my_packages:.*"))).get());
        assertFalse("expect false", check2.shouldExpectAfterExtract(PackageId.fromString("adamcin:test:1.0")));
        assertTrue("expect true", check2.shouldExpectAfterExtract(PackageId.fromString("my_packages:test:1.0")));
    }

    @Test
    public void testAceCriteria_parse() throws Exception {
        Result<ExpectAces.AceCriteria> noPrincipalResult = ExpectAces.AceCriteria.parse("", "");
        assertTrue("expect error no principal starts with principal",
                noPrincipalResult.getError().map(Throwable::getMessage).orElse("").startsWith("principal "));

        Result<ExpectAces.AceCriteria> noTypeResult = ExpectAces.AceCriteria.parse("nouser", "");
        assertTrue("expect error no type starts with type",
                noTypeResult.getError().map(Throwable::getMessage).orElse("").startsWith("type "));

        Result<ExpectAces.AceCriteria> wrongTypeResult = ExpectAces.AceCriteria.parse("nouser", "type=restrict");
        assertTrue("expect error wrong type starts with specified type",
                wrongTypeResult.getError().map(Throwable::getMessage).orElse("").startsWith("restrict is not a valid"));

        Result<ExpectAces.AceCriteria> noPathResult = ExpectAces.AceCriteria.parse("nouser", "type=allow");
        assertTrue("expect error no path starts with path",
                noPathResult.getError().map(Throwable::getMessage).orElse("").startsWith("path "));

        Result<ExpectAces.AceCriteria> noPrivilegesResult = ExpectAces.AceCriteria.parse("nouser", "type=allow;path=/;privileges=");
        assertTrue("expect error no privileges starts with privileges",
                noPrivilegesResult.getError().map(Throwable::getMessage).orElse("").startsWith("privileges "));

        Result<ExpectAces.AceCriteria> emptyPathResult = ExpectAces.AceCriteria.parse("nouser", "type=allow;path=;privileges=jcr:read");
        assertTrue("expect success empty path has empty path",
                emptyPathResult.map(ExpectAces.AceCriteria::getPath).getOrDefault("failed").isEmpty());

        Result<ExpectAces.AceCriteria> noRestrictionsResult = ExpectAces.AceCriteria.parse("nouser", "type=allow;path=/foo;privileges=jcr:read");
        ExpectAces.AceCriteria noRestrictionsCriteria = noRestrictionsResult.getOrDefault(null);
        assertNotNull("expect success with no restrictions", noRestrictionsCriteria);
        assertEquals("expect path", "/foo", noRestrictionsCriteria.path);
        assertTrue("expect isAllow", noRestrictionsCriteria.isAllow);
        assertArrayEquals("expect privileges", new String[]{"jcr:read"}, noRestrictionsCriteria.privileges);
    }

    @Test
    public void testAceCriteria_getSpec() {
        ExpectAces.AceCriteria criteriaNoSpec = new ExpectAces.AceCriteria("nouser", false, "/foo",
                new String[]{"jcr:read", "jcr:all"},
                new ExpectAces.RestrictionCriteria[]{
                        new ExpectAces.RestrictionCriteria("rep:glob", "*"),
                        new ExpectAces.RestrictionCriteria("myRestriction", null)
                },
                null);
        assertEquals("expect spec", "type=deny;path=/foo;privileges=jcr:read,jcr:all;rep:glob=*;myRestriction",
                criteriaNoSpec.getSpec());
        assertEquals("expect toString", "principal:nouser ace:type=deny;path=/foo;privileges=jcr:read,jcr:all;rep:glob=*;myRestriction",
                criteriaNoSpec.toString());
        ExpectAces.AceCriteria criteriaWithSpec = new ExpectAces.AceCriteria("nouser", false, "/foo",
                new String[]{"jcr:read", "jcr:all"},
                new ExpectAces.RestrictionCriteria[]{
                        new ExpectAces.RestrictionCriteria("rep:glob", "*"),
                        new ExpectAces.RestrictionCriteria("myRestriction", null)
                },
                "whuheay!");
        assertEquals("expect whatever spec", "whuheay!",
                criteriaWithSpec.getSpec());
        assertEquals("expect whatever toString", "principal:nouser ace:whuheay!",
                criteriaWithSpec.toString());
    }

    static class MockPrivilege implements Privilege {
        private final String name;
        private final Privilege[] declaredAggregatePrivileges;

        MockPrivilege(String name, Privilege[] declaredAggregatePrivileges) {
            this.name = name;
            this.declaredAggregatePrivileges = declaredAggregatePrivileges;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isAbstract() {
            return false;
        }

        @Override
        public boolean isAggregate() {
            return declaredAggregatePrivileges.length > 0;
        }

        @Override
        public Privilege[] getDeclaredAggregatePrivileges() {
            return declaredAggregatePrivileges;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MockPrivilege that = (MockPrivilege) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public Privilege[] getAggregatePrivileges() {
            return Stream.of(declaredAggregatePrivileges)
                    .flatMap(priv -> Stream.concat(Stream.of(priv), Stream.of(priv.getAggregatePrivileges())))
                    .distinct().toArray(Privilege[]::new);
        }
    }

    static final Privilege[] emptyPrivileges = new Privilege[0];

    final Privilege privilegeByName(final @NotNull String name) {
        final String jcrName = compose(uncheck1(resolver::getQName), uncheck1(resolver::getJCRName)).apply(name);
        if (PrivilegeConstants.AGGREGATE_PRIVILEGES.containsKey(jcrName)) {
            return new MockPrivilege(jcrName, Stream.of(PrivilegeConstants.AGGREGATE_PRIVILEGES.get(jcrName))
                    .map(this::privilegeByName).toArray(Privilege[]::new));
        } else {
            return new MockPrivilege(jcrName, emptyPrivileges);
        }
    }

    @Test
    public void testAceCriteria_satisfiedBy() throws Exception {
        final JackrabbitAccessControlList emptyAcl = mock(JackrabbitAccessControlList.class);
        when(emptyAcl.getRestrictionNames()).thenReturn(new String[0]);
        when(emptyAcl.getAccessControlEntries()).thenReturn(new AccessControlEntry[0]);
        ExpectAces.AceCriteria criteriaNoPrivs = new ExpectAces.AceCriteria("nouser", false, "/foo", new String[0], new ExpectAces.RestrictionCriteria[0], null);
        assertTrue("expect satisfied by empty acl with empty privileges", criteriaNoPrivs.satisfiedBy(emptyAcl));

        final JackrabbitAccessControlList acl1 = mock(JackrabbitAccessControlList.class);
        when(acl1.getRestrictionNames()).thenReturn(new String[0]);
        final JackrabbitAccessControlEntry everyoneAce = mock(JackrabbitAccessControlEntry.class);
        when(everyoneAce.isAllow()).thenReturn(true);
        when(everyoneAce.getPrincipal()).thenReturn(new PrincipalImpl("everyone"));
        when(everyoneAce.getPrivileges()).thenReturn(new Privilege[]{privilegeByName("jcr:read")});
        when(acl1.getAccessControlEntries()).thenReturn(new AccessControlEntry[]{everyoneAce});
        ExpectAces.AceCriteria criteria1 = new ExpectAces.AceCriteria("everyone", true, "/foo", new String[]{"jcr:read"}, new ExpectAces.RestrictionCriteria[0], null);
        assertTrue("expect satisfied by everyone ace with jcr:read privilege", criteria1.satisfiedBy(acl1));
        final JackrabbitAccessControlList acl2 = mock(JackrabbitAccessControlList.class);
        final String repGlob = "rep:glob";
        final String appsConfig = "apps/*/config/*";
        when(acl2.getRestrictionNames()).thenReturn(new String[]{repGlob});
        final JackrabbitAccessControlEntry contributorAce = mock(JackrabbitAccessControlEntry.class);
        when(contributorAce.isAllow()).thenReturn(false);
        when(contributorAce.getPrincipal()).thenReturn(new PrincipalImpl("contributor"));
        when(contributorAce.getPrivileges()).thenReturn(new Privilege[]{privilegeByName("jcr:read")});
        when(contributorAce.getRestriction(repGlob)).thenReturn(ValueFactoryImpl.getInstance().createValue(appsConfig));
        when(acl2.getAccessControlEntries()).thenReturn(new AccessControlEntry[]{contributorAce});

        ExpectAces.AceCriteria criteria2apps = new ExpectAces.AceCriteria("contributor", false, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("rep:glob", "apps/*/config/*")}, null);
        ExpectAces.AceCriteria criteria2libs = new ExpectAces.AceCriteria("contributor", false, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("rep:glob", "libs/*/config/*")}, null);

        assertTrue("expect satisfied by apps ace with rep:glob restriction", criteria2apps.satisfiedBy(acl2));
        assertFalse("expect not satisfied by libs ace with rep:glob restriction", criteria2libs.satisfiedBy(acl2));

        final JackrabbitAccessControlList acl3 = mock(JackrabbitAccessControlList.class);
        final String repMvTags = "mvNames";
        final String name1 = "tag:football";
        final String name2 = "tag:pingpong";
        when(acl3.getRestrictionNames()).thenReturn(new String[]{repMvTags, repGlob});
        when(acl3.isMultiValueRestriction(repMvTags)).thenReturn(true);
        final JackrabbitAccessControlEntry editorAce = mock(JackrabbitAccessControlEntry.class);
        when(editorAce.isAllow()).thenReturn(true);
        when(editorAce.getPrincipal()).thenReturn(new PrincipalImpl("editors"));
        when(editorAce.getPrivileges()).thenReturn(new Privilege[]{privilegeByName("jcr:read")});
        when(editorAce.getRestrictionNames()).thenReturn(new String[]{repMvTags});
        when(editorAce.getRestrictions(repMvTags)).thenReturn(
                new Value[]{ValueFactoryImpl.getInstance().createValue(name2),
                        ValueFactoryImpl.getInstance().createValue(name1)});
        when(acl3.getAccessControlEntries()).thenReturn(new AccessControlEntry[]{editorAce});

        ExpectAces.AceCriteria criteria3mva = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:football,tag:pingpong")}, null);
        assertTrue("expect satisfied by mv ace with 2-valued restriction", criteria3mva.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3mvb = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:pingpong,tag:football")}, null);
        assertTrue("expect satisfied by mv ace with 2-value-reversed restriction", criteria3mvb.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3mvc = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:pingpong,tag:baseball")}, null);
        assertFalse("expect not satisfied by mv ace with 2-value 1-incorrect restriction", criteria3mvc.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3mvd = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:baseball,tag:football")}, null);
        assertFalse("expect not satisfied by mv ace with 2-value-reversed 1-incorrect restriction", criteria3mvd.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3sva = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:football")}, null);
        assertTrue("expect satisfied by sv ace with 1-value correct restriction", criteria3sva.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3svb = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:pingpong")}, null);
        assertTrue("expect satisfied by sv ace with 1-value other correct restriction", criteria3svb.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3svc = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", "tag:baseball")}, null);
        assertFalse("expect not satisfied by sv ace with 1-value other incorrect restriction", criteria3svc.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3svd = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("mvNames", null)}, null);
        assertTrue("expect satisfied by sv ace with 1-value other incorrect restriction", criteria3svd.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3sve = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[0], null);
        assertTrue("expect satisfied by sv ace with no restriction", criteria3sve.satisfiedBy(acl3));
        ExpectAces.AceCriteria criteria3svz = new ExpectAces.AceCriteria("editors", true, "/foo",
                new String[]{"jcr:read"},
                new ExpectAces.RestrictionCriteria[]{new ExpectAces.RestrictionCriteria("rep:glob", "*")}, null);
        assertFalse("expect not satisfied by sv ace with wrong restriction name", criteria3svz.satisfiedBy(acl3));

    }

    @Test
    public void testRestrictionCriteria_equalsHashCode() {
        final ExpectAces.RestrictionCriteria left = new ExpectAces.RestrictionCriteria("left", null);
        assertEquals("expect same", left, left);
        assertEquals("expect same hash", left.hashCode(), left.hashCode());
        assertNotEquals("expect null not equal", left, null);
        final ExpectAces.RestrictionCriteria leftAgain = new ExpectAces.RestrictionCriteria("left", null);
        assertEquals("expect same", left, leftAgain);
        assertEquals("expect same hash", left.hashCode(), leftAgain.hashCode());
        final ExpectAces.RestrictionCriteria right = new ExpectAces.RestrictionCriteria("right", null);
        assertNotEquals("expect not same", left, right);
        assertNotEquals("expect not same hash", left.hashCode(), right.hashCode());
    }
}