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
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
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
 *         },{
 *             "type": "allow",
 *             "pattern": "/etc/tags/acme(/.*)?"
 *         }],
 *         "denyAllDeletes": true
 *     }
 * </pre>
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code rules}</dt>
 * <dd>A list of {@link Rule} elements. Rules are evaluated top-to-bottom. The type of the last rule to match is
 * the effective action taken for the element. Any affected path matching a DENY rule will be reported as a
 * violation.</dd>
 * <dt>{@code denyAllDeletes}</dt>
 * <dd>By default any affected path matching a DENY rule will be reported as a violation regardless of the nature
 * of the change. Set this true to also report a violation if ANY delete is detected, because that is generally
 * indicative of an existing path (forced root or preInstallPackage) being inadvertently captured by this package's
 * workspace filter.</dd>
 * </dl>
 */
public class Paths implements ProgressCheckFactory {
    public static final String CONFIG_RULES = "rules";
    public static final String CONFIG_DENY_ALL_DELETES = "denyAllDeletes";

    public class Check extends SimpleProgressCheck {
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

            Rule lastMatch = Rule.fuzzyDefaultAllow(rules);
            for (Rule rule : rules) {
                if (rule.matches(path)) {
                    lastMatch = rule;
                }
            }

            if (lastMatch.isDeny()) {
                reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("imported path %s matches deny pattern %s", path,
                                lastMatch.getPattern().pattern()), packageId));
            }
        }

        @Override
        public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
                throws RepositoryException {
            if (this.denyAllDeletes) {
                reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("deleted path %s. All deletions are denied.", path), packageId));
            } else {
                Rule lastMatch = Rule.fuzzyDefaultAllow(rules);
                for (Rule rule : rules) {
                    if (rule.matches(path)) {
                        lastMatch = rule;
                    }
                }

                if (lastMatch.isDeny()) {
                    reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("deleted path %s matches deny rule %s", path,
                                    lastMatch.getPattern().pattern()), packageId));
                }
            }
        }
    }

    @Override
    public ProgressCheck newInstance(final JSONObject config) throws Exception {
        List<Rule> rules = Rule.fromJSON(config.optJSONArray(CONFIG_RULES));

        final boolean denyAllDeletes = config.has(CONFIG_DENY_ALL_DELETES)
                && config.optBoolean(CONFIG_DENY_ALL_DELETES);

        return new Check(rules, denyAllDeletes);
    }
}
