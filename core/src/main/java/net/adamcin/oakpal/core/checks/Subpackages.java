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
import org.apache.jackrabbit.vault.packaging.PackageId;

import javax.json.JsonObject;
import java.util.List;

import static net.adamcin.oakpal.core.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.core.JavaxJson.hasNonNull;

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
    public static final String CONFIG_RULES = "rules";
    public static final String CONFIG_DENY_ALL = "denyAll";

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        List<Rule> rules = Rule.fromJsonArray(arrayOrEmpty(config, CONFIG_RULES));

        final boolean denyAll = hasNonNull(config, CONFIG_DENY_ALL) && config.getBoolean(CONFIG_DENY_ALL);

        return new Check(rules, denyAll);
    }

    static final class Check extends SimpleProgressCheck {
        private final List<Rule> rules;
        private final boolean denyAll;

        Check(final List<Rule> rules, final boolean denyAll) {
            this.rules = rules;
            this.denyAll = denyAll;
        }

        @Override
        public String getCheckName() {
            return Subpackages.class.getSimpleName();
        }

        @Override
        public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
            if (denyAll) {
                reportViolation(Violation.Severity.MAJOR,
                        String.format("subpackage %s included by %s. no subpackages are allowed.",
                                packageId, parentId), packageId);
            } else {
                final Rule lastMatch = Rule.lastMatch(rules, packageId.toString());
                if (lastMatch.isDeny()) {
                    reportViolation(Violation.Severity.MAJOR,
                            String.format("subpackage %s included by %s matches deny pattern %s",
                                    packageId.toString(), parentId.toString(),
                                    lastMatch.getPattern().pattern()), packageId);
                }
            }
        }
    }


}
