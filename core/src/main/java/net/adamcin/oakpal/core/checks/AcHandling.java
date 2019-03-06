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

import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.core.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.core.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.core.Util.compose;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.IGNORE;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.MERGE;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.MERGE_PRESERVE;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.OVERWRITE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

/**
 * Limit package {@code acHandling} mode to prevent unforeseen changes to ACLs upon installation.
 * <p>
 * Example config using {@link ACHandlingLevelSet#ONLY_IGNORE}:
 * <pre>
 *     "config": {
 *         "levelSet": "only_ignore"
 *     }
 * </pre>
 * <p>
 * Example config requiring {@link AccessControlHandling#MERGE_PRESERVE}:
 * <pre>
 *     "config": {
 *         "allowedModes": ["merge_preserve"]
 *     }
 * </pre>
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code allowedModes}</dt>
 * <dd>An explicit list of allowed {@link AccessControlHandling} modes.</dd>
 * <dt>{@code levelSet}</dt>
 * <dd>(default: {@code no_unsafe}) One of {@code no_clear}, {@code no_unsafe}, {@code only_add}, or {@code only_ignore},
 * matching the values in {@link ACHandlingLevelSet}.</dd>
 * </dl>
 */
public final class AcHandling extends CompatBaseFactory implements ProgressCheckFactory {
    public static final String CONFIG_ALLOWED_MODES = "allowedModes";
    public static final String CONFIG_LEVEL_SET = "levelSet";
    static final ACHandlingLevelSet DEFAULT_LEVEL_SET = ACHandlingLevelSet.NO_UNSAFE;

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        if (hasNonNull(config, CONFIG_ALLOWED_MODES)) {
            List<AccessControlHandling> allowedModes = mapArrayOfStrings(arrayOrEmpty(config, CONFIG_ALLOWED_MODES),
                    compose(String::toUpperCase, AccessControlHandling::valueOf), true);
            return new Check(ACHandlingLevelSet.EXPLICIT, allowedModes);
        } else if (hasNonNull(config, CONFIG_LEVEL_SET)) {
            ACHandlingLevelSet levelSet = ACHandlingLevelSet.valueOf(config.getString(CONFIG_LEVEL_SET).toUpperCase());
            return new Check(levelSet, Collections.emptyList());
        } else {
            return new Check(DEFAULT_LEVEL_SET, Collections.emptyList());
        }
    }

    /**
     * Encapsulation of incrementally wider sets of forbidden acHandling modes as discrete levels.
     */
    public enum ACHandlingLevelSet {
        /**
         * Marker set for explicit enumeration in {@code allowedModes} array.
         */
        EXPLICIT(Collections.emptyList()),

        /**
         * Allow all acHandling modes except for {@link AccessControlHandling#CLEAR}.
         */
        NO_CLEAR(Arrays.asList(OVERWRITE, MERGE, MERGE_PRESERVE, IGNORE)),

        /**
         * (Default levelSet) Prevent blindly destructive modes. Package can still replace existing permissions for
         * any principal identified in ACEs included in the package
         */
        NO_UNSAFE(Arrays.asList(MERGE, MERGE_PRESERVE, IGNORE)),

        /**
         * Allow only {@link AccessControlHandling#MERGE_PRESERVE} or {@link AccessControlHandling#IGNORE}. This results
         * in only allowing additive ACE changes.
         */
        ONLY_ADD(Arrays.asList(MERGE_PRESERVE, IGNORE)),

        /**
         * Prevent any ACL changes by requiring {@link AccessControlHandling#IGNORE}.
         */
        ONLY_IGNORE(Collections.singletonList(IGNORE));

        private final List<AccessControlHandling> allowedModes;

        ACHandlingLevelSet(final List<AccessControlHandling> allowedModes) {
            this.allowedModes = allowedModes;
        }

        public List<AccessControlHandling> getAllowedModes() {
            return allowedModes;
        }
    }

    static final class Check extends SimpleProgressCheck {
        final ACHandlingLevelSet levelSet;
        final List<AccessControlHandling> allowedModes;

        Check(final ACHandlingLevelSet levelSet,
              final List<AccessControlHandling> allowedModes) {
            this.levelSet = levelSet;
            this.allowedModes = allowedModes;
        }

        @Override
        public String getCheckName() {
            return AcHandling.class.getSimpleName();
        }

        @Override
        public void beforeExtract(final PackageId packageId, final Session inspectSession,
                                  final PackageProperties packageProperties, final MetaInf metaInf,
                                  final List<PackageId> subpackages) throws RepositoryException {
            if (this.levelSet == null) {
                return;
            }

            AccessControlHandling packageMode = ofNullable(packageProperties.getACHandling()).orElse(IGNORE);
            if (this.levelSet == ACHandlingLevelSet.EXPLICIT) {
                if (!allowedModes.contains(packageMode)) {
                    reportViolation(Violation.Severity.MAJOR,
                            String.format("acHandling mode %s is forbidden. acHandling values in allowedModes are %s",
                                    packageMode, allowedModes), packageId);
                }
            } else {
                if (!this.levelSet.getAllowedModes().contains(packageMode)) {
                    reportViolation(Violation.Severity.MAJOR,
                            String.format("acHandling mode %s is forbidden. allowed acHandling values in levelSet:%s are %s",
                                    packageMode, this.levelSet.name().toLowerCase(), this.levelSet.getAllowedModes()),
                            packageId);
                }
            }
        }
    }
}
