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

package net.adamcin.oakpal.core.jcrfacade.security.user;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;

public class UserFacade extends AuthorizableFacade<User> implements User {

    public UserFacade(final User delegate) {
        super(delegate);
    }

    @Override
    public boolean isAdmin() {
        return delegate.isAdmin();
    }

    @Override
    public boolean isSystemUser() {
        return delegate.isSystemUser();
    }

    @Override
    public Credentials getCredentials() throws RepositoryException {
        return delegate.getCredentials();
    }

    @Override
    public Impersonation getImpersonation() throws RepositoryException {
        Impersonation internal = delegate.getImpersonation();
        return new ImpersonationFacade(internal);
    }

    @Override
    public void changePassword(final String password) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void changePassword(final String password, final String oldPassword) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void disable(final String reason) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean isDisabled() throws RepositoryException {
        return delegate.isDisabled();
    }

    @Override
    public String getDisabledReason() throws RepositoryException {
        return delegate.getDisabledReason();
    }
}
