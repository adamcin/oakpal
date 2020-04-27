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

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;

/**
 * The {@code overlaps} check keeps track of installed package workspace filters, and checks every affected path going
 * forward against previous workspace filters for overlap, using {@link WorkspaceFilter#contains(String)}. Overlapping
 * deletions are reported as {@link Severity#MAJOR}, whereas other affected paths are
 * reported as {@link Severity#MINOR}.
 * <p>
 * This check is sequence-dependent, in that changing the sequence of packages in the scan may result in a different
 * outcome. It is recommended to test multiple sequences if the actual process for package deployment is undefined or
 * subject to change. This check is not effective for single package scans nor is it effective for determining overlaps
 * with {@code preInstallPackages}.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code reportAllOverlaps}</dt>
 * <dd>Set to true to report all overlapping paths, which can be verbose. Otherwise, report only one violation at
 * the highest severity encountered.</dd>
 * </dl>
 */
public final class Overlaps implements ProgressCheckFactory {
    @ProviderType
    public interface JsonKeys {
        String reportAllOverlaps();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String reportAllOverlaps() {
            return "reportAllOverlaps";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    @Deprecated
    public static final String CONFIG_REPORT_ALL_OVERLAPS = keys().reportAllOverlaps();

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final boolean reportAllOverlaps = hasNonNull(config, keys().reportAllOverlaps())
                && config.getBoolean(keys().reportAllOverlaps());
        return new Check(reportAllOverlaps);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<Overlaps> {

        final Map<PackageId, WorkspaceFilter> filters = new HashMap<>();
        final Map<PackageId, Severity> reported = new HashMap<>();

        final boolean reportAllOverlaps;

        Check(final boolean reportAllOverlaps) {
            super(Overlaps.class);
            this.reportAllOverlaps = reportAllOverlaps;
        }

        @Override
        public void startedScan() {
            super.startedScan();
            filters.clear();
            reported.clear();
        }

        @Override
        public void beforeExtract(final PackageId packageId, final Session inspectSession,
                                  final PackageProperties packageProperties, final MetaInf metaInf,
                                  final List<PackageId> subpackages) throws RepositoryException {
            filters.put(packageId, metaInf.getFilter());
        }

        Predicate<? super Map.Entry<PackageId, WorkspaceFilter>> overlaps(final String path) {
            return entry -> entry.getValue().contains(path);
        }

        void findOverlaps(final PackageId currentPackageId, final String path,
                          final Severity severity) {
            // fast escape! no need to belabor the point.
            if (!reportAllOverlaps
                    && reported.containsKey(currentPackageId)
                    && !reported.get(currentPackageId).isLessSevereThan(severity)) {
                return;
            }

            // find any overlapping filters, looking forward.
            List<PackageId> overlapping = filters.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(currentPackageId))
                    .filter(overlaps(path))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());


            if (!overlapping.isEmpty()) {
                if (!reportAllOverlaps) {
                    reported.put(currentPackageId, severity);
                }
                reporting(violation -> violation.withSeverity(severity)
                        .withPackage(currentPackageId)
                        .withDescription("affected path {0} overlaps {1}")
                        .withArgument(path, overlapping));
            }
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node,
                                 final PathAction action)
                throws RepositoryException {
            // don't worry about nodes outside of our own scope.
            if (filters.get(packageId).contains(path)) {
                findOverlaps(packageId, path, Severity.MINOR);
            }
        }

        @Override
        public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
                throws RepositoryException {
            // any deletions should be considered major overlap violations.
            findOverlaps(packageId, path, Severity.MAJOR);
        }
    }
}
