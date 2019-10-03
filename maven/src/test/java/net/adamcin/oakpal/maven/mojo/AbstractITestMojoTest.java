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

package net.adamcin.oakpal.maven.mojo;

import net.adamcin.oakpal.api.SimpleViolation;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.SimpleReport;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AbstractITestMojoTest {
    private final File testOutBaseDir = new File("target/test-out/AbstractITestMojoTest");

    @Test
    public void testExecutedGuaredIntegrationTest() throws Exception {
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.executeGuardedIntegrationTest();
    }

    @Test
    public void testIsTestSCopeContainer() {
        ConcreteMojo mojo = new ConcreteMojo();
        assertTrue("expect true", mojo.isTestScopeContainer());
    }

    @Test
    public void testGetContainerClassLoader() throws Exception {
        ConcreteMojo mojo = new ConcreteMojo();
        final ClassLoader firstCl = mojo.getContainerClassLoader();
        assertNotNull("expect non null", firstCl);
        assertSame("same cl", firstCl, mojo.getContainerClassLoader());
    }

    @Test
    public void testReactToReports_emptyList() throws Exception {
        MockMojoLog log = new MockMojoLog();
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setLog(log);
        mojo.reactToReports(Collections.emptyList());
        assertEquals("expect no logs", 0, log.entries.size());
    }

    @Test
    public void testReactToReports_emptyReports() throws Exception {
        MockMojoLog log = new MockMojoLog();
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setLog(log);
        mojo.reactToReports(Arrays.asList(
                new SimpleReport("one", Collections.emptyList()),
                new SimpleReport("two", Collections.emptyList())));
        assertEquals("expect no logs", 0, log.entries.size());
    }

    @Test
    public void testReactToReports_nonEmptyReports_noFail() throws Exception {
        final PackageId packageId = PackageId.fromString("my_packages:example-one:1.0");
        MockMojoLog log = new MockMojoLog();
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setLog(log);
        mojo.reactToReports(Arrays.asList(
                new SimpleReport("one", Collections
                        .singletonList(new SimpleViolation(Violation.Severity.MINOR, "one",
                                packageId))),
                new SimpleReport("two", Collections
                        .singletonList(new SimpleViolation(Violation.Severity.MINOR, "two")))));
        assertFalse("no error messages", log.any(MockMojoLog.MockMojoLogEntry::isError));
        assertTrue("expect log header", log.any(entry -> "OakPAL Check Reports".equals(entry.message)));
        assertTrue("expect report one", log.any(entry -> "  one".equals(entry.message)));
        assertTrue("expect violation one", log.any(entry -> entry.message.contains("+- <MINOR> one")));
        assertTrue("expect report two", log.any(entry -> "  two".equals(entry.message)));
        assertTrue("expect violation two", log.any(entry -> entry.message.contains("+- <MINOR> two")));
        assertTrue("expect violation two", log.any(entry -> entry.message.contains(packageId.getDownloadName())));
    }

    @Test
    public void testReactToReports_nonEmptyReports_fail() {
        final PackageId packageId = PackageId.fromString("my_packages:example-one:1.0");
        MockMojoLog log = new MockMojoLog();
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setFailOnSeverity(Violation.Severity.MINOR);
        mojo.setLog(log);
        final List<CheckReport> reports = Arrays.asList(
                new SimpleReport("one", Collections
                        .singletonList(new SimpleViolation(Violation.Severity.MINOR, "one",
                                packageId))),
                new SimpleReport("two", Collections
                        .singletonList(new SimpleViolation(Violation.Severity.MINOR, "two"))));
        boolean failed = false;
        try {
            mojo.reactToReports(reports);
        } catch (MojoFailureException e) {
            failed = true;
        }
        assertTrue("expect failure", failed);
        assertTrue("expect failed log",
                log.any(entry -> "** Violations were reported at or above severity: MINOR **".equals(entry.message)));
        assertTrue("expect error messages", log.any(MockMojoLog.MockMojoLogEntry::isError));
        assertTrue("expect info messages", log.any(MockMojoLog.MockMojoLogEntry::isInfo));
        assertTrue("expect log header", log.any(entry -> "OakPAL Check Reports".equals(entry.message)));
        assertTrue("expect report one", log.any(entry -> "  one".equals(entry.message)));
        assertTrue("expect violation one", log.any(entry -> entry.message.contains("+- <MINOR> one")));
        assertTrue("expect report two", log.any(entry -> "  two".equals(entry.message)));
        assertTrue("expect violation two", log.any(entry -> entry.message.contains("+- <MINOR> two")));
        assertTrue("expect violation two", log.any(entry -> entry.message.contains(packageId.getDownloadName())));
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteNoParams() throws Exception {
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteWithInvalidSummaryFile() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteWithInvalidSummaryFile");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File parentFile = new File(testOutDir, "invalidSummaryDir");
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setSummaryFile(new File(parentFile, "invalidSummaryFile.json"));
        FileUtils.touch(parentFile);
        mojo.execute();
    }

    @Test
    public void testExecuteWithValidSummaryFile() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteWithValidSummaryFile");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File parentFile = new File(testOutDir, "validSummaryDir");
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setSummaryFile(new File(parentFile, "validSummaryFile.json"));
        mojo.execute();
        assertTrue("parentFile exists", parentFile.exists());
    }

    @Test
    public void testExecuteWithSkips() throws Exception {
        MockMojoLog log = new MockMojoLog();
        ConcreteMojo mojo = new ConcreteMojo();
        mojo.setLog(log);

        mojo.setSkip(true);
        mojo.execute();
        assertTrue("expect log skip", log.entries.stream()
                .anyMatch(entry -> entry.cause == null && entry.isInfo() && entry.message.contains("skip=true")));
        log.entries.clear();
        mojo.setSkip(false);
        mojo.setSkipITs(true);
        mojo.execute();
        assertTrue("expect log skipITs", log.entries.stream()
                .anyMatch(entry -> entry.cause == null && entry.isInfo() && entry.message.contains("skipITs=true")));
        log.entries.clear();
        mojo.setSkipITs(false);
        mojo.setSkipTests(true);
        mojo.execute();
        assertTrue("expect log skipTests", log.entries.stream()
                .anyMatch(entry -> entry.cause == null && entry.isInfo() && entry.message.contains("skipTests=true")));
    }

    private static class ConcreteMojo extends AbstractITestMojo {

        public void setSkipITs(boolean skipITs) {
            this.skipITs = skipITs;
        }

        public void setSkipTests(boolean skipTests) {
            this.skipTests = skipTests;
        }

        public void setSummaryFile(File summaryFile) {
            this.summaryFile = summaryFile;
        }

        public void setFailOnSeverity(Violation.Severity failOnSeverity) {
            this.failOnSeverity = failOnSeverity;
        }

        private boolean skip;

        public void setSkip(boolean skip) {
            this.skip = skip;
        }

        @Override
        public ClassLoader createContainerClassLoader() throws MojoFailureException {
            return new URLClassLoader(new URL[0], null);
        }

        @Override
        protected boolean isIndividuallySkipped() {
            return skip;
        }

    }
}