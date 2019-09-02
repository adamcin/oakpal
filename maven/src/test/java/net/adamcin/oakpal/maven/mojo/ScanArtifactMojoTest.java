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

import java.io.File;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.derby.iapi.services.io.FileUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.ContainerConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanArtifactMojoTest {

    private final File testOutBaseDir = new File("target/test-out/ScanArtifactMojoTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    static ScanArtifactMojo newMojo() {
        ScanArtifactMojo mojo = new ScanArtifactMojo();
        MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    @Test(expected = MojoFailureException.class)
    public void testGetScanArtifactFile_failWhenEmpty() throws Exception {
        newMojo().getScanArtifactFile();
    }

    @Test
    public void testGetScanArtifactFile() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testGetScanArtifactFile");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File artifactFile = new File(testOutDir, "test-artifact.zip");
        FileUtils.touch(artifactFile);

        final MavenProject project = mock(MavenProject.class);
        ScanArtifactMojo mojo = newMojo();
        mojo.project = project;
        final Artifact artifact = mock(Artifact.class);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getFile()).thenReturn(artifactFile);
        assertSame("expect same artifact", artifactFile, mojo.getScanArtifactFile());
    }

    @Test
    public void testIsIndividuallySkipped() {
        ScanArtifactMojo mojo = newMojo();
        assertFalse("is not skipped", mojo.isIndividuallySkipped());
        mojo.skip = true;
        assertTrue("is skipped", mojo.isIndividuallySkipped());
    }

    @Test
    public void testExecuteGuardedIntegrationTest() throws Exception {
        final File testPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final File testOutDir = new File(testOutBaseDir, "testExecuteGuardedIntegrationTest");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File artifactFile = testPackage;

        final MavenProject project = mock(MavenProject.class);
        ScanArtifactMojo mojo = newMojo();
        mojo.summaryFile = new File(testOutDir, "summary.json");
        mojo.project = project;
        final Artifact artifact = mock(Artifact.class);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getFile()).thenReturn(artifactFile);
        mojo.executeGuardedIntegrationTest();
    }

}
