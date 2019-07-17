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
import javax.jcr.Value;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AuthorizableFacade<A extends Authorizable> implements Authorizable {

    protected final @NotNull A delegate;

    @SuppressWarnings("WeakerAccess")
    public AuthorizableFacade(final @NotNull A delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("WeakerAccess")
    public static @Nullable Authorizable ensureBestWrapper(final @Nullable Authorizable authorizable) {
        if (authorizable == null) {
            return null;
        } else if (authorizable instanceof AuthorizableFacade) {
            return authorizable;
        } else if (authorizable instanceof User) {
            return new UserFacade((User) authorizable);
        } else if (authorizable instanceof Group) {
            return new GroupFacade((Group) authorizable);
        } else {
            return new AuthorizableFacade<>(authorizable);
        }
    }

    @Override
    public String getID() throws RepositoryException {
        return delegate.getID();
    }

    @Override
    public boolean isGroup() {
        return delegate.isGroup();
    }

    @Override
    public Principal getPrincipal() throws RepositoryException {
        return delegate.getPrincipal();
    }

    @Override
    public Iterator<String> getPropertyNames() throws RepositoryException {
        return delegate.getPropertyNames();
    }

    @Override
    public Iterator<String> getPropertyNames(final String relPath) throws RepositoryException {
        return delegate.getPropertyNames(relPath);
    }

    @Override
    public boolean hasProperty(final String relPath) throws RepositoryException {
        return delegate.hasProperty(relPath);
    }

    @Override
    public Value[] getProperty(final String relPath) throws RepositoryException {
        return delegate.getProperty(relPath);
    }

    @Override
    public String getPath() throws RepositoryException {
        return delegate.getPath();
    }

    @Override
    public Iterator<Group> declaredMemberOf() throws RepositoryException {
        Iterator<Group> internal = delegate.declaredMemberOf();
        return Iterators.transformIterator(internal, GroupFacade::new);
    }

    @Override
    public Iterator<Group> memberOf() throws RepositoryException {
        Iterator<Group> internal = delegate.memberOf();
        return Iterators.transformIterator(internal, GroupFacade::new);
    }


    @Override
    public final void setProperty(final String relPath, final Value value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void setProperty(final String relPath, final Value[] value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void remove() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final boolean removeProperty(final String relPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

}
