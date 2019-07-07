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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.json.JsonArray;
import javax.json.JsonObject;

import net.adamcin.oakpal.core.JavaxJson;

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
public final class Rule implements JavaxJson.ObjectConvertible {
    static final Pattern PATTERN_MATCH_ALL = Pattern.compile(".*");

    /**
     * A default INCLUDE rule that matches everything.
     */
    public static final Rule DEFAULT_INCLUDE = new Rule(RuleType.INCLUDE, PATTERN_MATCH_ALL);

    /**
     * A default EXCLUDE rule that matches everything.
     */
    public static final Rule DEFAULT_EXCLUDE = new Rule(RuleType.EXCLUDE, PATTERN_MATCH_ALL);

    /**
     * A default ALLOW rule that matches everything.
     */
    public static final Rule DEFAULT_ALLOW = new Rule(RuleType.ALLOW, PATTERN_MATCH_ALL);

    /**
     * A default DENY rule that matches everything.
     */
    public static final Rule DEFAULT_DENY = new Rule(RuleType.DENY, PATTERN_MATCH_ALL);

    public static final String CONFIG_TYPE = "type";
    public static final String CONFIG_PATTERN = "pattern";
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
        return JavaxJson.key(CONFIG_TYPE, getType().name()).key(CONFIG_PATTERN, getPattern().pattern()).get();
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

    public enum RuleType {
        INCLUDE, EXCLUDE, ALLOW, DENY;

        public static RuleType fromName(final String name) {
            for (RuleType value : RuleType.values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("RuleType not recognized: " + name);
        }
    }

    /**
     * Conveniently creates a list of Rules from the conventional use case of a JSON array containing a list of rule
     * JSON objects to be evaluated in sequence.
     *
     * @param rulesArray a JSON array where calling {@link #fromJson(JsonObject)} on each element will construct a
     *                   valid {@link Rule}
     * @return a list of rules to be evaluated in sequence.
     */
    public static List<Rule> fromJsonArray(final JsonArray rulesArray) {
        return JavaxJson.mapArrayOfObjects(rulesArray, Rule::fromJson);
    }

    /**
     * Construct a single rule from a JSON object with keys {@code type} and {@code pattern}.
     *
     * @param ruleJson a single rule config object
     * @return a new rule
     */
    public static Rule fromJson(final JsonObject ruleJson) {
        return new Rule(Rule.RuleType.fromName(ruleJson.getString(CONFIG_TYPE)),
                Pattern.compile(ruleJson.getString(CONFIG_PATTERN)));
    }

    /**
     * Return {@link #DEFAULT_ALLOW}, unless first element in rules list is an explicit allow, in which
     * case return {@link #DEFAULT_DENY}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_ALLOW}, but sometimes {@link #DEFAULT_DENY}
     */
    public static Rule fuzzyDefaultAllow(final List<Rule> rules) {
        return fuzzyDefaultInclude(rules);
    }

    /**
     * Return {@link #DEFAULT_DENY}, unless first element in rules list is an explicit deny, in which
     * case return {@link #DEFAULT_ALLOW}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_EXCLUDE}, but sometimes {@link #DEFAULT_INCLUDE}
     */
    public static Rule fuzzyDefaultDeny(final List<Rule> rules) {
        return fuzzyDefaultExclude(rules);
    }

    /**
     * Return {@link #DEFAULT_INCLUDE}, unless first element in rules list is an explicit include, in which
     * case return {@link #DEFAULT_EXCLUDE}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_INCLUDE}, but sometimes {@link #DEFAULT_EXCLUDE}
     */
    public static Rule fuzzyDefaultInclude(final List<Rule> rules) {
        if (rules != null && !rules.isEmpty() && rules.get(0).isInclude()) {
            return DEFAULT_EXCLUDE;
        }
        return DEFAULT_INCLUDE;
    }

    /**
     * Return {@link #DEFAULT_EXCLUDE}, unless first element in rules list is an explicit exclude, in which
     * case return {@link #DEFAULT_INCLUDE}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_EXCLUDE}, but sometimes {@link #DEFAULT_INCLUDE}
     */
    public static Rule fuzzyDefaultExclude(final List<Rule> rules) {
        if (rules != null && !rules.isEmpty() && rules.get(0).isExclude()) {
            return DEFAULT_INCLUDE;
        }
        return DEFAULT_EXCLUDE;
    }

    /**
     * Evaluate the provided list of rules against the String value using the provided selector function to select the
     * default rule when none match.
     *
     * @param rules         a list of rules to match against the value. the last one to match, if any, is returned.
     * @param value         the string value to match against.
     * @param selectDefault a function to select the default rule based on the specified list of rules.
     * @return the last rule in the list that matches the value, or a default rule
     */
    public static Rule lastMatch(final List<Rule> rules,
                                 final String value,
                                 final Function<List<Rule>, Rule> selectDefault) {
        Rule lastMatched = Optional.ofNullable(selectDefault).map(func -> func.apply(rules)).orElse(DEFAULT_INCLUDE);
        for (Rule rule : rules) {
            if (rule.matches(value)) {
                lastMatched = rule;
            }
        }
        return lastMatched;
    }

    /**
     * Evaluate the provided list of rules against the String value, using {@link #fuzzyDefaultInclude(List)} to select
     * the default rule when none match.
     *
     * @param rules a list of rules to match against the value. the last one to match, if any, is returned.
     * @param value the string value to match against.
     * @return the last rule in the list that matches the value, or a default rule
     */
    public static Rule lastMatch(final List<Rule> rules, final String value) {
        return lastMatch(rules, value, Rule::fuzzyDefaultInclude);
    }

}
