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
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.List;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;

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
    @ProviderType
    public interface JsonKeys {
        String rules();

        String denyAllDeletes();

        String severity();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String rules() {
            return "rules";
        }

        @Override
        public String denyAllDeletes() {
            return "denyAllDeletes";
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
    public static final String CONFIG_RULES = keys().rules();
    @Deprecated
    public static final String CONFIG_DENY_ALL_DELETES = keys().denyAllDeletes();
    @Deprecated
    public static final String CONFIG_SEVERITY = keys().severity();

    public static final Severity DEFAULT_SEVERITY = Severity.MAJOR;

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        List<Rule> rules = Rule.fromJsonArray(arrayOrEmpty(config, keys().rules()));

        final boolean denyAllDeletes = hasNonNull(config, keys().denyAllDeletes())
                && config.getBoolean(keys().denyAllDeletes());

        final Severity severity = Severity.valueOf(
                config.getString(keys().severity(), DEFAULT_SEVERITY.name()).toUpperCase());

        return new Check(rules, denyAllDeletes, severity);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<Paths> {
        private final List<Rule> rules;
        private final boolean denyAllDeletes;
        private final Severity severity;

        Check(final List<Rule> rules, final boolean denyAllDeletes, final Severity severity) {
            super(Paths.class);
            this.rules = rules;
            this.denyAllDeletes = denyAllDeletes;
            this.severity = severity;
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node,
                                 final PathAction action)
                throws RepositoryException {

            Rule lastMatch = Rule.lastMatch(rules, path);
            if (lastMatch.isDeny()) {
                reporting(violation -> violation
                        .withSeverity(severity)
                        .withPackage(packageId)
                        .withDescription("imported path {0} matches deny pattern {1}")
                        .withArgument(path, lastMatch.getPattern().pattern()));
            }
        }

        @Override
        public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
                throws RepositoryException {
            if (this.denyAllDeletes) {
                reporting(violation -> violation
                        .withSeverity(severity)
                        .withPackage(packageId)
                        .withDescription("deleted path {0}. All deletions are denied.")
                        .withArgument(path));
            } else {
                final Rule lastMatch = Rule.lastMatch(rules, path);
                if (lastMatch.isDeny()) {
                    reporting(violation -> violation
                            .withSeverity(severity)
                            .withPackage(packageId)
                            .withDescription("deleted path {0} matches deny rule {1}")
                            .withArgument(path, lastMatch.getPattern().pattern()));
                }
            }
        }
    }
}
