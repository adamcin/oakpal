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

import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Rules;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.optArray;

/**
 * ExpectPaths: assert the existence or non-existence of specific repository item paths after extracting a package.
 * {@code config} options:
 * <dl>
 * <dt>{@code expectedPaths}</dt>
 * <dd>({@code String[]}) A list of expected repository item paths that may reference a node or a property.</dd>
 * <dt>{@code notExpectedPaths}</dt>
 * <dd>({@code String[]}) A list of non-expected repository item paths that may reference a node or a property.</dd>
 * <dt>{@code afterPackageIdRules}</dt>
 * <dd>({@code Rule[]}) An optional list of patterns describing the scope of package IDs that should trigger evaluation
 * of path expectations after extraction. By default, the expectations will be evaluated after every package is installed.</dd>
 * <dt>{@code severity}</dt>
 * <dd>By default, the severity of violations created by this check is MAJOR, but can be set to MINOR or SEVERE.</dd>
 * </dl>
 */
public final class ExpectPaths implements ProgressCheckFactory {
    @ProviderType
    public interface JsonKeys {
        String expectedPaths();

        String notExpectedPaths();

        String afterPackageIdRules();

        String severity();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String expectedPaths() {
            return "expectedPaths";
        }

        @Override
        public String notExpectedPaths() {
            return "notExpectedPaths";
        }

        @Override
        public String afterPackageIdRules() {
            return "afterPackageIdRules";
        }

        @Override
        public String severity() {
            return "severity";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    @Deprecated
    public static final String CONFIG_EXPECTED_PATHS = keys().expectedPaths();
    @Deprecated
    public static final String CONFIG_NOT_EXPECTED_PATHS = keys().notExpectedPaths();
    @Deprecated
    public static final String CONFIG_AFTER_PACKAGE_ID_RULES = keys().afterPackageIdRules();
    @Deprecated
    public static final String CONFIG_SEVERITY = keys().severity();
    static final Severity DEFAULT_SEVERITY = Severity.MAJOR;

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final List<String> expectedPaths = optArray(config, keys().expectedPaths())
                .map(JavaxJson::mapArrayOfStrings)
                .orElse(Collections.emptyList());
        final List<String> notExpectedPaths = optArray(config, keys().notExpectedPaths())
                .map(JavaxJson::mapArrayOfStrings)
                .orElse(Collections.emptyList());
        final List<Rule> afterPackageIdRules = Rules.fromJsonArray(arrayOrEmpty(config, keys().afterPackageIdRules()));
        final Severity severity = Severity.valueOf(
                config.getString(keys().severity(), DEFAULT_SEVERITY.name()).toUpperCase());
        return new Check(expectedPaths, notExpectedPaths, afterPackageIdRules, severity);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<ExpectPaths> {

        final List<String> expectedPaths;
        final List<String> notExpectedPaths;
        final List<Rule> afterPackageIdRules;
        final Severity severity;
        final Map<String, List<PackageId>> expectedViolators = new LinkedHashMap<>();
        final Map<String, List<PackageId>> notExpectedViolators = new LinkedHashMap<>();

        Check(final @NotNull List<String> expectedPaths,
              final @NotNull List<String> notExpectedPaths,
              final @NotNull List<Rule> afterPackageIdRules,
              final @NotNull Severity severity) {
            super(ExpectPaths.class);
            this.expectedPaths = expectedPaths;
            this.notExpectedPaths = notExpectedPaths;
            this.afterPackageIdRules = afterPackageIdRules;
            this.severity = severity;
        }

        @Override
        public void startedScan() {
            super.startedScan();
            expectedViolators.clear();
            notExpectedViolators.clear();
        }

        static List<PackageId> getViolatorListForExpectedPath(final @NotNull Map<String, List<PackageId>> violatorsMap,
                                                              final @NotNull String path) {
            if (!violatorsMap.containsKey(path)) {
                violatorsMap.put(path, new ArrayList<>());
            }
            return violatorsMap.get(path);
        }

        boolean shouldExpectAfterExtract(final @NotNull PackageId packageId) {
            return Rules.lastMatch(afterPackageIdRules, packageId.toString()).isInclude();
        }

        @Override
        public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
            if (shouldExpectAfterExtract(packageId)) {
                for (final String expectedPath : expectedPaths) {
                    if (!inspectSession.itemExists(expectedPath)) {
                        getViolatorListForExpectedPath(expectedViolators, expectedPath).add(packageId);
                    }
                }
                for (final String notExpectedPath : notExpectedPaths) {
                    if (inspectSession.itemExists(notExpectedPath)) {
                        getViolatorListForExpectedPath(notExpectedViolators, notExpectedPath).add(packageId);
                    }
                }
            }
        }

        @Override
        public void finishedScan() {
            for (Map.Entry<String, List<PackageId>> violatorsEntry : expectedViolators.entrySet()) {
                if (!violatorsEntry.getValue().isEmpty()) {
                    this.reporting(violation -> violation
                            .withSeverity(severity)
                            .withDescription("expected path missing: {0}")
                            .withArgument(violatorsEntry.getKey())
                            .withPackages(violatorsEntry.getValue()));
                }
            }
            expectedViolators.clear();
            for (Map.Entry<String, List<PackageId>> violatorsEntry : notExpectedViolators.entrySet()) {
                if (!violatorsEntry.getValue().isEmpty()) {
                    this.reporting(violation -> violation
                            .withSeverity(severity)
                            .withDescription("unexpected path present: {0}")
                            .withArgument(violatorsEntry.getKey())
                            .withPackages(violatorsEntry.getValue()));
                }
            }
            notExpectedViolators.clear();
        }
    }
}
