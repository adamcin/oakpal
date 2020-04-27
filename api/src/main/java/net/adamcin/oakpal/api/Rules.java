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

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Factory methods and defaults for {@link Rule}.
 *
 * @since 2.0.0
 */
public final class Rules {
    /**
     * Constructor.
     */
    private Rules() {
        /* no constructor */
    }

    static final Pattern PATTERN_MATCH_ALL = Pattern.compile(".*");
    /**
     * A default DENY rule that matches everything.
     */
    public static final Rule DEFAULT_DENY = new Rule(RuleType.DENY, PATTERN_MATCH_ALL);
    /**
     * A default ALLOW rule that matches everything.
     */
    public static final Rule DEFAULT_ALLOW = new Rule(RuleType.ALLOW, PATTERN_MATCH_ALL);
    /**
     * A default EXCLUDE rule that matches everything.
     */
    public static final Rule DEFAULT_EXCLUDE = new Rule(RuleType.EXCLUDE, PATTERN_MATCH_ALL);
    /**
     * A default INCLUDE rule that matches everything.
     */
    public static final Rule DEFAULT_INCLUDE = new Rule(RuleType.INCLUDE, PATTERN_MATCH_ALL);

    /**
     * Conveniently creates a list of Rules from the conventional use case of a JSON array containing a list of rule
     * JSON objects to be evaluated in sequence.
     *
     * @param rulesArray a JSON array where calling {@link #fromJson(JsonObject)} on each element will construct a
     *                   valid {@link Rule}
     * @return a list of rules to be evaluated in sequence.
     */
    public static List<Rule> fromJsonArray(final JsonArray rulesArray) {
        return JavaxJson.mapArrayOfObjects(rulesArray, Rules::fromJson);
    }

    /**
     * Construct a single rule from a JSON object with keys {@code type} and {@code pattern}.
     *
     * @param ruleJson a single rule config object
     * @return a new rule
     */
    public static Rule fromJson(final JsonObject ruleJson) {
        return new Rule(RuleType.fromName(ruleJson.getString(Rule.keys().type())),
                Pattern.compile(ruleJson.getString(Rule.keys().pattern())));
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
        return lastMatch(rules, value, Rules::fuzzyDefaultInclude);
    }
}
