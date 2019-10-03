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
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpearPlanMojoTest {
    private final File testOutBaseDir = new File("target/test-out/OpearPlanMojoTest");

    private static OpearPlanMojo newMojo() {
        OpearPlanMojo mojo = new OpearPlanMojo();
        MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    @Test
    public void testGetPlanBuilderParams() {
        OpearPlanMojo mojo = newMojo();
        assertNotNull("always have plan params", mojo.getPlanBuilderParams());

        mojo.planParams = new PlanParams();
        assertSame("same plan params", mojo.planParams, mojo.getPlanBuilderParams());
    }

    @Test(expected = MojoFailureException.class)
    public void testExecute_failDir() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecute_failDir");
        FileUtils.deleteDirectory(testOutDir);
        OpearPlanMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final Build build = mock(Build.class);
        when(project.getBuild()).thenReturn(build);
        final File classes = new File(testOutDir, "classes");
        when(build.getOutputDirectory()).thenReturn(classes.getPath());

        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        mojo.planFile = new File(testOutDir, "opear-plans/plan.json");
        mojo.planFile.mkdirs();
        mojo.execute();
    }

    @Test
    public void testExecute() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testExecute");
        FileUtils.deleteDirectory(testOutDir);
        OpearPlanMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final Build build = mock(Build.class);
        when(project.getBuild()).thenReturn(build);
        final File classes = new File(testOutDir, "classes");
        when(build.getOutputDirectory()).thenReturn(classes.getPath());

        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;
        mojo.planParams = new PlanParams();
        mojo.planParams.setChecklists(Arrays.asList("checklist1", "checklist2"));
        mojo.planFile = new File(testOutDir, "opear-plans/plan.json");
        mojo.execute();

        try (Reader reader = new InputStreamReader(new FileInputStream(mojo.planFile), StandardCharsets.UTF_8);
             JsonReader jsonReader = Json.createReader(reader)) {
            final JsonObject readObject = jsonReader.readObject();
            assertEquals("expect json", key("checklists", arr("checklist1", "checklist2")).get(), readObject);
        }
    }
}