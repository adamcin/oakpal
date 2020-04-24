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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SimpleProgressCheckTest {

    @Test
    public void testSetResourceBundle() {
        final SimpleProgressCheck check = new SimpleProgressCheck();

        ResourceBundle originalBundle = check.getResourceBundle();
        assertSame("same object returned twice", originalBundle, check.getResourceBundle());
        ResourceBundle newBundle = ResourceBundle.getBundle(check.getResourceBundleBaseName(),
                Locale.getDefault(), new URLClassLoader(new URL[0], getClass().getClassLoader()));
        assertNotSame("not same object as created externally", newBundle, check.getResourceBundle());
        check.setResourceBundle(newBundle);
        assertSame("same object as set", newBundle, check.getResourceBundle());
        assertSame("same object as set, again", newBundle, check.getResourceBundle());
    }

    @Test
    public void testGetString() {
        final SimpleProgressCheck check = new SimpleProgressCheck();
        assertEquals("expect passthrough", "testKey", check.getString("testKey"));
        ResourceBundle newBundle = ResourceBundle.getBundle(getClass().getName());
        check.setResourceBundle(newBundle);
        assertEquals("expect from bundle", "yeKtset", check.getString("testKey"));
    }

    @Test
    public void testReportViolation() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        SimpleViolation violation = new SimpleViolation(Severity.SEVERE, "description");
        check.reportViolation(violation);
        assertTrue("contains violation", check.getReportedViolations().contains(violation));
    }

    @Test
    public void testReportViolation_args() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        check.reportViolation(Severity.MINOR, "description", id0, id1);
        assertTrue("violation reported", check.getReportedViolations().stream()
                .anyMatch(violation -> violation.getSeverity() == Severity.MINOR
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
        check.reportViolation(Severity.MINOR, "description", id0, id1);
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
                .anyMatch(violation -> violation.getSeverity() == Severity.MINOR
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
                .anyMatch(violation -> violation.getSeverity() == Severity.MAJOR
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
                .anyMatch(violation -> violation.getSeverity() == Severity.SEVERE
                        && "description".equals(violation.getDescription())
                        && violation.getPackages().contains(id0)
                        && violation.getPackages().contains(id1)
                ));
    }
}