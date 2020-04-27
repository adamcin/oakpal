/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.core.opear;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.Result;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AdhocOpearTest {

    @Test
    public void testFromPlanFile() {
        final File planFile = new File("src/test/resources/OpearFileTest/folders_on_classpath/plan.json");
        Result<AdhocOpear> opearResult = AdhocOpear.fromPlanFile(planFile, null);

        assertFalse("opear should not be a failure", opearResult.isFailure());
        AdhocOpear opear = opearResult.getOrDefault(null);
        assertNotNull("opear is not null", opear);

        assertEquals("expect planFile is referenced by default plan url",
                planFile.getAbsolutePath(),
                Fun.tryOrOptional0(() -> new File(opear.getDefaultPlan().toURI()).getAbsolutePath()).get().orElse(""));

        assertEquals("expect planFile is referenced by specific plan referenced by empty plan name",
                planFile.getAbsolutePath(), opear.getSpecificPlan("")
                        .flatMap(Fun.result1(URL::toURI))
                        .map(Fun.compose(File::new, File::getAbsolutePath)).getOrDefault(""));

        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader planCl = opear.getPlanClassLoader(parent);
        assertNotSame("not same classloader", parent, planCl);
        assertSame("expect same parent", parent, planCl.getParent());

        assertTrue("expect instance of URLClassLoader", planCl instanceof URLClassLoader);
        final URL firstUrl = ((URLClassLoader) planCl).getURLs()[0];

        assertEquals("expect planFile parent is referenced by first url",
                planFile.getParentFile().getAbsolutePath(),
                Fun.tryOrOptional0(() -> new File(firstUrl.toURI()).getAbsolutePath()).get().orElse(""));
    }

    @Test
    public void testFromPlanFileWithBaseDir() {
        final File targetDir = new File("target");
        final File planFile = new File("src/test/resources/OpearFileTest/folders_on_classpath/plan.json");
        Result<AdhocOpear> opearResult = AdhocOpear.fromPlanFile(planFile, targetDir);

        assertFalse("opear should not be a failure", opearResult.isFailure());
        AdhocOpear opear = opearResult.getOrDefault(null);
        assertNotNull("opear is not null", opear);

        assertEquals("expect planFile is referenced by default plan url",
                planFile.getAbsolutePath(),
                Fun.tryOrOptional0(() -> new File(opear.getDefaultPlan().toURI()).getAbsolutePath()).get().orElse(""));

        assertEquals("expect planFile is referenced by specific plan referenced by empty plan name",
                planFile.getAbsolutePath(), opear.getSpecificPlan("")
                        .flatMap(Fun.result1(URL::toURI))
                        .map(Fun.compose(File::new, File::getAbsolutePath)).getOrDefault(""));

        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader planCl = opear.getPlanClassLoader(parent);
        assertNotSame("not same classloader", parent, planCl);
        assertSame("expect same parent", parent, planCl.getParent());

        assertTrue("expect instance of URLClassLoader", planCl instanceof URLClassLoader);
        final URL firstUrl = ((URLClassLoader) planCl).getURLs()[0];

        assertEquals("expect planFile parent is referenced by first url",
                targetDir.getAbsolutePath(),
                Fun.tryOrOptional0(() -> new File(firstUrl.toURI()).getAbsolutePath()).get().orElse(""));
    }
}