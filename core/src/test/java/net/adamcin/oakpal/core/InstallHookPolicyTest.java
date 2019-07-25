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

package net.adamcin.oakpal.core;

import static net.adamcin.oakpal.core.InstallHookPolicy.ABORT;
import static net.adamcin.oakpal.core.InstallHookPolicy.PROHIBIT;
import static net.adamcin.oakpal.core.InstallHookPolicy.REPORT;
import static net.adamcin.oakpal.core.InstallHookPolicy.SKIP;
import static org.junit.Assert.*;

import org.junit.Test;

public class InstallHookPolicyTest {

    @Test
    public void testForName() {
        assertNull("forName null is null", InstallHookPolicy.forName(null));
        assertSame("forName prohibit", PROHIBIT, InstallHookPolicy.forName("prohibit"));
        assertSame("forName report", REPORT, InstallHookPolicy.forName("report"));
        assertSame("forName abort", ABORT, InstallHookPolicy.forName("abort"));
        assertSame("forName skip", SKIP, InstallHookPolicy.forName("skip"));
        assertSame("forName default is report", REPORT, InstallHookPolicy.DEFAULT);
    }
}