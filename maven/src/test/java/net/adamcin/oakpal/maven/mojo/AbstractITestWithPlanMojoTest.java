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

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.adamcin.oakpal.core.Fun.result0;
import static net.adamcin.oakpal.core.InstallHookPolicy.PROHIBIT;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AbstractITestWithPlanMojoTest {
    private final File testOutBaseDir = new File("target/test-out/AbstractITestWithPlanMojoTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    private static final class AbstractITestWithPlanMojoStub extends AbstractITestWithPlanMojo {
        @Override
        protected boolean isIndividuallySkipped() {
            return false;
        }
    }

    private static AbstractITestWithPlanMojo newMojo() throws Exception {
        final AbstractITestWithPlanMojo mojo = new AbstractITestWithPlanMojoStub();
        MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    @Test
    public void testGetters() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testGetters");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        AbstractITestWithPlanMojo mojo = newMojo();

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getPreInstallArtifacts());
        final DependencyFilter preInstallArtifact = new DependencyFilter();
        mojo.preInstallArtifacts.add(preInstallArtifact);
        assertEquals("expect same preInstallArtifacts",
                Collections.singletonList(preInstallArtifact), mojo.getPreInstallArtifacts());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getPreInstallFiles());
        final File preInstallFile = new File(testOutDir, "pre-install-file.zip");
        mojo.preInstallFiles.add(preInstallFile);
        assertEquals("expect same preInstallFiles",
                Collections.singletonList(preInstallFile), mojo.getPreInstallFiles());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getCndNames());
        final String expectCndName = "foo.cnd";
        mojo.cndNames.add(expectCndName);
        assertEquals("expect same cndNames",
                Collections.singletonList(expectCndName), mojo.getCndNames());

        assertFalse("expect false slingNodeTypes by default", mojo.isSlingNodeTypes());
        mojo.slingNodeTypes = true;
        assertTrue("expect true slingNodeTypes", mojo.isSlingNodeTypes());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getJcrNamespaces());
        final JcrNs expectJcrNs = JcrNs.create("foo", "http://foo.com");
        mojo.jcrNamespaces.add(expectJcrNs);
        assertEquals("expect same jcrNamespaces",
                Collections.singletonList(expectJcrNs), mojo.getJcrNamespaces());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getJcrPrivileges());
        final String expectJcrPrivilege = "foo:canDo";
        mojo.jcrPrivileges.add(expectJcrPrivilege);
        assertEquals("expect same jcrPrivileges",
                Collections.singletonList(expectJcrPrivilege), mojo.getJcrPrivileges());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getForcedRoots());
        final ForcedRoot expectForcedRoot = new ForcedRoot().withPath("/foo");
        mojo.forcedRoots.add(expectForcedRoot);
        assertEquals("expect same forcedRoots",
                Collections.singletonList(expectForcedRoot), mojo.getForcedRoots());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getChecks());
        final CheckSpec expectCheck = CheckSpec.fromJson(key("name", "myCheck").get());
        mojo.checks.add(expectCheck);
        assertEquals("expect same checks",
                Collections.singletonList(expectCheck), mojo.getChecks());

        assertEquals("expect empty by default",
                Collections.emptyList(), mojo.getChecklists());
        final String expectChecklist = "fooChecklist";
        mojo.checklists.add(expectChecklist);
        assertEquals("expect same checklists",
                Collections.singletonList(expectChecklist), mojo.getChecklists());

        assertFalse("expect false enablePreInstallHooks by default", mojo.isEnablePreInstallHooks());
        mojo.enablePreInstallHooks = true;
        assertTrue("expect true enablePreInstallHooks", mojo.isEnablePreInstallHooks());

        assertNull("expect null installHookPolicy by default", mojo.getInstallHookPolicy());
        final InstallHookPolicy expectInstallHookPolicy = PROHIBIT;
        mojo.installHookPolicy = PROHIBIT;
        assertSame("expect same installHookPolicy",
                PROHIBIT, mojo.getInstallHookPolicy());

        PlanBuilderParams params = mojo.getPlanBuilderParams();
        assertEquals("expect param", Collections.singletonList(preInstallArtifact), params.getPreInstallArtifacts());
        assertEquals("expect param", Collections.singletonList(preInstallFile), params.getPreInstallFiles());
        assertEquals("expect param", Collections.singletonList(expectCndName), params.getCndNames());
        assertTrue("expect param", params.isSlingNodeTypes());
        assertEquals("expect param", Collections.singletonList(expectJcrNs), params.getJcrNamespaces());
        assertEquals("expect param", Collections.singletonList(expectJcrPrivilege), params.getJcrPrivileges());
        assertEquals("expect param", Collections.singletonList(expectForcedRoot), params.getForcedRoots());
        assertEquals("expect param", Collections.singletonList(expectCheck), params.getChecks());
        assertEquals("expect param", Collections.singletonList(expectChecklist), params.getChecklists());
        assertTrue("expect param", params.isEnablePreInstallHooks());
        assertEquals("expect param", PROHIBIT, params.getInstallHookPolicy());
    }

    @Test(expected = AbortedScanException.class)
    public void testPerformScan_abortedScan() throws Exception {
        final File abortingPackage = TestPackageUtil.prepareTestPackage("unfiltered_package.zip");
        AbstractITestWithPlanMojo mojo = newMojo();
        result0(() -> {
            mojo.performScan(Collections.singletonList(abortingPackage));
            return Nothing.instance;
        }).get().throwCause(AbortedScanException.class);
    }

    @Test(expected = MojoFailureException.class)
    public void testPerformScan_invalidCheck() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testPerformScan_invalidBlobStore");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        AbstractITestWithPlanMojo mojo = newMojo();
        final File blobStore = new File(testOutDir, "blobStore");
        FileUtils.touch(blobStore);
        mojo.blobStorePath = blobStore.getAbsolutePath();
        mojo.checks.add(CheckSpec.fromJson(key("impl", "com.example.NotAClass").get()));
        mojo.performScan(Collections.emptyList());
    }

    @Test(expected = MojoFailureException.class)
    public void testPerformScan_failViolations() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testPerformScan_failViolations");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File summaryFile = new File(testOutDir, "summary.json");
        AbstractITestWithPlanMojo mojo = newMojo();
        scanWithViolations(mojo, summaryFile);
    }

    static void scanWithViolations(final @NotNull AbstractITestWithPlanMojo mojo, final @NotNull File summaryFile)
            throws Exception {
        mojo.summaryFile = summaryFile;
        mojo.checks.add(CheckSpec.fromJson(
                key("inlineScript", "function afterExtract(packageId){ oakpal.majorViolation(\"fail\", packageId);}")
                        .get()));

        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        mojo.performScan(Collections.singletonList(testPackage));
    }

    static void scanWithSubpackageViolations(final @NotNull AbstractITestWithPlanMojo mojo, final @NotNull File summaryFile)
            throws Exception {
        mojo.summaryFile = summaryFile;
        mojo.checks.add(CheckSpec.fromJson(obj()
                .key("name", "subfailer")
                .key("inlineScript", "function identifySubpackage(subpackageId, parentId){ oakpal.majorViolation(\"fail\", subpackageId);}")
                        .get()));

        final File testPackage = TestPackageUtil.prepareTestPackage("subsubtest.zip");
        mojo.performScan(Collections.singletonList(testPackage));
    }

    @Test
    public void testPerformScan_silenceAllSubpackages() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testPerformScan_silenceAllSubpackages");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File summaryWithSubsFile = new File(testOutDir, "summaryWithSubs.json");
        AbstractITestWithPlanMojo mojo = newMojo();
        mojo.deferBuildFailure = true;
        scanWithSubpackageViolations(mojo, summaryWithSubsFile);
        List<CheckReport> reportsWithSubs = ReportMapper.readReportsFromFile(summaryWithSubsFile);
        Optional<CheckReport> checkReportWithSubs = reportsWithSubs.stream().filter(report -> "subfailer".equals(report.getCheckName())).findFirst();
        assertTrue("subfailer is present", checkReportWithSubs.isPresent());
        assertEquals("subfailer violations count", 3, checkReportWithSubs.get().getViolations().size());
        final File summaryNoSubsFile = new File(testOutDir, "summaryWithSubs.json");
        mojo.silenceAllSubpackages = true;
        scanWithSubpackageViolations(mojo, summaryNoSubsFile);
        List<CheckReport> reportsNoSubs = ReportMapper.readReportsFromFile(summaryWithSubsFile);
        Optional<CheckReport> checkReportNoSubs = reportsNoSubs.stream().filter(report -> "subfailer".equals(report.getCheckName())).findFirst();
        assertTrue("subfailer is present", checkReportNoSubs.isPresent());
        assertEquals("subfailer violations count", 0, checkReportNoSubs.get().getViolations().size());

    }

    @Test
    public void testPerformScan_deferBuildFailure() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testPerformScan_deferBuildFailure");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File summaryFile = new File(testOutDir, "summary.json");
        AbstractITestWithPlanMojo mojo = newMojo();
        mojo.deferBuildFailure = true;
        scanWithViolations(mojo, summaryFile);
        MockMojoLog log = (MockMojoLog) mojo.getLog();
        assertTrue("last message matches", log.last().filter(entry -> entry.message.startsWith("Evaluation of check reports")).isPresent());
    }

    @Test
    public void testPerformScan_withBlobStorePath() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testPerformScan_withBlobStorePath");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File summaryFile = new File(testOutDir, "summary.json");
        AbstractITestWithPlanMojo mojo = newMojo();
        final File blobStore = new File(testOutDir, "blobStore");
        mojo.deferBuildFailure = true;
        mojo.blobStorePath = blobStore.getAbsolutePath();
        scanWithViolations(mojo, summaryFile);
        MockMojoLog log = (MockMojoLog) mojo.getLog();
        assertTrue("last message matches", log.last().filter(entry -> entry.message.startsWith("Evaluation of check reports")).isPresent());
        final File[] preChildren = blobStore.listFiles();
        assertNull("blobStore file is not yet a directory", preChildren);
        mojo.storeBlobs = true;
        scanWithViolations(mojo, summaryFile);
        final File[] children = blobStore.listFiles();
        assertNotNull("blobStore file is directory", children);
        assertTrue("blobStore has children", children.length > 0);
    }

    @Test(expected = MojoFailureException.class)
    public void testPerformScan_writeSummaryFailure() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testPerformScan_writeSummaryFailure");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File summaryFile = new File(testOutDir, "summarydir");
        summaryFile.mkdirs();
        AbstractITestWithPlanMojo mojo = newMojo();
        mojo.deferBuildFailure = true;
        scanWithViolations(mojo, summaryFile);
    }
}