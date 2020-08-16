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

package net.adamcin.oakpal.core.installable;

import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.junit.Test;

import javax.jcr.Session;

import java.util.Iterator;

import static net.adamcin.oakpal.core.installable.NoopInstallWatcher.instance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class NoopInstallWatcherTest {

    @Test
    public void testInstance() {
        assertNotNull("expect non-null instance", instance());
    }

    @Test
    public void testGetCheckName() {
        assertEquals("expect checkName",
                NoopInstallWatcher.class.getSimpleName(), instance().getCheckName());
    }

    @Test
    public void testIteratable() {
        Iterator<PathInstallable<?>> iterator = instance().iterator();
        assertNotNull("expect non-null iterator", iterator);
        assertFalse("iterator is empty", iterator.hasNext());
    }

    @Test
    public void testOpenRepoInitInstallable() {
        assertNotNull("expect non-null iteratable",
                instance().open(
                        new RepoInitInstallable(null, null),
                        mock(Session.class),
                        mock(JcrPackageManager.class)));
    }

    @Test
    public void testOpenSubpackageInstallable() {
        assertNotNull("expect non-null iteratable",
                instance().open(
                        new EmbeddedPackageInstallable(null, null),
                        mock(Session.class),
                        mock(JcrPackageManager.class)));
    }

    @Test
    public void testSetSilenced() {
        instance().setSilenced(true);
    }

    @Test
    public void testGetReportedViolations() {
        assertNotNull("expect non-null iteratable",
                instance().getReportedViolations());
    }
}