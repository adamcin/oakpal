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

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.List;

import static net.adamcin.oakpal.core.JavaxJson.hasNonNull;

/**
 * Sanity check for {@link WorkspaceFilter}.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code importModeSeverity}</dt>
 * <dd>(default: {@link net.adamcin.oakpal.core.Violation.Severity#MINOR}) The default {@link ImportMode} for a
 * filter set is {@link ImportMode#REPLACE}. FileVault also supports {@link ImportMode#UPDATE} and
 * {@link ImportMode#MERGE}, but it also supports forcing the import mode for an entire package via
 * {@link org.apache.jackrabbit.vault.fs.io.ImportOptions#setImportMode(ImportMode)}, which certain platforms use to
 * force {@code REPLACE}, such as when installing a {@code .snapshot} package when a user has requested an
 * uninstall. For this reason, {@code MERGE} and {@code UPDATE} are considered to be risky, and developers are
 * encouraged to make filter roots more specific and granular to eliminate overlap with other packages installing
 * content within the same repository tree. Set this to value to {@code MAJOR} or {@code SEVERE} to dial up the
 * encouragement.</dd>
 * <dt>allowEmptyFilter</dt>
 * <dd>Set to true to suppress violations for empty workspace filters </dd>
 * <dt>allowRootFilter</dt>
 * <dd>Set to true to suppress violations for filterSets with a root path of /.</dd>
 * </dl>
 */
public final class FilterSets implements ProgressCheckFactory {
    public static final String CONFIG_IMPORT_MODE_SEVERITY = "importModeSeverity";
    public static final String CONFIG_ALLOW_EMPTY_FILTER = "allowEmptyFilter";
    public static final String CONFIG_ALLOW_ROOT_FILTER = "allowRootFilter";
    public static final Violation.Severity DEFAULT_IMPORT_MODE_SEVERITY = Violation.Severity.MINOR;

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final Violation.Severity importModeSeverity = Violation.Severity
                .valueOf(config.getString(CONFIG_IMPORT_MODE_SEVERITY, DEFAULT_IMPORT_MODE_SEVERITY.name())
                        .toUpperCase());
        final boolean allowEmptyFilter = hasNonNull(config, CONFIG_ALLOW_EMPTY_FILTER)
                && config.getBoolean(CONFIG_ALLOW_EMPTY_FILTER);
        final boolean allowRootFilter = hasNonNull(config, CONFIG_ALLOW_ROOT_FILTER)
                && config.getBoolean(CONFIG_ALLOW_ROOT_FILTER);
        return new Check(importModeSeverity, allowEmptyFilter, allowRootFilter);
    }

    static final class Check extends SimpleProgressCheck {
        final Violation.Severity importModeSeverity;
        final boolean allowEmptyFilter;
        final boolean allowRootFilter;

        Check(final Violation.Severity importModeSeverity, final boolean allowEmptyFilter, final boolean allowRootFilter) {
            this.importModeSeverity = importModeSeverity;
            this.allowEmptyFilter = allowEmptyFilter;
            this.allowRootFilter = allowRootFilter;
        }

        @Override
        public String getCheckName() {
            return FilterSets.class.getSimpleName();
        }

        @Override
        public void beforeExtract(final PackageId packageId, final Session inspectSession,
                                  final PackageProperties packageProperties, final MetaInf metaInf,
                                  final List<PackageId> subpackages) throws RepositoryException {
            final WorkspaceFilter filter = metaInf.getFilter();
            if (filter == null || filter.getFilterSets().isEmpty()) {
                if (!allowEmptyFilter) {
                    reportViolation(Violation.Severity.MAJOR,
                            "empty workspace filter is not allowed", packageId);
                }
            } else {
                for (PathFilterSet filterSet : filter.getFilterSets()) {
                    if (filterSet.getImportMode() != ImportMode.REPLACE) {
                        reportViolation(importModeSeverity,
                                String.format("non-default import mode %s defined for filterSet with root %s",
                                        filterSet.getImportMode(), filterSet.getRoot()),
                                packageId);
                    }
                    if (!allowRootFilter && "/".equals(filterSet.getRoot())) {
                        reportViolation(Violation.Severity.MAJOR,
                                "root filter sets are not allowed", packageId);
                    }
                }
            }
        }
    }
}
