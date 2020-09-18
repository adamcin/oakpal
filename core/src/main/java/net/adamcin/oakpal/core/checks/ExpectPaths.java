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
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Rules;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import net.adamcin.oakpal.api.SlingInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
 * <dt>{@code ignoreNestedPackages}</dt>
 * <dd>By default, expectations are evaluated for all packages matching the {@code afterPackageIdRules}. Set this to
 * true to only evaluate after extracting and again after scanning a matching root package.</dd>
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
        // TODO 2.3.0 export to JsonKeys interface
        final boolean ignoreNestedPackages = config.getBoolean("ignoreNestedPackages", false);
        final Severity severity = Severity.valueOf(
                config.getString(keys().severity(), DEFAULT_SEVERITY.name()).toUpperCase());
        return new Check(expectedPaths, notExpectedPaths, afterPackageIdRules, ignoreNestedPackages, severity);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<ExpectPaths> {

        final List<String> expectedPaths;
        final List<String> notExpectedPaths;
        final List<Rule> afterPackageIdRules;
        final boolean ignoreNestedPackages;
        final Severity severity;
        final PackageGraph graph = new PackageGraph();
        final Map<String, List<PackageId>> expectedViolators = new LinkedHashMap<>();
        final Map<String, List<PackageId>> notExpectedViolators = new LinkedHashMap<>();

        Check(final @NotNull List<String> expectedPaths,
              final @NotNull List<String> notExpectedPaths,
              final @NotNull List<Rule> afterPackageIdRules,
              final boolean ignoreNestedPackages,
              final @NotNull Severity severity) {
            super(ExpectPaths.class);
            this.expectedPaths = expectedPaths;
            this.notExpectedPaths = notExpectedPaths;
            this.afterPackageIdRules = afterPackageIdRules;
            this.ignoreNestedPackages = ignoreNestedPackages;
            this.severity = severity;
        }

        @Override
        public void startedScan() {
            super.startedScan();
            graph.startedScan();
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
            return (graph.isRoot(packageId) || !this.ignoreNestedPackages)
                    && Rules.lastMatch(afterPackageIdRules, packageId.toString()).isInclude();
        }

        /**
         * Exposed for testing.
         *
         * @return the package graph
         */
        PackageGraph getGraph() {
            return graph;
        }

        @Override
        public void identifyPackage(final PackageId packageId, final File file) {
            graph.identifyPackage(packageId, file);
        }

        @Override
        public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
            graph.identifySubpackage(packageId, parentId);
        }

        @Override
        public void identifyEmbeddedPackage(final PackageId packageId, final PackageId parentId,
                                            final EmbeddedPackageInstallable slingInstallable) {
            graph.identifyEmbeddedPackage(packageId, parentId, slingInstallable);
        }

        /**
         * Perform the logic to validate expectations against current state using the provided {@code inspectSession}.
         * Any new violations detected for a particular set of path criteria will be blamed on the collection of
         * packageIds provided by the {@code possibleViolators} argument.
         *
         * @param possibleViolators the collection of possible violator packages with influence over the current
         *                          repository state
         * @param inspectSession    the JCR session to inspect for conformance to configured expectations
         * @throws RepositoryException if JCR error occurs during validation
         */
        void blameViolatorsForMissedExpectations(final @NotNull Collection<PackageId> possibleViolators,
                                                 final @NotNull Session inspectSession) throws RepositoryException {
            for (final String expectedPath : expectedPaths) {
                // only look for sling violators of an expected path if a violation for said path has not already
                // been collected.
                final List<PackageId> violators = getViolatorListForExpectedPath(expectedViolators, expectedPath);
                if (violators.isEmpty() && !inspectSession.itemExists(expectedPath)) {
                    violators.addAll(possibleViolators);
                } else if (!violators.isEmpty() && inspectSession.itemExists(expectedPath)) {
                    violators.removeAll(possibleViolators);
                }
            }
            for (final String notExpectedPath : notExpectedPaths) {
                // only look for sling violators of an unexpected path if a violation for said path has not already
                // been collected.
                final List<PackageId> violators = getViolatorListForExpectedPath(notExpectedViolators, notExpectedPath);
                if (violators.isEmpty() && inspectSession.itemExists(notExpectedPath)) {
                    violators.addAll(possibleViolators);
                } else if (!violators.isEmpty() && !inspectSession.itemExists(notExpectedPath)) {
                    violators.removeAll(possibleViolators);
                }
            }
        }

        /**
         * Validate expectations immediately after extracting a package whose ID matches the
         * {@code config.afterPackageIdRules}.
         *
         * @param packageId      the current package
         * @param inspectSession session providing access to repository state
         * @throws RepositoryException if a JCR error occurs during validation of expectations
         */
        @Override
        public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
            if (shouldExpectAfterExtract(packageId)) {
                blameViolatorsForMissedExpectations(graph.getSelfAndAncestors(packageId), inspectSession);
            }
        }

        /**
         * Validate expectations immediately after applying repo init scripts.
         *
         * @param scanPackageId    the last preinstall or scan package
         * @param scripts          the repoinit scripts that were applied
         * @param slingInstallable the associated {@link SlingInstallable} identifying the source JCR event that provided
         *                         the repo init scripts
         * @param inspectSession   session providing access to repository state
         * @throws RepositoryException if an error occurs while validating expectations
         */
        @Override
        public void appliedRepoInitScripts(final PackageId scanPackageId, final List<String> scripts,
                                           final SlingInstallable slingInstallable, final Session inspectSession)
                throws RepositoryException {
            if (shouldExpectAfterExtract(slingInstallable.getParentId())) {
                blameViolatorsForMissedExpectations(graph.getSelfAndAncestors(slingInstallable.getParentId()),
                        inspectSession);
            }
        }

        /**
         * If configured to {@code ignoreNestedPackages}, this handler will evaluate the expectations after
         * the package scan in case any violations with a transitive relationship to a packageId matching the
         * {@code config.afterPackageIdRules} can be detected.
         *
         * @param scanPackageId  the scanned package id
         * @param inspectSession session providing access to repository state
         * @throws RepositoryException if an error occurs while validating expectations
         */
        @Override
        public void afterScanPackage(final PackageId scanPackageId,
                                     final Session inspectSession) throws RepositoryException {
            if (ignoreNestedPackages && shouldExpectAfterExtract(scanPackageId)) {
                blameViolatorsForMissedExpectations(graph.getSelfAndDescendants(scanPackageId), inspectSession);
            }
        }

        /**
         * Report all violations collected during the scan.
         */
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
