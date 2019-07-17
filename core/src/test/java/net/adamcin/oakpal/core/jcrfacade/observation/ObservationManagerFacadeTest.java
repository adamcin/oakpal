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

package net.adamcin.oakpal.core.jcrfacade.observation;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.observation.EventJournal;
import javax.jcr.observation.ObservationManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ObservationManagerFacadeTest {

    ObservationManagerFacade getFacade(final @NotNull ObservationManager mockManager) {
        return new ObservationManagerFacade(mockManager);
    }

    @Test
    public void testGetRegisteredEventListeners() throws Exception {
        ObservationManagerFacade facade = getFacade(mock(ObservationManager.class));
        assertTrue("should always return an empty iterator",
                facade.getRegisteredEventListeners() instanceof EmptyEventListenerIterator);
    }

    @Test
    public void testGetEventJournal() throws Exception {
        ObservationManager delegate = mock(ObservationManager.class);
        ObservationManagerFacade facade = getFacade(delegate);
        final EventJournal value = mock(EventJournal.class);
        when(delegate.getEventJournal()).thenReturn(value);
        assertSame("same value", value, facade.getEventJournal());
    }

    @Test
    public void testGetEventJournalArgs() throws Exception {
        ObservationManager delegate = mock(ObservationManager.class);
        ObservationManagerFacade facade = getFacade(delegate);
        final EventJournal value = mock(EventJournal.class);
        when(delegate.getEventJournal(1, "", true, null, null)).thenReturn(value);
        assertSame("same value", value, facade.getEventJournal(1, "", true, null, null));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetUserData() throws Exception {
        ObservationManagerFacade facade = getFacade(mock(ObservationManager.class));
        facade.setUserData("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddEventListener() throws Exception {
        ObservationManagerFacade facade = getFacade(mock(ObservationManager.class));
        facade.addEventListener(null, 0, "", true,
                new String[0], new String[0], true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveEventListener() throws Exception {
        ObservationManagerFacade facade = getFacade(mock(ObservationManager.class));
        facade.removeEventListener(null);
    }
}