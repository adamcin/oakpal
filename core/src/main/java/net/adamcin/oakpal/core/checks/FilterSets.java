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
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.List;

import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;

/**
 * Sanity check for {@link WorkspaceFilter}.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code importModeSeverity}</dt>
 * <dd>(default: {@link Severity#MINOR}) The default {@link ImportMode} for a
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
    @ProviderType
    public interface JsonKeys {
        String importModeSeverity();

        String allowEmptyFilter();

        String allowRootFilter();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String importModeSeverity() {
            return "importModeSeverity";
        }

        @Override
        public String allowEmptyFilter() {
            return "allowEmptyFilter";
        }

        @Override
        public String allowRootFilter() {
            return "allowRootFilter";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    @Deprecated
    public static final String CONFIG_IMPORT_MODE_SEVERITY = keys().importModeSeverity();
    @Deprecated
    public static final String CONFIG_ALLOW_EMPTY_FILTER = keys().allowEmptyFilter();
    @Deprecated
    public static final String CONFIG_ALLOW_ROOT_FILTER = keys().allowRootFilter();

    public static final Severity DEFAULT_IMPORT_MODE_SEVERITY = Severity.MINOR;

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final Severity importModeSeverity = Severity
                .valueOf(config.getString(keys().importModeSeverity(), DEFAULT_IMPORT_MODE_SEVERITY.name())
                        .toUpperCase());
        final boolean allowEmptyFilter = hasNonNull(config, keys().allowEmptyFilter())
                && config.getBoolean(keys().allowEmptyFilter());
        final boolean allowRootFilter = hasNonNull(config, keys().allowRootFilter())
                && config.getBoolean(keys().allowRootFilter());
        return new Check(importModeSeverity, allowEmptyFilter, allowRootFilter);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<FilterSets> {
        final Severity importModeSeverity;
        final boolean allowEmptyFilter;
        final boolean allowRootFilter;

        Check(final Severity importModeSeverity, final boolean allowEmptyFilter, final boolean allowRootFilter) {
            super(FilterSets.class);
            this.importModeSeverity = importModeSeverity;
            this.allowEmptyFilter = allowEmptyFilter;
            this.allowRootFilter = allowRootFilter;
        }

        @Override
        public void beforeExtract(final PackageId packageId, final Session inspectSession,
                                  final PackageProperties packageProperties, final MetaInf metaInf,
                                  final List<PackageId> subpackages) throws RepositoryException {
            final WorkspaceFilter filter = metaInf.getFilter();
            if (filter == null || filter.getFilterSets().isEmpty()) {
                if (!allowEmptyFilter) {
                    reportViolation(Severity.MAJOR,
                            "empty workspace filter is not allowed", packageId);
                }
            } else {
                for (PathFilterSet filterSet : filter.getFilterSets()) {
                    if (filterSet.getImportMode() != ImportMode.REPLACE) {
                        reporting(violation -> violation.withSeverity(importModeSeverity)
                                .withDescription("non-default import mode {0} defined for filterSet with root {1}")
                                .withArgument(filterSet.getImportMode(), filterSet.getRoot())
                                .withPackage(packageId));
                    }
                    if (!allowRootFilter && "/".equals(filterSet.getRoot())) {
                        reportViolation(Severity.MAJOR,
                                "root filter sets are not allowed", packageId);
                    }
                }
            }
        }
    }
}
