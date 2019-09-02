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

import org.apache.commons.io.FileUtils;
import org.apache.derby.iapi.services.io.FileUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanManyArtifactsMojoTest {
    private final File testOutBaseDir = new File("target/test-out/ScanManyArtifactsMojoTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    static ScanManyArtifactsMojo newMojo() {
        ScanManyArtifactsMojo mojo = new ScanManyArtifactsMojo();
        MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    @Test
    public void testIsIndividuallySkipped() {
        ScanManyArtifactsMojo mojo = newMojo();
        assertFalse("is not skipped", mojo.isIndividuallySkipped());
        mojo.skip = true;
        assertTrue("is skipped", mojo.isIndividuallySkipped());
    }

    @Test
    public void testListScanFiles() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testListScanFiles");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        ScanManyArtifactsMojo mojo = newMojo();
        assertTrue("empty by default", mojo.listScanFiles().isEmpty());

        final File artifactFile = new File(testOutDir, "scan-artifact-file.zip");
        FileUtils.touch(artifactFile);
        final File fileFile = new File(testOutDir, "scan-file-file.zip");
        FileUtils.touch(fileFile);

        final Dependency dependency = new DependencyFilter()
                .withGroupId("com.example")
                .withArtifactId("scan-artifact-file")
                .withVersion("1.0-SNAPSHOT")
                .withType("zip")
                .toDependency();
        mojo.scanArtifacts = Collections.singletonList(dependency);

        mojo.scanFiles = Collections.singletonList(fileFile);

        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        final Artifact scanArtifact = mock(Artifact.class);
        when(scanArtifact.getFile()).thenReturn(artifactFile);
        when(repositorySystem.createDependencyArtifact(dependency)).thenReturn(scanArtifact);

        final List<File> scanFiles = mojo.listScanFiles();
        assertEquals("expect files", Arrays.asList(artifactFile, fileFile), scanFiles);
    }

    @Test
    public void testExecuteGuardedIntegrationTest() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecuteGuardedIntegrationTest");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        ScanManyArtifactsMojo mojo = newMojo();
        mojo.summaryFile = new File(testOutDir, "summary.json");
        mojo.executeGuardedIntegrationTest();
    }
}