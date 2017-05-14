/*
 * Copyright 2017 Mark Adamcin
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

import java.security.Principal;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;

/**
 * Wraps {@link JackrabbitAccessControlManager} to prevent writes by handlers.
 */
public class AccessControlManagerFacade implements JackrabbitAccessControlManager {

    private final JackrabbitAccessControlManager delegate;

    public AccessControlManagerFacade(JackrabbitAccessControlManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public JackrabbitAccessControlPolicy[] getApplicablePolicies(Principal principal) throws RepositoryException {
        return delegate.getApplicablePolicies(principal);
    }

    @Override
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws RepositoryException {
        return delegate.getApplicablePolicies(principal);
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals) throws RepositoryException {
        return delegate.getEffectivePolicies(principals);
    }

    @Override
    public boolean hasPrivileges(String absPath, Set<Principal> principals, Privilege[] privileges) throws RepositoryException {
        return delegate.hasPrivileges(absPath, principals, privileges);
    }

    @Override
    public Privilege[] getPrivileges(String absPath, Set<Principal> principals) throws RepositoryException {
        return delegate.getPrivileges(absPath, principals);
    }

    @Override
    public Privilege[] getSupportedPrivileges(String absPath) throws RepositoryException {
        return delegate.getSupportedPrivileges(absPath);
    }

    @Override
    public Privilege privilegeFromName(String privilegeName) throws RepositoryException {
        return delegate.privilegeFromName(privilegeName);
    }

    @Override
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws RepositoryException {
        return delegate.hasPrivileges(absPath, privileges);
    }

    @Override
    public Privilege[] getPrivileges(String absPath) throws RepositoryException {
        return delegate.getPrivileges(absPath);
    }

    @Override
    public AccessControlPolicy[] getPolicies(String absPath) throws RepositoryException {
        return delegate.getPolicies(absPath);
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws RepositoryException {
        return delegate.getEffectivePolicies(absPath);
    }

    @Override
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws RepositoryException {
        return delegate.getApplicablePolicies(absPath);
    }

    @Override
    public void setPolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void removePolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
