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

import java.util.List;
import java.util.Optional;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.PackageCheck;
import net.adamcin.oakpal.core.PackageCheckFactory;
import net.adamcin.oakpal.core.SimplePackageCheck;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.json.JSONObject;

/**
 * Deny path imports/deletes by regular expression.
 * <p>
 * Example config:
 * <pre>
 *     "config": {
 *         "rules": [{
 *             "type": "deny",
 *             "pattern": "/etc/tags(/.*)?"
 *         }],
 *         "denyAllDeletes": true
 *     }
 * </pre>
 */
public class Paths implements PackageCheckFactory {
    public static final String CONFIG_RULES = "rules";
    public static final String CONFIG_DENY_ALL_DELETES = "denyAllDeletes";

    public class Check extends SimplePackageCheck {
        private final List<Rule> rules;
        private final boolean denyAllDeletes;

        public Check(final List<Rule> rules, final boolean denyAllDeletes) {
            this.rules = rules;
            this.denyAllDeletes = denyAllDeletes;
        }

        @Override
        public String getCheckName() {
            return Paths.this.getClass().getSimpleName();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {
            Rule lastMatch = null;

            for (Rule rule : rules) {
                if (rule.getPattern().matcher(path).matches()) {
                    lastMatch = rule;
                }
            }

            Optional.ofNullable(lastMatch).filter(rule -> rule.getType() == Rule.RuleType.DENY).ifPresent(rule -> {
                reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("imported path %s matches deny pattern %s", path,
                                rule.getPattern().pattern()), packageId));
            });
        }

        @Override
        public void deletedPath(final PackageId packageId, final String path) {
            if (this.denyAllDeletes) {
                reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("deleted path %s. All deletions are denied.", path), packageId));
            } else {
                Rule lastMatch = null;
                for (Rule rule : rules) {
                    if (rule.getPattern().matcher(path).matches()) {
                        lastMatch = rule;
                    }
                }

                Optional.ofNullable(lastMatch).filter(rule -> rule.getType() == Rule.RuleType.DENY).ifPresent(rule -> {
                    reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("deleted path %s matches deny rule %s", path,
                                    rule.getPattern().pattern()), packageId));
                });
            }
        }
    }

    @Override
    public PackageCheck newInstance(final JSONObject config) throws Exception {
        List<Rule> rules = Rule.fromJSON(config.optJSONArray(CONFIG_RULES));

        final boolean denyAllDeletes = config.has(CONFIG_DENY_ALL_DELETES)
                && config.optBoolean(CONFIG_DENY_ALL_DELETES);

        return new Check(rules, denyAllDeletes);
    }
}
