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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class VersionFacadeTest {

    VersionFacade<Session> getFacade(final @NotNull Version delegate) {
        return new VersionFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Version.class, this::getFacade)
                .testFacadeGetter(VersionHistory.class, VersionHistoryFacade.class, Version::getContainingHistory)
                .testFacadeGetter(Version.class, VersionFacade.class, Version::getLinearSuccessor)
                .testFacadeGetter(Version.class, VersionFacade.class, Version::getLinearPredecessor)
                .testFacadeGetter(Node.class, NodeFacade.class, Version::getFrozenNode)
                .testFacadeArrayGetter(Version.class, VersionFacade.class, Version::getSuccessors)
                .testFacadeArrayGetter(Version.class, VersionFacade.class, Version::getPredecessors);
    }

    @Test
    public void testGetCreated() throws Exception {
        Version delegate = mock(Version.class);
        VersionFacade<Session> facade = getFacade(delegate);
        final Calendar value = Calendar.getInstance();
        when(delegate.getCreated()).thenReturn(value);
        assertSame("is same", value, facade.getCreated());
    }

    @Test
    public void testGetSuccessorsNull() throws Exception {
        Version delegate = mock(Version.class);
        VersionFacade<Session> facade = getFacade(delegate);
        assertNull("should be null", facade.getSuccessors());
    }

    @Test
    public void testGetPredecessorsNull() throws Exception {
        Version delegate = mock(Version.class);
        VersionFacade<Session> facade = getFacade(delegate);
        assertNull("should be null", facade.getPredecessors());
    }
}