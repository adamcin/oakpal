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

package net.adamcin.oakpal.api;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.json.JsonObject;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Standard Rule tuple capturing a rule type (include/exclude or allow/deny) and a regex pattern.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code type}</dt>
 * <dd>The {@link RuleType} of the rule: {@code include} or {@code exclude} (or {@code allow} or {@code deny}). The
 * meaning of this value is usually dependent on context.</dd>
 * <dt>{@code pattern}</dt>
 * <dd>A regular expression pattern matched against the full context value (start [{@code ^}] and end [{@code $}]
 * are assumed).</dd>
 * </dl>
 */
public class Rule implements JsonObjectConvertible {
    @ProviderType
    public interface JsonKeys {
        String type();

        String pattern();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String type() {
            return "type";
        }

        @Override
        public String pattern() {
            return "pattern";
        }
    };

    @NotNull
    public static JsonKeys keys() {
        return KEYS;
    }

    private final RuleType type;
    private final Pattern pattern;

    /**
     * Create a new rule.
     *
     * @param type    {@link RuleType#INCLUDE} or {@link RuleType#EXCLUDE}
     *                (or {@link RuleType#ALLOW} or {@link RuleType#DENY})
     * @param pattern a compiled regular expression pattern
     */
    public Rule(final RuleType type, final Pattern pattern) {
        if (type == null) {
            throw new NullPointerException("RuleType type");
        }
        if (pattern == null) {
            throw new NullPointerException("Pattern pattern");
        }
        this.type = type;
        this.pattern = pattern;
    }

    public RuleType getType() {
        return type;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Readability alias for {@link #isAllow()} when the rule is used in the more abstract context of scope
     * definition.
     *
     * @return true if the matched value should be included.
     */
    public boolean isInclude() {
        return getType() == RuleType.INCLUDE || getType() == RuleType.ALLOW;
    }

    /**
     * Readability alias for {@link #isDeny()} when the rule is used in the more abstract context of scope
     * definition.
     *
     * @return true if the matched value should be excluded.
     */
    public boolean isExclude() {
        return !isInclude();
    }

    /**
     * Readability alias for {@link #isInclude()} when the rule is used in the more abstract context of scope
     * definition.
     *
     * @return true if the matched value should be allowed.
     */
    public boolean isAllow() {
        return isInclude();
    }

    /**
     * Readability alias for {@link #isExclude()} when the rule is used in the more literal context of acceptable vs
     * unacceptable values.
     *
     * @return true if the matched value should be denied.
     */
    public boolean isDeny() {
        return isExclude();
    }

    /**
     * Conventional usage of the rule's {@link Pattern} to match the entirety of the provided string value. The nature
     * of the value is not considered.
     *
     * @param value a string value
     * @return true if the pattern matches
     */
    public boolean matches(String value) {
        return getPattern().matcher(value).matches();
    }

    /**
     * Serializes the rule to a {@link javax.json.JsonObject}.
     *
     * @return a JsonObject
     */
    @Override
    public JsonObject toJson() {
        return JavaxJson.key(keys().type(), getType().name()).key(keys().pattern(), getPattern().pattern()).get();
    }

    @Override
    public String toString() {
        return getType().name() + ":" + getPattern().pattern();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return type == rule.type &&
                pattern.pattern().equals(rule.pattern.pattern());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pattern.pattern());
    }

}
