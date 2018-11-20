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

import java.util.Iterator;
import java.util.Set;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

public class GroupFacade extends AuthorizableFacade<Group> implements Group {

    public GroupFacade(final Group delegate) {
        super(delegate);
    }

    @Override
    public Iterator<Authorizable> getDeclaredMembers() throws RepositoryException {
        Iterator<Authorizable> internal = delegate.getDeclaredMembers();
        return null;
    }

    @Override
    public Iterator<Authorizable> getMembers() throws RepositoryException {
        Iterator<Authorizable> internal = delegate.getMembers();
        return null;
    }

    @Override
    public boolean isDeclaredMember(final Authorizable authorizable) throws RepositoryException {
        return delegate.isDeclaredMember(authorizable);
    }

    @Override
    public boolean isMember(final Authorizable authorizable) throws RepositoryException {
        return delegate.isMember(authorizable);
    }

    @Override
    public boolean addMember(final Authorizable authorizable) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Set<String> addMembers(final String... memberIds) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean removeMember(final Authorizable authorizable) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Set<String> removeMembers(final String... memberIds) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
