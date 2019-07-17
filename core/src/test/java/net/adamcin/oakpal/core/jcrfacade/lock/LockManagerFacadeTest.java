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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class LockManagerFacadeTest {

    LockManagerFacade<Session> getFacade(final @NotNull LockManager mockLockManager) {
        return new LockManagerFacade<>(mockLockManager, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test
    public void testGetLock() throws Exception {
        new FacadeGetterMapping.Tester<>(LockManager.class, this::getFacade)
                .testFacadeGetter(Lock.class, LockFacade.class, delegate -> delegate.getLock(""));
    }

    @Test
    public void testGetLockTokens() throws Exception {
        LockManager delegate = mock(LockManager.class);
        LockManagerFacade<Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getLockTokens()).thenReturn(value);
        assertSame("is same value", value, facade.getLockTokens());
    }

    @Test
    public void testHoldsLock() throws Exception {
        LockManager delegate = mock(LockManager.class);
        LockManagerFacade<Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.holdsLock(path)).thenReturn(true);
        assertTrue("is true", facade.holdsLock(path));
    }

    @Test
    public void testIsLocked() throws Exception {
        LockManager delegate = mock(LockManager.class);
        LockManagerFacade<Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.isLocked(path)).thenReturn(true);
        assertTrue("is true", facade.isLocked(path));
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