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

import java.security.Principal;
import java.util.Iterator;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps {@link UserManager} to prevent authorizable modifications and to wrap retrieved {@link Authorizable},
 * {@link User}, and {@link Group} instances.
 */
public final class UserManagerFacade implements UserManager {
    private final @NotNull UserManager delegate;

    public UserManagerFacade(final @NotNull UserManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isAutoSave() {
        return false;
    }

    @Override
    public Authorizable getAuthorizable(final String id) throws RepositoryException {
        Authorizable internal = delegate.getAuthorizable(id);
        return AuthorizableFacade.ensureBestWrapper(internal);
    }

    @Override
    public <T extends Authorizable> T getAuthorizable(final String id, final Class<T> authorizableClass) throws RepositoryException {
        T internal = delegate.getAuthorizable(id, authorizableClass);
        if (internal == null) {
            return null;
        }

        Authorizable wrapped = AuthorizableFacade.ensureBestWrapper(internal);
        if (authorizableClass.isAssignableFrom(wrapped.getClass())) {
            return authorizableClass.cast(wrapped);
        } else {
            throw new AuthorizableTypeException("Authorizable type not supported: " + authorizableClass.getName());
        }
    }

    @Override
    public Authorizable getAuthorizable(final Principal principal) throws RepositoryException {
        Authorizable internal = delegate.getAuthorizable(principal);
        return AuthorizableFacade.ensureBestWrapper(internal);
    }

    @Override
    public Authorizable getAuthorizableByPath(final String path) throws RepositoryException {
        Authorizable internal = delegate.getAuthorizableByPath(path);
        return AuthorizableFacade.ensureBestWrapper(internal);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(final String relPath, final String value) throws RepositoryException {
        Iterator<Authorizable> internal = delegate.findAuthorizables(relPath, value);
        return Iterators.transformIterator(internal, AuthorizableFacade::ensureBestWrapper);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(final String relPath, final String value, final int searchType) throws RepositoryException {
        Iterator<Authorizable> internal = delegate.findAuthorizables(relPath, value, searchType);
        return Iterators.transformIterator(internal, AuthorizableFacade::ensureBestWrapper);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(final Query query) throws RepositoryException {
        Iterator<Authorizable> internal = delegate.findAuthorizables(query);
        return Iterators.transformIterator(internal, AuthorizableFacade::ensureBestWrapper);
    }

    @Override
    public User createUser(final String userID, final String password) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public User createUser(final String userID, final String password, final Principal principal, final String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public User createSystemUser(final String userID, final String intermediatePath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Group createGroup(final String groupID) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Group createGroup(final Principal principal) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Group createGroup(final Principal principal, final String intermediatePath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Group createGroup(final String groupID, final Principal principal, final String intermediatePath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void autoSave(final boolean enable) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
