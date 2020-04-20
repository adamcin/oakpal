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

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheck;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.compose;
import static net.adamcin.oakpal.api.Fun.composeTest;
import static net.adamcin.oakpal.api.Fun.inSet;
import static net.adamcin.oakpal.api.Fun.inferTest1;
import static net.adamcin.oakpal.api.Fun.mapEntry;
import static net.adamcin.oakpal.api.Fun.result0;
import static net.adamcin.oakpal.api.Fun.uncheck0;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.Fun.zipKeysWithValueFunc;

/**
 * ExpectAces: assert the existence or non-existence of specific access control entries after extracting a package.
 * {@code config} options:
 * <dl>
 * <dt>{@code principal}</dt>
 * <dd>({@code String}) REQUIRED (or {@code principals}): The expected principal name (userId or groupId) associated with
 * all the ace criteria. Takes precendence over {@code principals} unless set to the empty string.</dd>
 * <dt>{@code principals}</dt>
 * <dd>({@code String[]}) REQUIRED (or {@code principal}): The expected principal names (userId or groupId) associated
 * with all the ace criteria. This is essentially shorthand for cases where the same aces policies apply to multiple
 * principals.</dd>
 * <dt>{@code expectedAces}</dt>
 * <dd>({@code String[]}) A list of expected ACE criteria including type, privileges, and path, as well as restriction
 * constraints. See below for syntax.</dd>
 * <dt>{@code notExpectedAces}</dt>
 * <dd>({@code String[]}) A list of non-expected ACE criteria including type, privileges, and path, as well as restriction
 * constraints. See below for syntax.</dd>
 * <dt>{@code afterPackageIdRules}</dt>
 * <dd>({@code Rule[]}) An optional list of patterns describing the scope of package IDs that should trigger evaluation
 * of ACLs after extraction. By default, the expectations will be evaluated after every package is installed.</dd>
 * <dt>{@code severity}</dt>
 * <dd>By default, the severity of violations created by this check is MAJOR, but can be set to MINOR or SEVERE.</dd>
 * </dl>
 * <p>
 * rep:policy nodes are imported differently from normal DocView content. Instead of having a predictable path, they are
 * potentially merged and renamed depending on existing rep:GrantACE or rep:DenyACE children of the rep:policy. Therefore,
 * we have a special check that evaluates the presence of expected rules based on their attributes, instead of their own
 * paths within a package.
 * <p>
 * Finer-grained Access Control Handling Policies like MERGE and MERGE_PRESERVE operate along lines defined by the principal
 * identified by a particular ACE. Therefore, this check requires that you specific a specific principal to expect aces for.
 * <p>
 * The syntax for expectedAces and notExpectedAces is similar to that used by ACS AEM Commons for OSGi config definitions.
 * type=allow;privileges=jcr:read,rep:write;path=/content/foo;rep:glob=/jcr:content/*
 * <p>
 * REQUIRED
 * <ul>
 *     <li>type = allow | deny</li>
 *     <li>privileges = privilegeNames comma separated</li>
 *     <li>path = absolute path (must exist in JCR). If not specified, or the empty string, the ace criteria are evaluated against {@code /rep:repoPolicy}.</li>
 * </ul>
 * <p>
 * OPTIONAL
 * <ul>
 *     <li>principal = overrides the check config {@code principal} parameter for this particular ACE</li>
 *     <li>rep:glob = rep glob expression</li>
 *     <li>rep:ntNames = ntNames expression</li>
 *     <li>rep:nodePath = for system users, aces defined in the user home can reference applicable repo path using this
 *     restriction</li>
 *     <li>anyOtherRestriction = any name can be used as a restriction constraint. if the restriction is not allowed for
 *     use at a particular path, it will be ignored.</li>
 * </ul>
 * <p>
 * Note - restrictions with comma-separated values are evaluated as multivalued when the restriction
 * definition so indicates. Otherwise the comma-separated values are treated as an opaque string.
 */
public final class ExpectAces implements ProgressCheckFactory {
    public interface JsonKeys {
        String principal();

        String principals();

        String expectedAces();

        String notExpectedAces();

        String afterPackageIdRules();

        String severity();

        String type();

        String privileges();

        String path();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String principal() {
            return "principal";
        }

        @Override
        public String principals() {
            return "principals";
        }

        @Override
        public String expectedAces() {
            return "expectedAces";
        }

        @Override
        public String notExpectedAces() {
            return "notExpectedAces";
        }

        @Override
        public String afterPackageIdRules() {
            return "afterPackageIdRules";
        }

        @Override
        public String severity() {
            return "severity";
        }

        @Override
        public String type() {
            return "type";
        }

        @Override
        public String privileges() {
            return "privileges";
        }

        @Override
        public String path() {
            return "path";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    @Deprecated
    public static final String CONFIG_PRINCIPAL = keys().principal();
    @Deprecated
    public static final String CONFIG_PRINCIPALS = keys().principals();
    @Deprecated
    public static final String CONFIG_EXPECTED_ACES = keys().expectedAces();
    @Deprecated
    public static final String CONFIG_NOT_EXPECTED_ACES = keys().notExpectedAces();
    @Deprecated
    public static final String CONFIG_AFTER_PACKAGE_ID_RULES = keys().afterPackageIdRules();
    @Deprecated
    public static final String ACE_PARAM_TYPE = keys().type();
    @Deprecated
    public static final String ACE_PARAM_PRIVILEGES = keys().privileges();
    @Deprecated
    public static final String ACE_PARAM_PATH = keys().path();

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpectAces.class);
    static final Severity DEFAULT_SEVERITY = Severity.MAJOR;
    public static final String DELIM_PARAM = ";";
    public static final String DELIM_VALUE = "=";
    public static final String DELIM_LIST = ",";

    @Override
    public ProgressCheck newInstance(final JsonObject config) throws Exception {
        final String principal = config.getString(keys().principal(), "").trim();
        final String[] principals = JavaxJson.mapArrayOfStrings(JavaxJson.arrayOrEmpty(config, keys().principals()))
                .stream().map(String::trim).filter(inferTest1(String::isEmpty).negate()).toArray(String[]::new);

        final String[] precedingPrincipals = principal.isEmpty() ? principals : new String[]{principal};

        final List<AceCriteria> expectedAces = parseAceCriteria(config, precedingPrincipals, keys().expectedAces());
        final List<AceCriteria> notExpectedAces = parseAceCriteria(config, precedingPrincipals, keys().notExpectedAces());
        final Severity severity = Severity.valueOf(
                config.getString(keys().severity(), DEFAULT_SEVERITY.name()).toUpperCase());
        return new Check(expectedAces, notExpectedAces,
                Rule.fromJsonArray(JavaxJson.arrayOrEmpty(config, keys().afterPackageIdRules())), severity);
    }

    static boolean isPrincipalSpec(final @NotNull String spec) {
        return spec.contains(keys().principal() + "=");
    }

    static boolean isGeneralSpec(final @NotNull String spec) {
        return !isPrincipalSpec(spec);
    }

    static List<AceCriteria> parseAceCriteria(final @NotNull JsonObject config,
                                              final @NotNull String[] principals,
                                              final @NotNull String key) throws Exception {
        final List<AceCriteria> allCriterias = new ArrayList<>();
        final List<String> specs = JavaxJson.mapArrayOfStrings(JavaxJson.arrayOrEmpty(config, key));
        final List<String> generalSpecs = specs.stream().filter(ExpectAces::isGeneralSpec).collect(Collectors.toList());
        if (!generalSpecs.isEmpty() && principals.length == 0) {
            throw new Exception("principal or principals check config param must be non-empty if general ACE criteria are specified");
        } else {
            for (String principal : principals) {
                final Result<List<AceCriteria>> expectedAceResults = generalSpecs.stream()
                        .map(spec -> AceCriteria.parse(principal, spec))
                        .collect(Result.tryCollect(Collectors.toList()));
                if (expectedAceResults.getError().isPresent()) {
                    throw new Exception("invalid criteria in " + key + ". " + expectedAceResults.getError()
                            .map(Throwable::getMessage).orElse(""));
                }
                expectedAceResults.forEach(allCriterias::addAll);
            }
        }
        final Result<List<AceCriteria>> principalAceResults = specs.stream()
                .filter(ExpectAces::isPrincipalSpec)
                .map(spec -> AceCriteria.parse("", spec))
                .collect(Result.tryCollect(Collectors.toList()));
        if (principalAceResults.getError().isPresent()) {
            throw new Exception("invalid criteria in " + key + ". " + principalAceResults.getError()
                    .map(Throwable::getMessage).orElse(""));
        }
        principalAceResults.forEach(allCriterias::addAll);

        return allCriterias;
    }

    static final class Check extends SimpleProgressCheck {
        final List<AceCriteria> expectedAces;
        final List<AceCriteria> notExpectedAces;
        final Map<AceCriteria, List<PackageId>> expectedViolators = new LinkedHashMap<>();
        final Map<AceCriteria, List<PackageId>> notExpectedViolators = new LinkedHashMap<>();
        final List<Rule> afterPackageIdRules;
        final Severity severity;

        Check(final @NotNull List<AceCriteria> expectedAces,
              final @NotNull List<AceCriteria> notExpectedAces,
              final @NotNull List<Rule> afterPackageIdRules,
              final @NotNull Severity severity) {
            this.expectedAces = expectedAces;
            this.notExpectedAces = notExpectedAces;
            this.afterPackageIdRules = afterPackageIdRules;
            this.severity = severity;
        }

        @Override
        public String getCheckName() {
            return ExpectAces.class.getSimpleName();
        }

        @Override
        public void startedScan() {
            super.startedScan();
            expectedViolators.clear();
            notExpectedViolators.clear();
        }

        static Map<String, List<AceCriteria>> groupCriteriaByPath(final @NotNull List<AceCriteria> criteriaList) {
            return criteriaList.stream().collect(Collectors.groupingBy(AceCriteria::getPath));
        }

        boolean shouldExpectAfterExtract(final @NotNull PackageId packageId) {
            return Rule.lastMatch(afterPackageIdRules, packageId.toString()).isInclude();
        }

        static List<PackageId> getViolatorListForExpectedCriteria(final @NotNull Map<AceCriteria, List<PackageId>> violatorsMap,
                                                                  final @NotNull AceCriteria criteria) {
            if (!violatorsMap.containsKey(criteria)) {
                violatorsMap.put(criteria, new ArrayList<>());
            }
            return violatorsMap.get(criteria);
        }

        @Override
        public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
            if (shouldExpectAfterExtract(packageId)) {
                final JackrabbitAccessControlManager aclManager = (JackrabbitAccessControlManager) inspectSession.getAccessControlManager();
                final Map<String, List<AceCriteria>> expectedsByPath = groupCriteriaByPath(expectedAces);
                final Map<String, List<AceCriteria>> notExpectedsByPath = groupCriteriaByPath(notExpectedAces);
                final Set<String> allPaths = new LinkedHashSet<>(expectedsByPath.keySet());
                allPaths.addAll(notExpectedsByPath.keySet());
                for (String path : allPaths) {
                    final JackrabbitAccessControlList[] policiesAtPath =
                            // provide null path for rep:repoPolicy evaluation
                            (path.isEmpty() ? Stream.of(aclManager.getPolicies((String) null))
                                    : (inspectSession.nodeExists(path) ? Stream.of(aclManager.getPolicies(path))
                                    : Stream.empty()))
                                    .filter(JackrabbitAccessControlList.class::isInstance)
                                    .map(JackrabbitAccessControlList.class::cast)
                                    .toArray(JackrabbitAccessControlList[]::new);
                    for (AceCriteria criteria : expectedsByPath.getOrDefault(path, Collections.emptyList())) {
                        if (Stream.of(policiesAtPath).noneMatch(criteria::satisfiedBy)) {
                            getViolatorListForExpectedCriteria(expectedViolators, criteria).add(packageId);
                        }
                    }
                    for (AceCriteria criteria : notExpectedsByPath.getOrDefault(path, Collections.emptyList())) {
                        if (Stream.of(policiesAtPath).anyMatch(criteria::satisfiedBy)) {
                            getViolatorListForExpectedCriteria(notExpectedViolators, criteria).add(packageId);
                        }
                    }
                }
            }
        }

        @Override
        public void finishedScan() {
            for (Map.Entry<AceCriteria, List<PackageId>> violatorsEntry : expectedViolators.entrySet()) {
                if (!violatorsEntry.getValue().isEmpty()) {
                    this.reportViolation(severity, "expected: " + violatorsEntry.getKey().toString(),
                            violatorsEntry.getValue().toArray(new PackageId[0]));
                }
            }
            expectedViolators.clear();
            for (Map.Entry<AceCriteria, List<PackageId>> violatorsEntry : notExpectedViolators.entrySet()) {
                if (!violatorsEntry.getValue().isEmpty()) {
                    this.reportViolation(severity, "unexpected: " + violatorsEntry.getKey().toString(),
                            violatorsEntry.getValue().toArray(new PackageId[0]));
                }
            }
            notExpectedViolators.clear();
        }
    }

    static final class RestrictionCriteria {
        final @NotNull String name;
        final @Nullable String value;

        RestrictionCriteria(@NotNull String name, @Nullable String value) {
            this.name = name;
            this.value = value;
        }

        @NotNull String getName() {
            return name;
        }

        @Nullable String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RestrictionCriteria that = (RestrictionCriteria) o;
            return name.equals(that.name) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        boolean satisfiedBy(final boolean multi, final @NotNull JackrabbitAccessControlEntry ace) {
            if (getValue() == null) {
                return Arrays.asList(uncheck0(ace::getRestrictionNames).get()).contains(getName());
            } else {
                if (multi) {
                    String[] expecteds = Stream.of(getValue().split(DELIM_LIST))
                            .map(String::trim)
                            .filter(inferTest1(String::isEmpty).negate())
                            .toArray(String[]::new);
                    Value[] definedValues = compose(uncheck1(ace::getRestrictions), Optional::ofNullable).apply(getName())
                            .orElse(new Value[0]);
                    List<String> defineds = Stream.of(definedValues)
                            .map(compose(uncheck1(Value::getString), String::trim))
                            .collect(Collectors.toList());
                    return Stream.of(expecteds).allMatch(inSet(defineds));
                } else {
                    String expected = getValue();
                    String defined = compose(uncheck1(ace::getRestriction), Optional::ofNullable)
                            .apply(getName())
                            .map(uncheck1(Value::getString))
                            .orElse(null);
                    return expected.equals(defined);
                }
            }
        }
    }

    static Predicate<JackrabbitAccessControlEntry>
    restrictionMatcher(final @NotNull JackrabbitAccessControlList acl,
                       final @NotNull RestrictionCriteria[] restrictionCriterias) {
        if (restrictionCriterias.length == 0) {
            return ace -> true;
        } else {
            final List<String> allowedRestrictions = result0(acl::getRestrictionNames).get()
                    .map(Arrays::asList).getOrDefault(Collections.emptyList());
            final List<Map.Entry<RestrictionCriteria, Boolean>> criterias = Stream.of(restrictionCriterias)
                    .filter(composeTest(RestrictionCriteria::getName, inSet(allowedRestrictions)))
                    .map(zipKeysWithValueFunc(compose(RestrictionCriteria::getName, uncheck1(acl::isMultiValueRestriction))))
                    .collect(Collectors.toList());
            return ace -> criterias.stream().allMatch(criteria -> criteria.getKey().satisfiedBy(criteria.getValue(), ace));
        }
    }

    static final class AceCriteria {
        final @NotNull String principal;
        final boolean isAllow;
        final @NotNull String path;
        final @NotNull String[] privileges;
        final @NotNull RestrictionCriteria[] restrictions;
        final @Nullable String spec;

        AceCriteria(final @NotNull String principal,
                    boolean isAllow,
                    final @NotNull String path,
                    final @NotNull String[] privileges,
                    final @NotNull RestrictionCriteria[] restrictions,
                    final @Nullable String spec) {
            this.isAllow = isAllow;
            this.path = path;
            this.principal = principal;
            this.privileges = privileges;
            this.restrictions = restrictions;
            this.spec = spec;
        }

        enum Type {
            ALLOW, DENY;

            static Type forName(final String name) {
                for (Type value : values()) {
                    if (value.name().equalsIgnoreCase(name)) {
                        return value;
                    }
                }
                return null;
            }
        }

        static Result<AceCriteria> parse(final @NotNull String principal, final @NotNull String spec) {
            final List<Map.Entry<String, Optional<String>>> pairs = Stream.of(spec.trim().split(DELIM_PARAM))
                    .map(param -> {
                        String[] parts = param.split(DELIM_VALUE, 2);
                        final String trimmedValue = parts.length > 1 ? parts[1].trim() : "";
                        return Fun.toEntry(parts[0].trim(),
                                trimmedValue.isEmpty()
                                        ? Optional.<String>empty()
                                        : Optional.of(trimmedValue));
                    }).collect(Collectors.toList());

            final Set<String> keys = pairs.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
            final Map<String, String> valueMap = pairs.stream()
                    .filter(Fun.testValue(Optional::isPresent))
                    .map(Fun.mapValue(Optional::get))
                    .collect(Fun.entriesToMap());

            final String effectivePrincipal = valueMap.getOrDefault(keys().principal(), principal).trim();
            if (effectivePrincipal.isEmpty()) {
                return Result.failure("principal must be non-empty: " + spec);
            }
            keys.remove(keys().principal());

            if (!valueMap.containsKey(keys().type())) {
                return Result.failure(keys().type() + " is a required ace parameter: " + spec);
            }

            final Type type = Type.forName(valueMap.get(keys().type()));
            if (type == null) {
                return Result.failure(valueMap.get(keys().type()) + " is not a valid value for parameter " + keys().type() + ": " + spec);
            }
            keys.remove(keys().type());

            if (!keys.contains(keys().path())) {
                return Result.failure(keys().path() + " parameter is required, but can be left empty to match rep:repoPolicy aces: " + spec);
            }
            final String path = Optional.ofNullable(valueMap.get(keys().path())).map(String::trim).orElse("");
            if (path.isEmpty()) {
                LOGGER.debug("empty path param. spec will evaluate /rep:repoPolicy : {}", spec);
            }
            keys.remove(keys().path());

            final String[] privileges = Stream.of(Optional.ofNullable(valueMap.get(keys().privileges()))
                    .orElse("").split(DELIM_LIST))
                    .map(String::trim)
                    .filter(inferTest1(String::isEmpty).negate())
                    .toArray(String[]::new);

            if (privileges.length == 0) {
                return Result.failure("privileges must be specified with at least one element: " + spec);
            }
            keys.remove(keys().privileges());

            RestrictionCriteria[] restrictions = keys.stream()
                    .map(Fun.zipKeysWithValueFunc(valueMap::get))
                    .map(mapEntry(RestrictionCriteria::new))
                    .toArray(RestrictionCriteria[]::new);

            return Result.success(new AceCriteria(effectivePrincipal,
                    type == Type.ALLOW, path, privileges, restrictions, spec));
        }

        String getSpec() {
            if (spec != null) {
                return spec;
            } else {
                final StringBuilder common = new StringBuilder()
                        .append(keys().type()).append(DELIM_VALUE).append(isAllow ? "allow" : "deny")
                        .append(DELIM_PARAM)
                        .append(keys().path()).append(DELIM_VALUE).append(path)
                        .append(DELIM_PARAM)
                        .append(keys().privileges()).append(DELIM_VALUE).append(String.join(DELIM_LIST, privileges));
                for (RestrictionCriteria restriction : restrictions) {
                    common.append(DELIM_PARAM).append(restriction.name);
                    if (restriction.value != null) {
                        common.append(DELIM_VALUE).append(restriction.value);
                    }
                }

                return common.toString();
            }
        }

        @Override
        public String toString() {
            return "principal:" + principal + " ace:" + getSpec();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AceCriteria criteria = (AceCriteria) o;
            return isAllow == criteria.isAllow &&
                    principal.equals(criteria.principal) &&
                    path.equals(criteria.path) &&
                    Arrays.equals(privileges, criteria.privileges) &&
                    Arrays.equals(restrictions, criteria.restrictions) &&
                    Objects.equals(spec, criteria.spec);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(principal, isAllow, path, spec);
            result = 31 * result + Arrays.hashCode(privileges);
            result = 31 * result + Arrays.hashCode(restrictions);
            return result;
        }

        public @NotNull String getPath() {
            return path;
        }

        /**
         * IMPORTANT: Path is not considered by this method. Instead, it should be used to retrieve the appropriate acl
         * to provide.
         * The test for satisfying this criteria against an acl is as follows:
         * 1. filter all aces in acl by implementation type, to ensure each is a JackrabbitAccessControlEntry
         * 2. filter further to only consider matching type of allow/deny and is same principal (group membership is not considered)
         * 3. filter further to only consider aces with restrictions that match the criteria restrictions, if specified.
         * 4. iterate over remaining aces checking that each required privilege is matched at least once.
         *
         * @param acl the acl to match against
         * @return true if this criteria is matched/satisfied by the provided acl.
         */
        boolean satisfiedBy(final @NotNull JackrabbitAccessControlList acl) {
            AccessControlEntry[] entries = Fun.uncheck0(acl::getAccessControlEntries).get();
            final Predicate<JackrabbitAccessControlEntry> satisfiesRestrictions = restrictionMatcher(acl, restrictions);
            final JackrabbitAccessControlEntry[] matchableEntries = Stream.of(entries)
                    .filter(JackrabbitAccessControlEntry.class::isInstance)
                    .map(JackrabbitAccessControlEntry.class::cast)
                    .filter(ace -> ace.isAllow() == this.isAllow
                            && principal.equals(ace.getPrincipal().getName()))
                    .filter(satisfiesRestrictions)
                    .toArray(JackrabbitAccessControlEntry[]::new);

            final Set<String> expectedPrivileges = new HashSet<>(Arrays.asList(privileges));
            for (JackrabbitAccessControlEntry entry : matchableEntries) {
                for (Privilege entryPrivilege : entry.getPrivileges()) {
                    expectedPrivileges.remove(entryPrivilege.getName());
                    for (Privilege aggregatedPrivilege : entryPrivilege.getAggregatePrivileges()) {
                        expectedPrivileges.remove(aggregatedPrivilege.getName());
                    }
                }
            }
            return expectedPrivileges.isEmpty();
        }
    }
}
