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

import static net.adamcin.oakpal.core.OrgJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.adamcin.oakpal.core.checks.Echo;
import net.adamcin.oakpal.core.checks.Paths;
import org.junit.Test;

public class LocatorTest {

    public static class NotACheck {

    }

    @Test
    public void testLoadProgressCheck() throws Exception {

        assertNotNull("echo should load by class name",
                Locator.loadProgressCheck(Echo.class.getName()));
        assertNotNull("echo should load by class name, with config",
                Locator.loadProgressCheck(Echo.class.getName(), obj().get()));
        assertNotNull("echo should load by class name, with config, with cl",
                Locator.loadProgressCheck(Echo.class.getName(), obj().get(), getClass().getClassLoader()));

        assertNotNull("paths should load by class name, with config, with cl",
                Locator.loadProgressCheck(Paths.class.getName(), obj().get(), getClass().getClassLoader()));

        assertNotNull("simpleHandler.js should load by resource name, with config, with cl",
                Locator.loadProgressCheck("simpleHandler.js", obj().get(), getClass().getClassLoader()));

        boolean notACheckThrew = false;
        try {
            Locator.loadProgressCheck(NotACheck.class.getName());
        } catch (final Exception e) {
            notACheckThrew = true;
        }

        assertTrue("locator should throw when attempting to load NotACheck", notACheckThrew);

        boolean notAClassThrew = false;
        try {
            Locator.loadProgressCheck("com.example.NotARealClassName");
        } catch (final Exception e) {
            notAClassThrew = true;
            assertTrue("locator should throw ClassNotFoundException when impl is not a real class name and not a script resource.",
                    e instanceof ClassNotFoundException);
        }

        assertTrue("locator should throw when attempting to load NotARealClass", notAClassThrew);

        boolean notAScriptThrew = false;
        try {
            Locator.loadProgressCheck("scripts/notAScript.js");
        } catch (final Exception e) {
            notAScriptThrew = true;
        }

        assertTrue("locator should throw when attempting to load a non-existent script", notAScriptThrew);
    }

    @Test
    public void testWrapAlias() throws Exception {
        ProgressCheck echo = Locator.loadProgressCheck(Echo.class.getName());
        ProgressCheck echoAlias = Locator.wrapWithAlias(echo, "echoAlias");
        assertEquals("echo alias check name should be", "echoAlias", echoAlias.getCheckName());
    }
}
