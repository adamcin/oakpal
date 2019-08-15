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

package net.adamcin.oakpal.cli;

import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

public class ConsoleTest {

    @Test
    public void testGetCwd() {
        final Console console = mock(Console.class);
        doCallRealMethod().when(console).getCwd();
        assertEquals("same cwd", new File("."), console.getCwd());
    }

    @Test
    public void testGetEnv() {
        final Console console = mock(Console.class);
        doCallRealMethod().when(console).getEnv();
        assertTrue("env is empty map", console.getEnv().isEmpty());
    }

    @Test
    public void testGetSystemProperties() {
        final Console console = mock(Console.class);
        doCallRealMethod().when(console).getSystemProperties();
        Properties props = console.getSystemProperties();
        assertEquals("system properties is equal to System.getProperties()",
                new Properties(System.getProperties()), props);
    }
}