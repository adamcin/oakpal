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
import javax.jcr.lock.LockManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class LockManagerFacadeTest {

    LockManagerFacade<Session> getFacade(final @NotNull LockManager mockLockManager) {
        return new LockManagerFacade<>(mockLockManager, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddLockToken() throws Exception {
        LockManagerFacade<Session> facade = getFacade(mock(LockManager.class));
        facade.addLockToken("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveLockToken() throws Exception {
        LockManagerFacade<Session> facade = getFacade(mock(LockManager.class));
        facade.removeLockToken("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROLock() throws Exception {
        LockManagerFacade<Session> facade = getFacade(mock(LockManager.class));
        facade.lock("", true, true, 0L, "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROUnlock() throws Exception {
        LockManagerFacade<Session> facade = getFacade(mock(LockManager.class));
        facade.unlock("");
    }
}