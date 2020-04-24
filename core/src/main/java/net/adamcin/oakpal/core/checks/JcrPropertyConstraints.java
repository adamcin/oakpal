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

import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleViolation;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.api.JavaxJson.mapArrayOfObjects;

/**
 * Encapsulation of constraints on a JCR property for the {@link JcrProperties} check.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code name}</dt>
 * <dd>The property name to apply restrictions to.</dd>
 * <dt>{@code denyIfAbsent}</dt>
 * <dd>Report violation if this property is absent on nodes within scope.</dd>
 * <dt>{@code denyIfPresent}</dt>
 * <dd>Report violation if this property is present on nodes within scope.</dd>
 * <dt>{@code denyIfMultivalued}</dt>
 * <dd>Report violation if this property has more than one value.</dd>
 * <dt>{@code requireType}</dt>
 * <dd>Require a specific property type. Must be a valid name for {@link PropertyType#valueFromName(String)}, e.g.
 * Binary, Boolean, Date, Decimal, Double, Long, Name, Path, Reference, String, WeakReference, or URI.</dd>
 * <dt>{@code valueRules}</dt>
 * <dd>A list of patterns to match against string values of this property. All rules are applied in sequence to each
 * value of the property. If the type of the last rule to match any value is DENY, a violation is reported.</dd>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link Severity#MAJOR}) specify the severity if a violation is
 * reported by this set of constraints.</dd>
 * </dl>
 */
public final class JcrPropertyConstraints {
    @ProviderType
    public interface JsonKeys {
        String name();

        String denyIfAbsent();

        String denyIfPresent();

        String denyIfMultivalued();

        String requireType();

        String valueRules();

        String severity();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String name() {
            return "name";
        }

        @Override
        public String denyIfAbsent() {
            return "denyIfAbsent";
        }

        @Override
        public String denyIfPresent() {
            return "denyIfPresent";
        }

        @Override
        public String denyIfMultivalued() {
            return "denyIfMultivalued";
        }

        @Override
        public String requireType() {
            return "requireType";
        }

        @Override
        public String valueRules() {
            return "valueRules";
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
    public static final String CONFIG_NAME = keys().name();
    @Deprecated
    public static final String CONFIG_DENY_IF_ABSENT = keys().denyIfAbsent();
    @Deprecated
    public static final String CONFIG_DENY_IF_PRESENT = keys().denyIfPresent();
    @Deprecated
    public static final String CONFIG_DENY_IF_MULTIVALUED = keys().denyIfMultivalued();
    @Deprecated
    public static final String CONFIG_REQUIRE_TYPE = keys().requireType();
    @Deprecated
    public static final String CONFIG_VALUE_RULES = keys().valueRules();
    @Deprecated
    public static final String CONFIG_SEVERITY = keys().severity();

    public static final Severity DEFAULT_SEVERITY = Severity.MAJOR;

    public static Function<JsonObject, JcrPropertyConstraints>
    fromJson(@NotNull final Supplier<ResourceBundle> resourceBundleSupplier) {
        return checkJson -> {
            final String name = checkJson.getString(keys().name());
            final boolean denyIfAbsent = hasNonNull(checkJson, keys().denyIfAbsent())
                    && checkJson.getBoolean(keys().denyIfAbsent());
            final boolean denyIfPresent = hasNonNull(checkJson, keys().denyIfPresent())
                    && checkJson.getBoolean(keys().denyIfPresent());
            final boolean denyIfMultivalued = hasNonNull(checkJson, keys().denyIfMultivalued())
                    && checkJson.getBoolean(keys().denyIfMultivalued());
            final String requireType = checkJson.getString(keys().requireType(), null);
            final List<Rule> valueRules = Rule.fromJsonArray(arrayOrEmpty(checkJson, keys().valueRules()));
            final Severity severity = Severity
                    .valueOf(checkJson.getString(keys().severity(), DEFAULT_SEVERITY.name()).toUpperCase());

            return new JcrPropertyConstraints(name, denyIfAbsent, denyIfPresent, denyIfMultivalued, requireType, valueRules,
                    severity, resourceBundleSupplier);
        };
    }

    public static List<JcrPropertyConstraints> fromJsonArray(
            @NotNull final Supplier<ResourceBundle> resourceBundleSupplier,
            final JsonArray rulesArray) {
        return mapArrayOfObjects(rulesArray, fromJson(resourceBundleSupplier));
    }

    private final String name;
    private final boolean denyIfAbsent;
    private final boolean denyIfPresent;
    private final boolean denyIfMultivalued;
    private final String requireType;
    private final List<Rule> valueRules;
    private final Severity severity;
    private final Supplier<ResourceBundle> resourceBundleSupplier;

    public JcrPropertyConstraints(final String name,
                                  final boolean denyIfAbsent,
                                  final boolean denyIfPresent,
                                  final boolean denyIfMultivalued,
                                  final String requireType,
                                  final List<Rule> valueRules,
                                  final Severity severity,
                                  final Supplier<ResourceBundle> resourceBundleSupplier) {
        this.name = name;
        this.denyIfAbsent = denyIfAbsent;
        this.denyIfPresent = denyIfPresent;
        this.denyIfMultivalued = denyIfMultivalued;
        this.requireType = requireType;
        this.valueRules = valueRules;
        this.severity = severity;
        this.resourceBundleSupplier = resourceBundleSupplier;
    }

    public String getName() {
        return name;
    }

    public boolean isDenyIfAbsent() {
        return denyIfAbsent;
    }

    public boolean isDenyIfPresent() {
        return denyIfPresent;
    }

    public boolean isDenyIfMultivalued() {
        return denyIfMultivalued;
    }

    public String getRequireType() {
        return requireType;
    }

    public List<Rule> getValueRules() {
        return valueRules;
    }

    public Severity getSeverity() {
        return severity;
    }

    @NotNull
    String getString(@NotNull final String key) {
        final ResourceBundle resourceBundle = resourceBundleSupplier.get();
        if (resourceBundle.containsKey(key)) {
            return resourceBundle.getString(key);
        } else {
            return key;
        }
    }

    Violation constructViolation(final PackageId packageId, final Node node, final String reason)
            throws RepositoryException {
        return new SimpleViolation(getSeverity(),
                MessageFormat.format("{0} (t: {1}, m: {2}): {3} -> {4}",
                        node.getPath(),
                        node.getPrimaryNodeType().getName(),
                        Stream.of(node.getMixinNodeTypes())
                                .map(NodeTypeDefinition::getName)
                                .collect(Collectors.toList()),
                        reason,
                        getName()),
                packageId);
    }

    Optional<Violation> evaluate(final PackageId packageId, final Node node) throws RepositoryException {
        if (!node.hasProperty(getName())) {
            if (isDenyIfAbsent()) {
                return Optional.of(constructViolation(packageId, node, getString("property absent")));
            }
        } else {
            if (isDenyIfPresent()) {
                return Optional.of(constructViolation(packageId, node, getString("property present")));
            }

            Property property = node.getProperty(getName());

            if (isDenyIfMultivalued() && property.isMultiple()) {
                return Optional.of(constructViolation(packageId, node, getString("property is multivalued")));
            }

            if (getRequireType() != null && !getRequireType().isEmpty()
                    && !getRequireType().equals(PropertyType.nameFromValue(property.getType()))) {
                return Optional.of(constructViolation(packageId, node,
                        MessageFormat.format(getString("required type mismatch: {0} != {1}"),
                                PropertyType.nameFromValue(property.getType()), getRequireType())));
            }

            List<String> values = new ArrayList<>();
            if (property.isMultiple()) {
                for (Value value : property.getValues()) {
                    values.add(value.getString());
                }
            } else {
                values.add(property.getString());
            }

            for (String value : values) {
                final Rule lastMatch = Rule.lastMatch(getValueRules(), value);
                if (lastMatch.isDeny()) {
                    return Optional.of(constructViolation(packageId, node,
                            MessageFormat.format(getString("value {0} denied by pattern {1}"),
                                    value, lastMatch.getPattern().pattern())));
                }
            }
        }

        return Optional.empty();
    }
}
