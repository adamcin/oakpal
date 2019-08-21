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

import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.webster.TestUtil;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.io.FileArchive;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.webster.TestUtil.assertFileContains;
import static net.adamcin.oakpal.webster.targets.WebsterNodetypesTargetTest.assertFileNotContains;
import static org.junit.Assert.*;

public class WebsterPrivilegesTargetTest {

    final File testOutDir = new File("target/test-out/WebsterPrivilegesTargetTest");

    @Before
    public void setUp() throws Exception {
        testOutDir.mkdirs();
    }

    @Test
    public void testFromJsonTargetFactory() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJsonTargetFactory.xml");
        WebsterTarget target = JsonTargetFactory.PRIVILEGES.createTarget(targetFile, obj().get());
        assertTrue("target is privileges target: " + target.getClass().getSimpleName(),
                target instanceof WebsterPrivilegesTarget);
    }

    @Test
    public void testConstruct() throws Exception {
        final File targetFile = new File(testOutDir, "testConstruct.xml");
        final File writeBack = new File(testOutDir, "testConstruct_writeback");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj().get());
        WebsterPrivilegesTarget target = new WebsterPrivilegesTarget(targetFile);
        target.setArchive(new FileArchive(new File("src/test/resources/filevault/onePrivRef")), writeBack);
        builder.build().initAndInspect(target::perform);

        assertFileContains(targetFile, "<privileges/>");
    }

    @Test
    public void testWithExisting() throws Exception {
        final File targetFile = new File(testOutDir, "testWithExisting.xml");
        final File writeBack = new File(testOutDir, "testWithExisting_writeback");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", arr(JcrNs.create("foo", "http://adamcin.net/foo")))
                .key("jcrPrivileges", arr("foo:canBar"))
                .get());
        WebsterPrivilegesTarget target = new WebsterPrivilegesTarget(targetFile);
        target.setArchive(new FileArchive(new File("src/test/resources/filevault/onePrivRef")), writeBack);
        builder.build().initAndInspect(target::perform);

        assertFileNotContains(targetFile, "<privileges/>");
        assertFileContains(targetFile, "name=\"foo:canBar\"");
    }


}