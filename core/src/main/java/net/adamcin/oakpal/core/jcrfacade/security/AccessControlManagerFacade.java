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

package net.adamcin.oakpal.core.jcrfacade.security;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps {@link AccessControlManager} to prevent writes by handlers.
 *
 * @param <M> the AccessControlManager type, likely {@link JackrabbitAccessControlManager}
 */
public class AccessControlManagerFacade<M extends AccessControlManager> implements AccessControlManager {

    protected final @NotNull M delegate;

    @SuppressWarnings("WeakerAccess")
    public AccessControlManagerFacade(final @NotNull M delegate) {
        this.delegate = delegate;
    }

    public static @Nullable AccessControlManager
    findBestWrapper(final @Nullable AccessControlManager manager) {
        if (manager instanceof JackrabbitAccessControlManager) {
            return new JackrabbitAccessControlManagerFacade((JackrabbitAccessControlManager) manager);
        } else if (manager != null) {
            return new AccessControlManagerFacade<>(manager);
        } else {
            return null;
        }
    }

    @Override
    public final Privilege[] getSupportedPrivileges(String absPath) throws RepositoryException {
        return delegate.getSupportedPrivileges(absPath);
    }

    @Override
    public final Privilege privilegeFromName(String privilegeName) throws RepositoryException {
        return delegate.privilegeFromName(privilegeName);
    }

    @Override
    public final boolean hasPrivileges(String absPath, Privilege[] privileges) throws RepositoryException {
        return delegate.hasPrivileges(absPath, privileges);
    }

    @Override
    public final Privilege[] getPrivileges(String absPath) throws RepositoryException {
        return delegate.getPrivileges(absPath);
    }

    @Override
    public final AccessControlPolicy[] getPolicies(String absPath) throws RepositoryException {
        return delegate.getPolicies(absPath);
    }

    @Override
    public final AccessControlPolicy[] getEffectivePolicies(String absPath) throws RepositoryException {
        return delegate.getEffectivePolicies(absPath);
    }

    @Override
    public final AccessControlPolicyIterator getApplicablePolicies(String absPath) throws RepositoryException {
        return delegate.getApplicablePolicies(absPath);
    }

    @Override
    public final void setPolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void removePolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
