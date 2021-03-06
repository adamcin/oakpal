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

package net.adamcin.oakpal.core.checks;

import net.adamcin.oakpal.api.SilenceableCheck;
import org.junit.Test;

import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.*;

public class SlingJcrInstallerTest {

    @Test
    public void testNullSafety() throws Exception {
        new SlingJcrInstaller().newInstance(obj().get())
                .importedPath(null, null, null, null);
    }

    @Test
    public void testSetSilencedSafety() {
        ((SilenceableCheck) new SlingJcrInstaller().newInstance(obj().get())).setSilenced(true);
    }

    @Test
    public void testNewInstanceNegMaxDepth() {
        new SlingJcrInstaller().newInstance(key(SlingJcrInstaller.keys().maxDepth(), -1).get());
    }
}