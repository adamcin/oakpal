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

import java.util.Arrays;
import java.util.Collections;

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.*;

public class SimpleReportTest {

    @Test
    public void testConstruct() {
        SimpleReport report = new SimpleReport("checkName", null);
        assertEquals("check name is", "checkName", report.getCheckName());
        assertTrue("violations are empty", report.getViolations().isEmpty());
        final PackageId id = PackageId.fromString("my_packages:test:1.0");
        SimpleReport reportMore = new SimpleReport("moreReport", Arrays.asList(
                new SimpleViolation(Violation.Severity.SEVERE, "severe", id),
                new SimpleViolation(Violation.Severity.MINOR, "minor", id)));

        assertEquals("check name is", "moreReport", reportMore.getCheckName());
        assertEquals("violations are 2", 2, reportMore.getViolations().size());
    }

    @Test
    public void testEqualsAndHash() {
        final PackageId id = PackageId.fromString("my_packages:test:1.0");
        SimpleReport original = new SimpleReport("moreReport", Arrays.asList(
                new SimpleViolation(Violation.Severity.SEVERE, "severe", id),
                new SimpleViolation(Violation.Severity.MINOR, "minor", id)));

        assertFalse("null not equal", original.equals(null));
        assertNotEquals("other not equal", original, new Object());
        assertEquals("same is equal", original, original);
        assertEquals("same hash is equal", original.hashCode(), original.hashCode());
        SimpleReport copy = new SimpleReport(original.getCheckName(), original.getViolations());
        assertEquals("copy is equal", original, copy);
        assertEquals("copy hash is equal", original.hashCode(), copy.hashCode());

        SimpleReport different = new SimpleReport("different", Arrays.asList(
                new SimpleViolation(Violation.Severity.MAJOR, "major", id)));
        assertNotEquals("different is not equal", original, different);
        assertNotEquals("different hash is not equal", original.hashCode(), different.hashCode());
    }

    @Test
    public void testToString() {
        assertNotNull("toString is still good", new SimpleReport("", null).toString());
    }

    @Test
    public void testGenerateReportFromProgressCheck() {
        SimpleProgressCheck check = new SimpleProgressCheck();
        check.minorViolation("minor");
        SimpleReport report = SimpleReport.generateReport(check);
        assertNotNull("report not null", report);
        assertEquals("report name", SimpleProgressCheck.class.getSimpleName(), report.getCheckName());
        assertEquals("violations are",
                Collections.singletonList(new SimpleViolation(Violation.Severity.MINOR, "minor")),
                report.getViolations());
    }

    @Test
    public void testGenerateReportFromErrorListener() {
        DefaultErrorListener errorListener = new DefaultErrorListener();
        errorListener.reportViolation(new SimpleViolation(Violation.Severity.MINOR, "minor"));
        SimpleReport report = SimpleReport.generateReport(errorListener);
        assertNotNull("report not null", report);
        assertEquals("report name", DefaultErrorListener.class.getSimpleName(), report.getCheckName());
        assertEquals("violations are",
                Collections.singletonList(new SimpleViolation(Violation.Severity.MINOR, "minor")),
                report.getViolations());
    }

    @Test
    public void testFromJson() {
        SimpleReport emptyReport = SimpleReport.fromJson(obj().get());
        assertEquals("empty report name is empty", "", emptyReport.getCheckName());
        assertTrue("empty report violations are empty", emptyReport.getViolations().isEmpty());

        SimpleViolation violation = new SimpleViolation(Violation.Severity.MINOR, "minor");
        SimpleReport moreReport = SimpleReport.fromJson(obj()
                .key(ReportMapper.KEY_CHECK_NAME, "more")
                .key(ReportMapper.KEY_VIOLATIONS, arr().val(violation))
                .get());

        assertEquals("more report name is more", "more", moreReport.getCheckName());
        assertEquals("more report violations are ",
                Collections.singletonList(violation), moreReport.getViolations());
    }
}
