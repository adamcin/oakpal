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

public class OpearPackageMojoTest extends OakpalMojoTestCaseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpearPackageMojoTest.class);

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
        File buildPlanPom = getTestFile("src/test/resources/unit/opear-plan-for-opear1/pom.xml");
        File buildPlanTarget = new File(buildPlanPom.getParentFile(), "target");
        FileUtils.deleteDirectory(buildPlanTarget);
        assertNotNull(buildPlanPom);
        assertTrue(buildPlanPom.exists());
        FileUtils.copyFile(
                new File(buildPlanPom.getParentFile(), "src/main/resources/simpleCnd.cnd"),
                new File(buildPlanTarget, "classes/simpleCnd.cnd"));

        SessionAndProject buildPlanPair = buildProject(buildPlanPom);
        try {
            OpearPlanMojo planMojo = (OpearPlanMojo) lookupConfiguredMojo(buildPlanPair.getProject(),
                    "opear-plan");
            assertNotNull("planMojo null", planMojo);
            planMojo.execute();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }

        File opearPom = getTestFile("src/test/resources/unit/opear1/pom.xml");

        assertNotNull(opearPom);
        assertTrue(opearPom.exists());

        File opearTarget = new File(opearPom.getParentFile(), "target");
        FileUtils.deleteDirectory(opearTarget);
        FileUtils.copyFile(
                new File(buildPlanTarget, "opear-plans/plan.json"),
                new File(opearTarget, "plan.json"));

        SessionAndProject opearPair = buildProject(opearPom);

        try {
            OpearPackageMojo opearMojo =
                    (OpearPackageMojo) lookupConfiguredMojo(opearPair.getProject(), "opear-package");
            assertNotNull("opearMojo null", opearMojo);
            opearMojo.execute();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

}
