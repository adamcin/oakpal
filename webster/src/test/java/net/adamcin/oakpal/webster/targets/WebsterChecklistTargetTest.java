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

package net.adamcin.oakpal.webster.targets;

import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.webster.TestUtil;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.io.FileArchive;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.*;

public class WebsterChecklistTargetTest {
    final File testOutDir = new File("target/test-out/WebsterChecklistTargetTest");

    @Before
    public void setUp() throws Exception {
        testOutDir.mkdirs();
    }

    @Test
    public void testFromJsonTargetFactory() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJsonTargetFactory.json");
        WebsterTarget target = JsonTargetFactory.CHECKLIST.createTarget(targetFile, obj().get());
        assertTrue("target is checklist target: " + target.getClass().getSimpleName(),
                target instanceof WebsterChecklistTarget);
    }

    @Test
    public void testFromJson() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson.json");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj().get());
        WebsterChecklistTarget target = WebsterChecklistTarget.fromJson(targetFile, obj().get());
        builder.build().initAndInspect(target::perform);
    }
}