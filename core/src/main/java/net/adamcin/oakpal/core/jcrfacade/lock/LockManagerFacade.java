/*
 * Copyright 2018 Mark Adamcin
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;

/**
 * Wraps a {@link LockManager} to prevent lock modifications and to wrap
 * retrieved {@link Lock} instances.
 */
public class LockManagerFacade<S extends Session> implements LockManager {
    private final LockManager delegate;
    private final SessionFacade<S> session;

    public LockManagerFacade(LockManager delegate, SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public void addLockToken(String lockToken) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Lock getLock(String absPath) throws RepositoryException {
        Lock internal = delegate.getLock(absPath);
        return new LockFacade<>(internal, session);
    }

    @Override
    public String[] getLockTokens() throws RepositoryException {
        return delegate.getLockTokens();
    }

    @Override
    public boolean holdsLock(String absPath) throws RepositoryException {
        return delegate.holdsLock(absPath);
    }

    @Override
    public Lock lock(String absPath, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerInfo) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean isLocked(String absPath) throws RepositoryException {
        return delegate.isLocked(absPath);
    }

    @Override
    public void removeLockToken(String lockToken) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void unlock(String absPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
