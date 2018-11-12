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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.ContainerConfiguration;
import org.junit.Rule;
import org.junit.Test;

public class ScanArtifactMojoTest extends OakpalMojoTestCaseBase {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    @Override
    protected void setUp() throws Exception {
        try {
            super.setUp();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

    @Override
    protected ContainerConfiguration setupContainerConfiguration() {
        return super.setupContainerConfiguration();
    }

    @Test
    public void testExecute() throws Exception {
        File pom = getTestFile("src/test/resources/unit/project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        SessionAndProject pair = buildProject(pom);

        pair.getProject().getArtifact().setFile(TestPackageUtil.prepareTestPackage("fullcoverage.zip"));

        try {
            ScanArtifactMojo myMojo = (ScanArtifactMojo) lookupConfiguredMojo(pair.getProject(), "scan");
            assertNotNull("myMojo null", myMojo);
            myMojo.summaryFile.getParentFile().mkdirs();
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(myMojo.getContainerClassLoader());
                myMojo.executeGuardedIntegrationTest();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

}
