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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.api.Severity.MAJOR;
import static net.adamcin.oakpal.api.Severity.MINOR;
import static net.adamcin.oakpal.api.Severity.SEVERE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViolationTest {

    @Test
    public void testSeverity_byName() {
        assertSame("severity for minor is", MINOR, Severity.byName("minor"));
        assertSame("severity for minor is", MINOR, Severity.byName("MiNoR"));
        assertSame("severity for major is", MAJOR, Severity.byName("major"));
        assertSame("severity for major is", MAJOR, Severity.byName("MaJoR"));
        assertSame("severity for severe is", SEVERE, Severity.byName("severe"));
        assertSame("severity for severe is", SEVERE, Severity.byName("SeVeRe"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSeverity_byName_throws() {
        Severity.byName("nothing");
    }

    @Test
    public void testIsLessSevereThan() {
        assertFalse("minor is not less than minor", MINOR.isLessSevereThan(MINOR));
        assertTrue("minor is less than major", MINOR.isLessSevereThan(MAJOR));
        assertFalse("major is not less than minor", MAJOR.isLessSevereThan(MINOR));
        assertFalse("major is not less than major", MAJOR.isLessSevereThan(MAJOR));
        assertTrue("major is less than severe", MAJOR.isLessSevereThan(SEVERE));
        assertFalse("severe is not less than minor", SEVERE.isLessSevereThan(MINOR));
        assertFalse("severe is not less than major", SEVERE.isLessSevereThan(MAJOR));
        assertFalse("severe is not less than severe", SEVERE.isLessSevereThan(SEVERE));
    }

    @Test
    public void testMeetsMinimumSeverity() {
        assertTrue("all meet min for minor",
                Stream.of(MINOR, MAJOR, SEVERE).allMatch(MINOR.meetsMinimumSeverity()));
        assertFalse("minor meets min no others",
                MAJOR.meetsMinimumSeverity().or(SEVERE.meetsMinimumSeverity()).test(MINOR));
        assertTrue("major, severe meet min for major",
                Stream.of(MAJOR, SEVERE).allMatch(MAJOR.meetsMinimumSeverity()));
        assertFalse("minor meets min no others", SEVERE.meetsMinimumSeverity().test(MAJOR));
        assertTrue("severe meets min for severe", SEVERE.meetsMinimumSeverity().test(SEVERE));
    }

    @Test
    public void testMaxSeverity() {
        assertSame("max of minor, minor is minor", MINOR, MINOR.maxSeverity(MINOR));
        assertSame("max of major, major is major", MAJOR, MAJOR.maxSeverity(MAJOR));
        assertSame("max of severe, sever is severe", SEVERE, SEVERE.maxSeverity(SEVERE));
        assertSame("max of minor, major is major", MAJOR, MINOR.maxSeverity(MAJOR));
        assertSame("max of major, minor is major", MAJOR, MAJOR.maxSeverity(MINOR));
        assertSame("max of minor, severe is severe", SEVERE, MINOR.maxSeverity(SEVERE));
        assertSame("max of severe, minor is severe", SEVERE, SEVERE.maxSeverity(MINOR));
        assertSame("max of major, severe is severe", SEVERE, MAJOR.maxSeverity(SEVERE));
        assertSame("max of severe, major is severe", SEVERE, SEVERE.maxSeverity(MAJOR));
    }

    @Test
    public void testToJson() {
        final PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        final PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        Violation violation = mock(Violation.class);
        doCallRealMethod().when(violation).toJson();
        when(violation.getSeverity()).thenReturn(SEVERE);
        when(violation.getDescription()).thenReturn("some failure");
        when(violation.getPackages()).thenReturn(Arrays.asList(id0, id1));

        JsonObject expected = obj()
                .key(ApiConstants.violationKeys().severity(), SEVERE)
                .key(ApiConstants.violationKeys().description(), "some failure")
                .key(ApiConstants.violationKeys().packages(), arr().val(id0.toString()).val(id1.toString()))
                .get();

        assertEquals("same json", expected, violation.toJson());

    }
}