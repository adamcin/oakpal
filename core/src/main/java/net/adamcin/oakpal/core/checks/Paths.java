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
 *         "denyAllDeletes": true,
 *         "severity": "minor"
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
 * <dt>{@code severity}</dt>
 * <dd>By default, the severity of violations created by this check is MAJOR, but can be set to MINOR or SEVERE.</dd>
 * </dl>
 */
public final class Paths implements ProgressCheckFactory {
    public static final String CONFIG_RULES = "rules";
    public static final String CONFIG_DENY_ALL_DELETES = "denyAllDeletes";
    public static final String CONFIG_SEVERITY = "severity";
    public static final Violation.Severity DEFAULT_SEVERITY = Violation.Severity.MAJOR;

    @Override
    public ProgressCheck newInstance(final JSONObject config) {
        List<Rule> rules = Rule.fromJSON(config.optJSONArray(CONFIG_RULES));

        final boolean denyAllDeletes = config.has(CONFIG_DENY_ALL_DELETES)
                && config.optBoolean(CONFIG_DENY_ALL_DELETES);

        final Violation.Severity severity = Violation.Severity.valueOf(
                config.optString(CONFIG_SEVERITY, DEFAULT_SEVERITY.name()).toUpperCase());

        return new Check(rules, denyAllDeletes, severity);
    }

    static final class Check extends SimpleProgressCheck {
        private final List<Rule> rules;
        private final boolean denyAllDeletes;
        private final Violation.Severity severity;

        Check(final List<Rule> rules, final boolean denyAllDeletes, final Violation.Severity severity) {
            this.rules = rules;
            this.denyAllDeletes = denyAllDeletes;
            this.severity = severity;
        }

        @Override
        public String getCheckName() {
            return Paths.class.getSimpleName();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {

            Rule lastMatch = Rule.lastMatch(rules, path);
            if (lastMatch.isDeny()) {
                reportViolation(severity,
                        String.format("imported path %s matches deny pattern %s", path,
                                lastMatch.getPattern().pattern()), packageId);
            }
        }

        @Override
        public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
                throws RepositoryException {
            if (this.denyAllDeletes) {
                reportViolation(severity,
                        String.format("deleted path %s. All deletions are denied.", path), packageId);
            } else {
                final Rule lastMatch = Rule.lastMatch(rules, path);
                if (lastMatch.isDeny()) {
                    reportViolation(severity,
                            String.format("deleted path %s matches deny rule %s", path,
                                    lastMatch.getPattern().pattern()), packageId);
                }
            }
        }
    }
}
