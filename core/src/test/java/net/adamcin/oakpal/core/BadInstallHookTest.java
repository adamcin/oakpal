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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Test;

import java.io.File;

public class BadInstallHookTest {

    @Test(expected = AbortedScanException.class)
    public void testBadInstallHook() throws Exception {
        File badInstallHookPackage = TestPackageUtil.prepareTestPackageFromFolder("bad-install-hook.zip",
                new File("src/test/resources/package_with_bad_installhook"));

        new OakMachine.Builder().build().scanPackage(badInstallHookPackage);
    }

    @Test()
    public void testBadInstallHookNoOpProcessor() throws Exception {
        File badInstallHookPackage = TestPackageUtil.prepareTestPackageFromFolder("bad-install-hook.zip",
                new File("src/test/resources/package_with_bad_installhook"));

        new OakMachine.Builder().withSkipInstallHooks(true)
                .build().scanPackage(badInstallHookPackage);
    }
}
