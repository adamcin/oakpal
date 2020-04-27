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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleViolation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Before;
import org.junit.Test;

public class ReportMapperTest {

    private final File baseDir = new File("target/test-output/ReportMapperTest");

    @Before
    public void setUp() throws Exception {
        baseDir.mkdirs();
    }

    @Test
    public void testReportsToJson() throws Exception {
        final List<CheckReport> originalReports = asList(
                new SimpleReport("test/first",
                        singletonList(
                                new SimpleViolation(Severity.MINOR,
                                        "one",
                                        PackageId.fromString("test:first")))
                ),
                new SimpleReport("test/second",
                        asList(
                                new SimpleViolation(Severity.MINOR,
                                        "one", PackageId.fromString("test:first")),
                                new SimpleViolation(Severity.MINOR,
                                        "two",
                                        PackageId.fromString("test:first"),
                                        PackageId.fromString("test:second"))
                        )
                )
        );

        final StringWriter writer = new StringWriter();
        ReportMapper.writeReports(originalReports, () -> writer);
        assertEquals("CheckReports should round trip",
                new ArrayList<>(originalReports),
                new ArrayList<>(ReportMapper.readReports(() -> new StringReader(writer.toString()))));
    }

    @Test
    public void testWriteThenRead() throws Exception {
        final File jsonFile = new File(baseDir, "reports.json");
        final List<CheckReport> originalReports = asList(
                new SimpleReport("test/first",
                        singletonList(
                                new SimpleViolation(Severity.MINOR,
                                        "one",
                                        PackageId.fromString("test:first")))
                ),
                new SimpleReport("test/second",
                        asList(
                                new SimpleViolation(Severity.MINOR,
                                        "one", PackageId.fromString("test:first")),
                                new SimpleViolation(Severity.MINOR,
                                        "two",
                                        PackageId.fromString("test:first"),
                                        PackageId.fromString("test:second"))
                        )
                )
        );

        final StringWriter writer = new StringWriter();
        ReportMapper.writeReportsToFile(originalReports, jsonFile);
        assertEquals("CheckReports should round trip",
                new ArrayList<>(originalReports),
                new ArrayList<>(ReportMapper.readReportsFromFile(jsonFile)));

    }
}
