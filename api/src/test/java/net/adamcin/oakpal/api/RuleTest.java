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

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RuleTest {

    @Test(expected = NullPointerException.class)
    public void testConstruct_nullType() {
        new Rule(null, Pattern.compile(".*"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstruct_nullPattern() {
        new Rule(RuleType.DENY, null);
    }

    @Test
    public void testRuleType() {
        assertSame("expect allow", RuleType.ALLOW, RuleType.fromName("allow"));
        assertSame("expect allow", RuleType.ALLOW, RuleType.fromName("ALLOW"));
        assertSame("expect deny", RuleType.DENY, RuleType.fromName("deny"));
        assertSame("expect deny", RuleType.DENY, RuleType.fromName("DENY"));
        assertSame("expect include", RuleType.INCLUDE, RuleType.fromName("include"));
        assertSame("expect include", RuleType.INCLUDE, RuleType.fromName("INCLUDE"));
        assertSame("expect exclude", RuleType.EXCLUDE, RuleType.fromName("exclude"));
        assertSame("expect exclude", RuleType.EXCLUDE, RuleType.fromName("EXCLUDE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRuleType_throws() {
        RuleType.fromName("not a thing");
    }

    @Test
    public void testEquals() {
        final Rule newAllow = new Rule(Rules.DEFAULT_ALLOW.getType(), Rules.DEFAULT_ALLOW.getPattern());
        assertEquals("DEFAULT_ALLOW should equal new rule with same params as DEFAULT_ALLOW",
                Rules.DEFAULT_ALLOW, newAllow);
        assertNotEquals("DEFAULT_ALLOW should not equal new ALLOW rule with different pattern",
                Rules.DEFAULT_ALLOW, new Rule(Rules.DEFAULT_ALLOW.getType(), Pattern.compile("foo")));
        assertNotEquals("DEFAULT_DENY should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rules.DEFAULT_DENY, newAllow);
        assertNotEquals("DEFAULT_INCLUDE should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rules.DEFAULT_INCLUDE, newAllow);
    }

    @Test
    public void testHashCode() {
        final Rule newAllow = new Rule(Rules.DEFAULT_ALLOW.getType(), Rules.DEFAULT_ALLOW.getPattern());
        assertEquals("hashCode(): DEFAULT_ALLOW should equal new rule with same params as DEFAULT_ALLOW",
                Rules.DEFAULT_ALLOW.hashCode(), newAllow.hashCode());
        assertNotEquals("hashCode(): DEFAULT_ALLOW should not equal new ALLOW rule with different pattern",
                Rules.DEFAULT_ALLOW.hashCode(), new Rule(Rules.DEFAULT_ALLOW.getType(), Pattern.compile("foo")).hashCode());
        assertNotEquals("hashCode(): DEFAULT_DENY should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rules.DEFAULT_DENY.hashCode(), newAllow.hashCode());
        assertNotEquals("hashCode(): DEFAULT_INCLUDE should not equal new ALLOW rule with same params as DEFAULT_ALLOW",
                Rules.DEFAULT_INCLUDE.hashCode(), newAllow.hashCode());
    }

    @Test
    public void testMatches() {
        assertTrue("default matches all", Rules.DEFAULT_ALLOW.matches("/foo"));
        final Rule allowsDigits = new Rule(RuleType.ALLOW, Pattern.compile("[0-9]*"));
        assertTrue("allows digits matches digits", allowsDigits.matches("123"));
        assertFalse("allows digits doesn't match letters", allowsDigits.matches("abc"));
    }

    @Test
    public void testToJson() {
        final Rule allowsDigits = new Rule(RuleType.ALLOW, Pattern.compile("[0-9]*"));
        assertEquals("expect json", key(Rule.keys().type(), "ALLOW")
                .key(Rule.keys().pattern(), "[0-9]*").get(), allowsDigits.toJson());
    }

    @Test
    public void testFromJson() {
        final Rule allowsDigits = Rules.fromJson(key(Rule.keys().type(), "ALLOW")
                .key(Rule.keys().pattern(), "[0-9]*").get());
        assertEquals("expect type", RuleType.ALLOW, allowsDigits.getType());
        assertEquals("expect pattern", "[0-9]*", allowsDigits.getPattern().pattern());
    }

    @Test
    public void testFromJsonArray() {
        final Rule allowsDigits = Rules.fromJson(key(Rule.keys().type(), "ALLOW")
                .key(Rule.keys().pattern(), "[0-9]*").get());
        final Rule allowsLetters = Rules.fromJson(key(Rule.keys().type(), "ALLOW")
                .key(Rule.keys().pattern(), "[a-z]*").get());

        final List<Rule> expectRules = Arrays.asList(allowsDigits, allowsLetters);
        assertEquals("exect same rules", expectRules, Rules.fromJsonArray(arr()
                .val(key(Rule.keys().type(), "allow").key(Rule.keys().pattern(), "[0-9]*"))
                .val(key(Rule.keys().type(), "allow").key(Rule.keys().pattern(), "[a-z]*"))
                .get()));
    }

    @Test
    public void testRuleDefaultAllow() {
        final Rule rule = Rules.DEFAULT_ALLOW;
        assertTrue("DEFAULT_ALLOW should be true for isInclude and isAllow",
                rule.isAllow() && rule.isInclude());
        assertTrue("DEFAULT_ALLOW should be false for isExclude and isDeny",
                !rule.isDeny() && !rule.isExclude());

        assertTrue("Rule toString for DEFAULT_ALLOW should start with 'ALLOW:'- " + rule.toString(),
                rule.toString().startsWith("ALLOW:"));
    }

    @Test
    public void testRuleDefaultInclude() {
        final Rule rule = Rules.DEFAULT_INCLUDE;
        assertTrue("DEFAULT_INCLUDE should be true for isInclude and isAllow",
                rule.isAllow() && rule.isInclude());
        assertTrue("DEFAULT_INCLUDE should be false for isExclude and isDeny",
                !rule.isDeny() && !rule.isExclude());

        assertTrue("Rule toString for DEFAULT_INCLUDE should start with 'INCLUDE:'- " + rule.toString(),
                rule.toString().startsWith("INCLUDE:"));
    }

    @Test
    public void testRuleDefaultDeny() {
        final Rule rule = Rules.DEFAULT_DENY;
        assertTrue("DEFAULT_DENY should be false for isInclude and isAllow",
                !rule.isAllow() && !rule.isInclude());
        assertTrue("DEFAULT_DENY should be true for isExclude and isDeny",
                rule.isDeny() && rule.isExclude());

        assertTrue("Rule toString for DEFAULT_DENY should start with 'DENY:'- " + rule.toString(),
                rule.toString().startsWith("DENY:"));
    }

    @Test
    public void testRuleDefaultExclude() {
        final Rule rule = Rules.DEFAULT_EXCLUDE;
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
        fuzzyDefaultIncFns.put("fuzzyDefaultAllow", Rules::fuzzyDefaultAllow);
        fuzzyDefaultIncFns.put("fuzzyDefaultInclude", Rules::fuzzyDefaultInclude);
        final Map<String, Function<List<Rule>, Rule>> fuzzyDefaultExcFns = new HashMap<>();
        fuzzyDefaultExcFns.put("fuzzyDefaultDeny", Rules::fuzzyDefaultDeny);
        fuzzyDefaultExcFns.put("fuzzyDefaultExclude", Rules::fuzzyDefaultExclude);

        final List<Rule> defaultIncRules = asList(Rules.DEFAULT_ALLOW, Rules.DEFAULT_INCLUDE);
        final List<Rule> defaultExcRules = asList(Rules.DEFAULT_DENY, Rules.DEFAULT_EXCLUDE);

        fuzzyDefaultIncFns.entrySet().forEach(fuzzyFnPair -> {
            final String fuzzyFnName = fuzzyFnPair.getKey();
            final Function<List<Rule>, Rule> fuzzyFn = fuzzyFnPair.getValue();
            defaultIncRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": invert default include when first rule is include-like: " + rule,
                        Rules.DEFAULT_EXCLUDE, defaultRule);
            });
            defaultExcRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": use default include when first rule is exclude-like: " + rule,
                        Rules.DEFAULT_INCLUDE, defaultRule);
            });
        });

        fuzzyDefaultExcFns.entrySet().forEach(fuzzyFnPair -> {
            final String fuzzyFnName = fuzzyFnPair.getKey();
            final Function<List<Rule>, Rule> fuzzyFn = fuzzyFnPair.getValue();
            defaultExcRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": invert default exclude when first rule is exclude-like: " + rule,
                        Rules.DEFAULT_INCLUDE, defaultRule);
            });
            defaultIncRules.forEach(rule -> {
                final Rule defaultRule = fuzzyFn.apply(singletonList(rule));
                assertEquals(fuzzyFnName + ": use default exclude when first rule is include-like: " + rule,
                        Rules.DEFAULT_EXCLUDE, defaultRule);
            });
        });
    }

    @Test
    public void testLastMatch() {
        final Rule allowsDigits = Rules.fromJson(key(Rule.keys().type(), "ALLOW")
                .key(Rule.keys().pattern(), "[0-9]*").get());
        final Rule deniesLetters = Rules.fromJson(key(Rule.keys().type(), "DENY")
                .key(Rule.keys().pattern(), "[a-z]*").get());

        final List<Rule> expectRules = Arrays.asList(allowsDigits, deniesLetters);
        final Rule adHocDefault = new Rule(RuleType.ALLOW, Pattern.compile("\\.\\."));
        final Rule matched0 = Rules.lastMatch(expectRules, "...", rules -> adHocDefault);
        assertSame("expect specific default", adHocDefault, matched0);

        final Rule matched1 = Rules.lastMatch(expectRules, "...", null);
        assertSame("expect globbal default", Rules.DEFAULT_INCLUDE, matched1);

        final Rule matchedDigits = Rules.lastMatch(expectRules, "123");
        assertSame("expect digits rule", allowsDigits, matchedDigits);

        final Rule matchedAlpha = Rules.lastMatch(expectRules, "abc");
        assertSame("expect alpha rule", deniesLetters, matchedAlpha);

    }
}
