/*
 * Copyright 2018 Mark Adamcin
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

package net.adamcin.oakpal.maven.mojo;

import net.adamcin.oakpal.core.CheckReport;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerifyMojoTest {

    private final File testOutBaseDir = new File("target/test-out/VerifyMojoTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    private static VerifyMojo newMojo() {
        VerifyMojo mojo = new VerifyMojo();
        MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    @Test
    public void testIsIndividuallySkipped() {
        VerifyMojo mojo = newMojo();
        assertFalse("is not skipped", mojo.isIndividuallySkipped());
        mojo.skip = true;
        assertTrue("is skipped", mojo.isIndividuallySkipped());
    }

    @Test
    public void testReadReportsFromFile() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testReadReportsFromFile");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        File summaryFile = new File("src/test/resources/unit/justverify/fake-summary.json");
        List<CheckReport> reports = VerifyMojo.readReportsFromFile(summaryFile);
        assertEquals("reports not empty", 8, reports.size());
    }

    @Test(expected = MojoFailureException.class)
    public void testCollectReports_throws() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testCollectReports_throws");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        VerifyMojo mojo = new VerifyMojo();
        final File summaryFile = new File(testOutDir, "summaryDir");
        summaryFile.mkdirs();
        mojo.summaryFile = summaryFile;
        mojo.collectReports();
    }

    @Test(expected = MojoFailureException.class)
    public void testCollectReports_addThrows() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testCollectReports_addThrows");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        VerifyMojo mojo = new VerifyMojo();
        final File summaryFile = new File(testOutDir, "summary.json");
        final File addSummaryFile = new File(testOutDir, "summary2.json");
        addSummaryFile.mkdirs();
        mojo.summaryFile = summaryFile;
        mojo.summaryFiles.add(addSummaryFile);
        mojo.collectReports();
    }

    @Test
    public void testExecuteGuardedIntegrationTest() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteGuardedIntegrationTest");
        VerifyMojo mojo = new VerifyMojo();
        final File summaryFile = new File(testOutDir, "summary.json");
        final File addSummaryFile = new File(testOutDir, "summary2.json");
        mojo.summaryFile = summaryFile;
        mojo.summaryFiles.add(addSummaryFile);
        mojo.executeGuardedIntegrationTest();
    }
}
