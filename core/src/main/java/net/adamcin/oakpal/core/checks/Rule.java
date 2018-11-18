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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Standard Rule tuple capturing a rule type (allow/deny) and a regex pattern.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code type}</dt>
 * <dd>The {@link RuleType} of the rule: {@code allow} or {@code deny}. The meaning of this value is usually
 * dependent on context.</dd>
 * <dt>{@code pattern}</dt>
 * <dd>A regular expression pattern matched against the full context value (start [{@code ^}] and end [{@code $}]
 * are assumed).</dd>
 * </dl>
 */
public final class Rule {
    public static final Rule DEFAULT_ALLOW = new Rule(RuleType.ALLOW, Pattern.compile(".*"));
    public static final Rule DEFAULT_DENY = new Rule(RuleType.DENY, Pattern.compile(".*"));

    public static final String CONFIG_TYPE = "type";
    public static final String CONFIG_PATTERN = "pattern";
    private final RuleType type;
    private final Pattern pattern;

    private Rule(final RuleType type, final Pattern pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    /**
     * Return {@link #DEFAULT_ALLOW}, unless first element in rules list is an explicit allow, in which
     * case return {@link #DEFAULT_DENY}
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_ALLOW}, but sometimes {@link #DEFAULT_DENY}
     */
    public static Rule fuzzyDefaultAllow(final List<Rule> rules) {
        if (rules != null && !rules.isEmpty() && rules.get(0).isAllow()) {
            return DEFAULT_DENY;
        }
        return DEFAULT_ALLOW;
    }

    /**
     * Return {@link #DEFAULT_DENY}, unless first element in rules list is an explicit deny, in which
     * case return {@link #DEFAULT_ALLOW}
     *
     * @param rules rules list
     * @return usually {@link #DEFAULT_DENY}, but sometimes {@link #DEFAULT_ALLOW}
     */
    public static Rule fuzzyDefaultDeny(final List<Rule> rules) {
        if (rules != null && !rules.isEmpty() && rules.get(0).isDeny()) {
            return DEFAULT_ALLOW;
        }
        return DEFAULT_DENY;
    }

    public RuleType getType() {
        return type;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean isDeny() {
        return getType() == RuleType.DENY;
    }

    public boolean isAllow() {
        return getType() == RuleType.ALLOW;
    }

    public boolean matches(String value) {
        return getPattern().matcher(value).matches();
    }

    @Override
    public String toString() {
        return getType().name() + ":" + getPattern().pattern();
    }

    public enum RuleType {
        ALLOW("INCLUDE"), DENY("EXCLUDE");
        private final String inex;

        RuleType(final String inex) {
            this.inex = inex;
        }

        public static RuleType fromName(final String name) {
            final String ucn = name;
            for (RuleType value : RuleType.values()) {
                if (value.inex.equalsIgnoreCase(ucn) || value.name().equalsIgnoreCase(ucn)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("RuleType not recognized: " + name);
        }
    }

    public static List<Rule> fromJSON(final JSONArray rulesArray) {
        List<JSONObject> ruleJsons = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();
        Optional.ofNullable(rulesArray).map(array -> StreamSupport.stream(array.spliterator(), true)
                .filter(json -> json instanceof JSONObject)
                .map(JSONObject.class::cast).collect(Collectors.toList()))
                .ifPresent(ruleJsons::addAll);

        for (JSONObject json : ruleJsons) {
            rules.add(fromJSON(json));
        }
        return rules;
    }

    public static Rule fromJSON(final JSONObject ruleJson) {
        return new Rule(Rule.RuleType.fromName(ruleJson.getString(CONFIG_TYPE)),
                Pattern.compile(ruleJson.getString(CONFIG_PATTERN)));
    }
}
