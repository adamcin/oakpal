/*
 * Copyright 2024 Mark Adamcin
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SeverityTest {

    @Test
    public void testComparisons() {
        for (Severity severity : Severity.values()) {
            // calling maxSeverity with itself always returns itself
            assertSame(severity, severity.maxSeverity(severity));
            // SEVERE is always the result of maxSeverity(Severity.SEVERE);
            assertSame(Severity.SEVERE, severity.maxSeverity(Severity.SEVERE));
            assertSame(Severity.SEVERE, Severity.SEVERE.maxSeverity(severity));

            // MINOR is only returned by maxSeverity(other) if both severity and other are MINOR,
            // so by contrast, other is always returned for other.maxSeverity(MINOR) or MINOR.maxSeverity(other)
            assertSame(severity, severity.maxSeverity(Severity.MINOR));
            assertSame(severity, Severity.MINOR.maxSeverity(severity));

            // everything is less severe than SEVERE, except for SEVERE
            if (severity != Severity.SEVERE) {
                assertTrue(severity.isLessSevereThan(Severity.SEVERE));
            } else {
                assertFalse(severity.isLessSevereThan(Severity.SEVERE));
            }
        }

        assertSame(Severity.MAJOR, Severity.MAJOR.maxSeverity(Severity.MINOR));
        assertSame(Severity.MAJOR, Severity.MINOR.maxSeverity(Severity.MAJOR));
        assertTrue(Severity.MINOR.isLessSevereThan(Severity.MAJOR));

        // enforce increasing ordinality from MINOR to SEVERE
        final List<Severity> minorToSevere = Arrays.asList(Severity.MINOR, Severity.MAJOR, Severity.SEVERE);
        assertEquals(minorToSevere, Stream.of(Severity.values()).sorted().collect(Collectors.toList()));

        assertEquals(Arrays.asList(Severity.MINOR, Severity.MAJOR, Severity.SEVERE),
                minorToSevere.stream().filter(Severity.MINOR.meetsMinimumSeverity()).collect(Collectors.toList()));

        assertEquals(Arrays.asList(Severity.MAJOR, Severity.SEVERE),
                minorToSevere.stream().filter(Severity.MAJOR.meetsMinimumSeverity()).collect(Collectors.toList()));

        assertEquals(Collections.singletonList(Severity.SEVERE),
                minorToSevere.stream().filter(Severity.SEVERE.meetsMinimumSeverity()).collect(Collectors.toList()));
    }

    @Test
    public void testByName() {
        assertSame(Severity.SEVERE, Severity.byName("SEVERE"));
        assertSame(Severity.SEVERE, Severity.byName("severe"));
        assertSame(Severity.MAJOR, Severity.byName("MAJOR"));
        assertSame(Severity.MAJOR, Severity.byName("major"));
        assertSame(Severity.MINOR, Severity.byName("MINOR"));
        assertSame(Severity.MINOR, Severity.byName("minor"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByName_Unknown() {
        Severity.byName("unknown");
        Severity.byName("");
    }
}
