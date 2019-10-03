/*
 * Copyright 2019 Mark Adamcin
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

package net.adamcin.oakpal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.SimpleViolation;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

public class CheckReportTest {

    @Test
    public void testGetViolationsBySeverity() {
        final CheckReport report = mock(CheckReport.class);
        final List<Violation> violations = new ArrayList<>();
        when(report.getViolations()).thenReturn(violations);
        doCallRealMethod().when(report).getViolations(nullable(Violation.Severity.class));

        assertEquals("violations size", 0, report.getViolations(null).size());

        violations.add(new SimpleViolation(Violation.Severity.MINOR, "minor violation"));
        violations.add(new SimpleViolation(Violation.Severity.MAJOR, "major violation"));
        violations.add(new SimpleViolation(Violation.Severity.SEVERE, "severe violation"));

        assertEquals("violations size after populating", 3, report.getViolations(null).size());
        assertEquals("violations size at or above minor", 3, report.getViolations(Violation.Severity.MINOR).size());
        assertEquals("violations size at or above major", 2, report.getViolations(Violation.Severity.MAJOR).size());
        assertEquals("violations size at or above severe", 1, report.getViolations(Violation.Severity.SEVERE).size());
    }

    @Test
    public void testToJson() {
        final CheckReport report = mock(CheckReport.class);
        final List<Violation> violations = new ArrayList<>();
        when(report.getViolations()).thenReturn(violations);
        when(report.getCheckName()).thenReturn("mock");
        doCallRealMethod().when(report).getViolations(nullable(Violation.Severity.class));
        doCallRealMethod().when(report).toJson();
        final PackageId fooId = PackageId.fromString("test:foo:1.0-SNAPSHOT");
        final PackageId barId = PackageId.fromString("test:bar:1.0-SNAPSHOT");
        violations.add(new SimpleViolation(Violation.Severity.MINOR, "minor violation"));
        violations.add(new SimpleViolation(Violation.Severity.MAJOR, "major violation", fooId));
        violations.add(new SimpleViolation(Violation.Severity.SEVERE, "severe violation", fooId, barId));

        JsonObject json = report.toJson();
        assertNotNull("json is not null", json);
        assertEquals("checkName should be", "mock", json.getString(ReportMapper.KEY_CHECK_NAME));
        JsonArray violationArray = json.getJsonArray(ReportMapper.KEY_VIOLATIONS);
        List<SimpleViolation> fromJson = JavaxJson.mapArrayOfObjects(violationArray, SimpleViolation::fromJson);
        assertEquals("fromJson should be an array of three simple violations", 3, fromJson.size());
        assertEquals("foo package is reported twice", 2,
                fromJson.stream().filter(viol -> viol.getPackages().contains(fooId)).count());
        assertEquals("bar package is reported once", 1,
                fromJson.stream().filter(viol -> viol.getPackages().contains(barId)).count());
    }
}