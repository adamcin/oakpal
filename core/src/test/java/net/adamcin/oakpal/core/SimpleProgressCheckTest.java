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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleProgressCheckTest {

    @Test
    public void testReportViolation() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        SimpleViolation violation = new SimpleViolation(Violation.Severity.SEVERE, "description");
        check.reportViolation(violation);
        assertTrue("contains violation", check.getReportedViolations().contains(violation));
    }

    @Test
    public void testReportViolation_args() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        check.reportViolation(Violation.Severity.MINOR, "description", id0, id1);
        assertTrue("violation reported", check.getReportedViolations().stream()
                .anyMatch(violation -> violation.getSeverity() == Violation.Severity.MINOR
                        && "description".equals(violation.getDescription())
                        && violation.getPackages().contains(id0)
                        && violation.getPackages().contains(id1)
                ));
    }

    @Test
    public void testStartedScan() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        check.reportViolation(Violation.Severity.MINOR, "description", id0, id1);
        assertEquals("violation reported", 1, check.getReportedViolations().size());
        check.startedScan();
        assertTrue("violations are now empty", check.getReportedViolations().isEmpty());
    }

    @Test
    public void testMinorViolation() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        check.minorViolation("description", id0, id1);
        assertTrue("violation reported", check.getReportedViolations().stream()
                .anyMatch(violation -> violation.getSeverity() == Violation.Severity.MINOR
                        && "description".equals(violation.getDescription())
                        && violation.getPackages().contains(id0)
                        && violation.getPackages().contains(id1)
                ));
    }

    @Test
    public void testMajorViolation() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        check.majorViolation("description", id0, id1);
        assertTrue("violation reported", check.getReportedViolations().stream()
                .anyMatch(violation -> violation.getSeverity() == Violation.Severity.MAJOR
                        && "description".equals(violation.getDescription())
                        && violation.getPackages().contains(id0)
                        && violation.getPackages().contains(id1)
                ));
    }

    @Test
    public void testSevereViolation() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        check.severeViolation("description", id0, id1);
        assertTrue("violation reported", check.getReportedViolations().stream()
                .anyMatch(violation -> violation.getSeverity() == Violation.Severity.SEVERE
                        && "description".equals(violation.getDescription())
                        && violation.getPackages().contains(id0)
                        && violation.getPackages().contains(id1)
                ));
    }
}