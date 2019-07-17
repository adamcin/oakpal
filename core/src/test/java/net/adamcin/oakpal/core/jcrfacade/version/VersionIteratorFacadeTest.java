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

package net.adamcin.oakpal.core.jcrfacade.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class VersionIteratorFacadeTest {

    VersionIteratorFacade<Session> getFacade(final @NotNull VersionIterator delegate) {
        return new VersionIteratorFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testNextVersion() throws Exception {
        VersionIterator delegate = mock(VersionIterator.class);
        VersionIteratorFacade<Session> facade = getFacade(delegate);
        final Version value = mock(Version.class);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        when(delegate.nextVersion()).thenReturn(value);
        final Version fromFacade = facade.nextVersion();
        assertEquals("same path", path, fromFacade.getPath());
        assertTrue("is facade", fromFacade instanceof VersionFacade);
    }

    @Test
    public void testNext() throws Exception {
        VersionIterator delegate = mock(VersionIterator.class);
        VersionIteratorFacade<Session> facade = getFacade(delegate);
        final Version value = mock(Version.class);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        when(delegate.nextVersion()).thenReturn(value);
        final Object fromFacade = facade.next();
        assertTrue("is version", fromFacade instanceof Version);
        final Version nodeFromFacade = (Version) fromFacade;
        assertEquals("same path", path, nodeFromFacade.getPath());
        assertTrue("is facade", nodeFromFacade instanceof VersionFacade);
    }
}