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

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathActionTest {

    @Test
    public void testGetShortCode() {
        assertEquals("short code for NOOP is -", "-", PathAction.NOOP.getShortCode());
        assertEquals("short code for UNKNOWN is ?", "?", PathAction.UNKNOWN.getShortCode());
        assertEquals("short code for ADDED is A", "A", PathAction.ADDED.getShortCode());
        assertEquals("short code for DELETED is D", "D", PathAction.DELETED.getShortCode());
        assertEquals("short code for REPLACED is R", "R", PathAction.REPLACED.getShortCode());
        assertEquals("short code for MODIFIED is U", "U", PathAction.MODIFIED.getShortCode());
        assertEquals("short code for ERROR is E", "E", PathAction.ERROR.getShortCode());
        assertEquals("short code for MISSING is !", "!", PathAction.MISSING.getShortCode());


    }

    @Test
    public void fromShortCode() {
        assertEquals("expect NOOP for -", PathAction.NOOP, PathAction.fromShortCode("-"));
        assertEquals("expect ADDED for A", PathAction.ADDED, PathAction.fromShortCode("A"));
        assertEquals("expect DELETED for D", PathAction.DELETED, PathAction.fromShortCode("D"));
        assertEquals("expect REPLACED for R", PathAction.REPLACED, PathAction.fromShortCode("R"));
        assertEquals("expect MODIFIED for U", PathAction.MODIFIED, PathAction.fromShortCode("U"));
        assertEquals("expect ERROR for E", PathAction.ERROR, PathAction.fromShortCode("E"));
        assertEquals("expect MISSING for !", PathAction.MISSING, PathAction.fromShortCode("!"));
        assertEquals("expect UNKNOWN for ?", PathAction.UNKNOWN, PathAction.fromShortCode("?"));
        assertEquals("expect UNKNOWN for Z", PathAction.UNKNOWN, PathAction.fromShortCode("Z"));
        assertEquals("expect UNKNOWN for ''", PathAction.UNKNOWN, PathAction.fromShortCode(""));
        assertEquals("expect UNKNOWN for null", PathAction.UNKNOWN, PathAction.fromShortCode(null));
    }

    @Test
    public void testToString() {
        for (PathAction action : PathAction.values()) {
            assertEquals(action + " toString is shortCode", action.getShortCode(), action.toString());
        }
    }

    @Test
    public void testIsNoop() {
        for (PathAction action : PathAction.values()) {
            if (action == PathAction.NOOP) {
                assertTrue("NOOP isNoop true", action.isNoop());
            } else {
                assertFalse(action.name() + " isNoop false", action.isNoop());
            }
        }
    }

    @Test
    public void testIsUnknown() {
        for (PathAction action : PathAction.values()) {
            if (action == PathAction.UNKNOWN) {
                assertTrue("NOOP isUnknown true", action.isUnknown());
            } else {
                assertFalse(action.name() + " isUnknown false", action.isUnknown());
            }
        }
    }

    @Test
    public void testCanGetItem() {
        for (PathAction action : PathAction.values()) {
            if (EnumSet.of(PathAction.NOOP, PathAction.MODIFIED,
                    PathAction.REPLACED, PathAction.ADDED).contains(action)) {
                assertTrue(action.name() + " canGetItem true", action.canGetItem());
            } else {
                assertFalse(action.name() + " canGetItem false", action.canGetItem());
            }
        }
    }
}