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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.Lock;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class LockFacadeTest {
    LockFacade<Session> getFacade(final @NotNull Lock mockLock) {
        return new LockFacade<>(mockLock, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test
    public void testGetLockOwner() {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        final String value = "owner";
        when(delegate.getLockOwner()).thenReturn(value);
        assertSame("same value", value, facade.getLockOwner());
    }

    @Test
    public void testIsDeep() {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        when(delegate.isDeep()).thenReturn(true);
        assertTrue("is true", facade.isDeep());
    }

    @Test
    public void testGetNode() throws Exception {
        new FacadeGetterMapping.Tester<>(Lock.class, this::getFacade)
                .testFacadeGetter(Node.class, NodeFacade.class, Lock::getNode);
    }

    @Test
    public void testGetLockToken() {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        final String value = "owner";
        when(delegate.getLockToken()).thenReturn(value);
        assertSame("same value", value, facade.getLockToken());
    }

    @Test
    public void testGetSecondsRemaining() throws Exception {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        final long value = 42L;
        when(delegate.getSecondsRemaining()).thenReturn(value);
        assertSame("same value", value, facade.getSecondsRemaining());
    }

    @Test
    public void testIsLive() throws Exception {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        when(delegate.isLive()).thenReturn(true);
        assertTrue("is true", facade.isLive());
    }

    @Test
    public void testIsSessionScoped() {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        when(delegate.isSessionScoped()).thenReturn(true);
        assertTrue("is true", facade.isSessionScoped());
    }

    @Test
    public void testIsLockOwningSession() {
        Lock delegate = mock(Lock.class);
        LockFacade<Session> facade = getFacade(delegate);
        when(delegate.isLockOwningSession()).thenReturn(true);
        assertTrue("is true", facade.isLockOwningSession());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRefresh() throws Exception {
        Lock mockLock = mock(Lock.class);
        LockFacade<Session> facade = getFacade(mockLock);
        facade.refresh();
    }
}