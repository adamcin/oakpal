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

import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.Rules;
import org.osgi.annotation.versioning.ProviderType;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Rule}
 */
@Deprecated
@ProviderType
public class Rule extends net.adamcin.oakpal.api.Rule {
    /**
     * A default INCLUDE rule that matches everything.
     *
     * @deprecated 2.0.0 use {@link Rules#DEFAULT_INCLUDE}
     */
    @Deprecated
    public static final Rule DEFAULT_INCLUDE = new Rule(RuleType.INCLUDE,
            Rules.DEFAULT_INCLUDE.getPattern());

    /**
     * A default EXCLUDE rule that matches everything.
     *
     * @deprecated 2.0.0 use {@link Rules#DEFAULT_EXCLUDE}
     */
    @Deprecated
    public static final Rule DEFAULT_EXCLUDE = new Rule(RuleType.EXCLUDE,
            Rules.DEFAULT_INCLUDE.getPattern());

    /**
     * A default ALLOW rule that matches everything.
     *
     * @deprecated 2.0.0 use {@link Rules#DEFAULT_ALLOW}
     */
    @Deprecated
    public static final Rule DEFAULT_ALLOW = new Rule(RuleType.ALLOW,
            Rules.DEFAULT_INCLUDE.getPattern());

    /**
     * A default DENY rule that matches everything.
     *
     * @deprecated 2.0.0 use {@link Rules#DEFAULT_DENY}
     */
    @Deprecated
    public static final Rule DEFAULT_DENY = new Rule(RuleType.DENY,
            Rules.DEFAULT_INCLUDE.getPattern());

    /**
     * @deprecated 2.0.0 use {@code Rule.keys().type()}
     */
    @Deprecated
    public static final String CONFIG_TYPE = keys().type();

    /**
     * @deprecated 2.0.0 use {@code Rule.keys().pattern()}
     */
    @Deprecated
    public static final String CONFIG_PATTERN = keys().pattern();

    public Rule(final RuleType type, final Pattern pattern) {
        super(type.ruleType, pattern);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return getType() == rule.getType() &&
                getPattern().pattern().equals(rule.getPattern().pattern());
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.RuleType}
     */
    @Deprecated
    public enum RuleType {
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.RuleType#INCLUDE}
         */
        @Deprecated
        INCLUDE(net.adamcin.oakpal.api.RuleType.INCLUDE),
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.RuleType#EXCLUDE}
         */
        @Deprecated
        EXCLUDE(net.adamcin.oakpal.api.RuleType.EXCLUDE),
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.RuleType#ALLOW}
         */
        @Deprecated
        ALLOW(net.adamcin.oakpal.api.RuleType.ALLOW),
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.RuleType#DENY}
         */
        @Deprecated
        DENY(net.adamcin.oakpal.api.RuleType.DENY);

        private final net.adamcin.oakpal.api.RuleType ruleType;

        RuleType(final net.adamcin.oakpal.api.RuleType ruleType) {
            this.ruleType = ruleType;
        }

        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.RuleType#fromName(String)}
         */
        @Deprecated
        public static RuleType fromName(final String name) {
            final net.adamcin.oakpal.api.RuleType ruleType = net.adamcin.oakpal.api.RuleType.fromName(name);
            for (RuleType value : values()) {
                if (value.ruleType == ruleType) {
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
     *                   valid {@link net.adamcin.oakpal.api.Rule}
     * @return a list of rules to be evaluated in sequence.
     * @deprecated 2.0.0 use {@link Rules#fromJsonArray(JsonArray)}
     */
    @Deprecated
    public static List<Rule> fromJsonArray(final JsonArray rulesArray) {
        return JavaxJson.mapArrayOfObjects(rulesArray, Rule::fromJson);
    }

    /**
     * Construct a single rule from a JSON object with keys {@code type} and {@code pattern}.
     *
     * @param ruleJson a single rule config object
     * @return a new rule
     * @deprecated 2.0.0 use {@link Rules#fromJson(JsonObject)}
     */
    @Deprecated
    public static Rule fromJson(final JsonObject ruleJson) {
        return new Rule(RuleType.fromName(ruleJson.getString(keys().type())),
                Pattern.compile(ruleJson.getString(keys().pattern())));
    }

    /**
     * Return {@link #DEFAULT_ALLOW}, unless first element in rules list is an explicit allow, in which
     * case return {@link #DEFAULT_DENY}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_ALLOW}, but sometimes {@link #DEFAULT_DENY}
     * @deprecated 2.0.0 use {@link Rules#fuzzyDefaultAllow(List)}
     */
    @Deprecated
    public static Rule fuzzyDefaultAllow(final List<Rule> rules) {
        return fuzzyDefaultInclude(rules);
    }

    /**
     * Return {@link #DEFAULT_DENY}, unless first element in rules list is an explicit deny, in which
     * case return {@link #DEFAULT_ALLOW}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_EXCLUDE}, but sometimes {@link #DEFAULT_INCLUDE}
     * @deprecated 2.0.0 use {@link Rules#fuzzyDefaultDeny(List)}
     */
    @Deprecated
    public static Rule fuzzyDefaultDeny(final List<Rule> rules) {
        return fuzzyDefaultExclude(rules);
    }

    /**
     * Return {@link #DEFAULT_INCLUDE}, unless first element in rules list is an explicit include, in which
     * case return {@link #DEFAULT_EXCLUDE}.
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_INCLUDE}, but sometimes {@link #DEFAULT_EXCLUDE}
     * @deprecated 2.0.0 use {@link Rules#fuzzyDefaultInclude(List)}
     */
    @Deprecated
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
     * @deprecated 2.0.0 use {@link Rules#fuzzyDefaultExclude(List)}
     */
    @Deprecated
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
     * @deprecated 2.0.0 use {@link Rules#lastMatch(List, String, Function)}
     */
    @Deprecated
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
     * Evaluate the provided list of rules against the String value, using {@link Rules#fuzzyDefaultInclude(List)} to select
     * the default rule when none match.
     *
     * @param rules a list of rules to match against the value. the last one to match, if any, is returned.
     * @param value the string value to match against.
     * @return the last rule in the list that matches the value, or a default rule
     * @deprecated 2.0.0 use {@link Rules#lastMatch(List, String)}
     */
    @Deprecated
    public static Rule lastMatch(final List<Rule> rules, final String value) {
        return lastMatch(rules, value, Rule::fuzzyDefaultInclude);
    }
}
