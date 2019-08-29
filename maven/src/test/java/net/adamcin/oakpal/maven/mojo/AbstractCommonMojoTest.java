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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AbstractCommonMojoTest {

    @Test
    public void testGettersWithMocks() {
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        final MojoExecution execution = mock(MojoExecution.class);
        final MavenSession session = mock(MavenSession.class);
        final Settings settings = mock(Settings.class);
        final MavenProject project = mock(MavenProject.class);

        ConcreteMojo mojo = new ConcreteMojo();
        assertNull("repositorySystem is null", mojo.getRepositorySystem());
        mojo.setRepositorySystem(repositorySystem);
        assertSame("same repositorySystem", repositorySystem, mojo.getRepositorySystem());
        assertNull("execution is null", mojo.getExecution());
        mojo.setExecution(execution);
        assertSame("same execution", execution, mojo.getExecution());
        assertNull("session is null", mojo.getSession());
        mojo.setSession(session);
        assertSame("same session", session, mojo.getSession());
        assertNull("settings is null", mojo.getSettings());
        mojo.setSettings(settings);
        assertSame("same settings", settings, mojo.getSettings());
        assertFalse("project is empty optional", mojo.getProject().isPresent());
        mojo.setProject(project);
        assertSame("same project", project, mojo.getProject().get());

    }

    static class ConcreteMojo extends AbstractCommonMojo {

        void setRepositorySystem(RepositorySystem repositorySystem) {
            this.repositorySystem = repositorySystem;
        }

        void setExecution(MojoExecution execution) {
            this.execution = execution;
        }

        void setSession(MavenSession session) {
            this.session = session;
        }

        void setSettings(Settings settings) {
            this.settings = settings;
        }

        void setProject(MavenProject project) {
            this.project = project;
        }

        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {

        }
    }
}