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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpearPlanMojoTest extends OakpalMojoTestCaseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpearPlanMojoTest.class);

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
    public void testExecute1() throws Exception {
        File buildPlanPom = getTestFile("src/test/resources/unit/opear-plan1/pom.xml");
        FileUtils.deleteDirectory(new File(buildPlanPom.getParentFile(), "target"));
        assertNotNull(buildPlanPom);
        assertTrue(buildPlanPom.exists());

        SessionAndProject pair = buildProject(buildPlanPom);

        try {
            OpearPlanMojo planMojo = (OpearPlanMojo) lookupConfiguredMojo(pair.getProject(), "opear-plan");
            assertNotNull("planMojo null", planMojo);
            planMojo.execute();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

}
