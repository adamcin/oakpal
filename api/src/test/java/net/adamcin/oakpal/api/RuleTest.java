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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import net.adamcin.oakpal.api.Rule;
import org.junit.Test;

public class RuleTest {

    @Test
    public void testConstruct() {
        boolean npeForRuleType = false;
        try {
            Rule rule = new Rule(null, Pattern.compile(".*"));
        } catch (final NullPointerException npe) {
            npeForRuleType = true;
        }

        assertTrue("null ruleType should throw NPE immediately", npeForRuleType);

        boolean npeForPattern = false;
        try {
            Rule rule = new Rule(Rule.RuleType.DENY, null);
        } catch (final NullPointerException npe) {
            npeForPattern = true;
        }

        assertTrue("null pattern should throw NPE immediately", npeForPattern);
    }

    @Test
    public void testRuleType() {
        boolean illegalForUnknown = false;
        try {
            Rule.RuleType.fromName("not a thing");
        } catch (IllegalArgumentException iae) {
            illegalForUnknown = true;
        }

        assertTrue("unknown ruletype name should throw IllegalArgumentException immediately",
                illegalForUnknown);
    }

    @Test
    public void testEquals() {
        final Rule newAllow = new Rule(Rule.DEFAULT_ALLOW.getType(), Rule.DEFAULT_ALLOW.getPattern());
        assertEquals("DEFAULT_ALLOW should equal new rule with same params as DEFAULT_ALLOW",
                Rule.DEFAULT_ALLOW, newAllow);
        assertNotEquals("DEFAULT_ALLOW should not equal new ALLOW rule with different pattern",
                Rule.DEFAULT_ALLOW, new Rule(Rule.DEFAULT_ALLOW.getType(), Pattern.compile("foo")));
        assertNotEquals("DEFAULT_DENY should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rule.DEFAULT_DENY, newAllow);
        assertNotEquals("DEFAULT_INCLUDE should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rule.DEFAULT_INCLUDE, newAllow);
    }

    @Test
    public void testHashCode() {
        final Rule newAllow = new Rule(Rule.DEFAULT_ALLOW.getType(), Rule.DEFAULT_ALLOW.getPattern());
        assertEquals("hashCode(): DEFAULT_ALLOW should equal new rule with same params as DEFAULT_ALLOW",
                Rule.DEFAULT_ALLOW.hashCode(), newAllow.hashCode());
        assertNotEquals("hashCode(): DEFAULT_ALLOW should not equal new ALLOW rule with different pattern",
                Rule.DEFAULT_ALLOW.hashCode(), new Rule(Rule.DEFAULT_ALLOW.getType(), Pattern.compile("foo")).hashCode());
        assertNotEquals("hashCode(): DEFAULT_DENY should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rule.DEFAULT_DENY.hashCode(), newAllow.hashCode());
        assertNotEquals("hashCode(): DEFAULT_INCLUDE should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rule.DEFAULT_INCLUDE.hashCode(), newAllow.hashCode());
    }

    @Test
    public void testRuleDefaultAllow() {
        final Rule rule = Rule.DEFAULT_ALLOW;
        assertTrue("DEFAULT_ALLOW should be true for isInclude and isAllow",
                rule.isAllow() && rule.isInclude());
        assertTrue("DEFAULT_ALLOW should be false for isExclude and isDeny",
                !rule.isDeny() && !rule.isExclude());

        assertTrue("Rule toString for DEFAULT_ALLOW should start with 'ALLOW:'- " + rule.toString(),
                rule.toString().startsWith("ALLOW:"));
    }

    @Test
    public void testRuleDefaultInclude() {
        final Rule rule = Rule.DEFAULT_INCLUDE;
        assertTrue("DEFAULT_INCLUDE should be true for isInclude and isAllow",
                rule.isAllow() && rule.isInclude());
        assertTrue("DEFAULT_INCLUDE should be false for isExclude and isDeny",
                !rule.isDeny() && !rule.isExclude());

        assertTrue("Rule toString for DEFAULT_INCLUDE should start with 'INCLUDE:'- " + rule.toString(),
                rule.toString().startsWith("INCLUDE:"));
    }

    @Test
    public void testRuleDefaultDeny() {
        final Rule rule = Rule.DEFAULT_DENY;
        assertTrue("DEFAULT_DENY should be false for isInclude and isAllow",
                !rule.isAllow() && !rule.isInclude());
        assertTrue("DEFAULT_DENY should be true for isExclude and isDeny",
                rule.isDeny() && rule.isExclude());

        assertTrue("Rule toString for DEFAULT_DENY should start with 'DENY:'- " + rule.toString(),
                rule.toString().startsWith("DENY:"));
    }

    @Test
    public void testRuleDefaultExclude() {
        final Rule rule = Rule.DEFAULT_EXCLUDE;
        assertTrue("DEFAULT_EXCLUDE should be false for isInclude and isAllow",
                !rule.isAllow() && !rule.isInclude());
        assertTrue("DEFAULT_EXCLUDE should be true for isExclude and isDeny",
                rule.isDeny() && rule.isExclude());

        assertTrue("Rule toString for DEFAULT_EXCLUDE should start with 'EXCLUDE:'- " + rule.toString(),
                rule.toString().startsWith("EXCLUDE:"));
    }

    @Test
    public void testFuzzyDefaults() {
        final Map<String, Function<List<Rule>, Rule>> fuzzyDefaultIncFns = new HashMap<>();
        fuzzyDefaultIncFns.put("fuzzyDefaultAllow", Rule::fuzzyDefaultAllow);
        fuzzyDefaultIncFns.put("fuzzyDefaultInclude", Rule::fuzzyDefaultInclude);
        final Map<String, Function<List<Rule>, Rule>> fuzzyDefaultExcFns = new HashMap<>();
        fuzzyDefaultExcFns.put("fuzzyDefaultDeny", Rule::fuzzyDefaultDeny);
        fuzzyDefaultExcFns.put("fuzzyDefaultExclude", Rule::fuzzyDefaultExclude);

        final List<Rule> defaultIncRules = asList(Rule.DEFAULT_ALLOW, Rule.DEFAULT_INCLUDE);
        final List<Rule> defaultExcRules = asList(Rule.DEFAULT_DENY, Rule.DEFAULT_EXCLUDE);

        fuzzyDefaultIncFns.entrySet().forEach(fuzzyFnPair -> {
            final String fuzzyFnName = fuzzyFnPair.getKey();
            final Function<List<Rule>, Rule> fuzzyFn = fuzzyFnPair.getValue();
            defaultIncRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": invert default include when first rule is include-like: " + rule,
                        Rule.DEFAULT_EXCLUDE, defaultRule);
            });
            defaultExcRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": use default include when first rule is exclude-like: " + rule,
                        Rule.DEFAULT_INCLUDE, defaultRule);
            });
        });

        fuzzyDefaultExcFns.entrySet().forEach(fuzzyFnPair -> {
                final String fuzzyFnName = fuzzyFnPair.getKey();
                final Function<List<Rule>, Rule> fuzzyFn = fuzzyFnPair.getValue();
            defaultExcRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": invert default exclude when first rule is exclude-like: " + rule,
                        Rule.DEFAULT_INCLUDE, defaultRule);
            });
            defaultIncRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": use default exclude when first rule is include-like: " + rule,
                        Rule.DEFAULT_EXCLUDE, defaultRule);
            });
        });
    }
}
