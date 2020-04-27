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
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Rules;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.json.JsonObject;
import java.util.List;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;

/**
 * Check for subpackage inclusion.
 * <p>
 * Example config denying all subpackages:
 * <pre>
 *     "config": {
 *         "denyAll": true
 *     }
 * </pre>
 * <p>
 * Example config requiring an explicit group for all subpackages:
 * <pre>
 *     "config": {
 *         "rules": [{
 *             "type": "deny",
 *             "pattern": "my_packages/.*"
 *         }]
 *     }
 * </pre>
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code rules}</dt>
 * <dd>A list of {@link Rule} elements. Rules are evaluated top-to-bottom. The type of the last rule to match is
 * the effective action taken for the element. Any affected packageId matching a DENY rule will be reported as a
 * violation.</dd>
 * <dt>{@code denyAll}</dt>
 * <dd>Set to true to report a violation for any subpackage installation.</dd>
 * </dl>
 */
public final class Subpackages implements ProgressCheckFactory {
    @ProviderType
    public interface JsonKeys {
        String rules();

        String denyAll();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String rules() {
            return "rules";
        }

        @Override
        public String denyAll() {
            return "denyAll";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    @Deprecated
    public static final String CONFIG_RULES = keys().rules();
    @Deprecated
    public static final String CONFIG_DENY_ALL = keys().denyAll();

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        List<Rule> rules = Rules.fromJsonArray(arrayOrEmpty(config, keys().rules()));

        final boolean denyAll = hasNonNull(config, keys().denyAll()) && config.getBoolean(keys().denyAll());

        return new Check(rules, denyAll);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<Subpackages> {
        private final List<Rule> rules;
        private final boolean denyAll;

        Check(final List<Rule> rules, final boolean denyAll) {
            super(Subpackages.class);
            this.rules = rules;
            this.denyAll = denyAll;
        }

        @Override
        public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
            if (denyAll) {
                reporting(violation -> violation
                        .withSeverity(Severity.MAJOR)
                        .withPackage(packageId)
                        .withDescription("subpackage {0} included by {1}. no subpackages are allowed.")
                        .withArgument(packageId, parentId));
            } else {
                final Rule lastMatch = Rules.lastMatch(rules, packageId.toString());
                if (lastMatch.isDeny()) {
                    reporting(violation -> violation
                            .withSeverity(Severity.MAJOR)
                            .withPackage(packageId)
                            .withDescription("subpackage {0} included by {1} matches deny pattern {2}")
                            .withArgument(packageId, parentId, lastMatch.getPattern().pattern()));
                }
            }
        }
    }
}
