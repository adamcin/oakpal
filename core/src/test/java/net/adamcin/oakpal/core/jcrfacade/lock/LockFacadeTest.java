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

package net.adamcin.oakpal.core.jcrfacade.lock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import javax.jcr.Session;
import javax.jcr.lock.Lock;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class LockFacadeTest {
    LockFacade<Session> getFacade(final @NotNull Lock mockLock) {
        return new LockFacade<>(mockLock, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRefresh() throws Exception {
        Lock mockLock = mock(Lock.class);
        LockFacade<Session> facade = getFacade(mockLock);
        facade.refresh();
    }


}