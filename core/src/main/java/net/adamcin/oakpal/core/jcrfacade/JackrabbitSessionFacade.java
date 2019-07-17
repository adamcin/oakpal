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

package net.adamcin.oakpal.core.jcrfacade;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.jcrfacade.security.user.UserManagerFacade;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a {@link JackrabbitSession} to guards against writes by listeners.
 */
public final class JackrabbitSessionFacade extends SessionFacade<JackrabbitSession> implements JackrabbitSession {

    @SuppressWarnings("WeakerAccess")
    public JackrabbitSessionFacade(final @NotNull JackrabbitSession delegate, final boolean notProtected) {
        super(delegate, notProtected);
    }

    @Override
    public boolean hasPermission(final @NotNull String absPath, final @NotNull String... actions)
            throws RepositoryException {
        return delegate.hasPermission(absPath, actions);
    }

    @Override
    public PrincipalManager getPrincipalManager() throws RepositoryException {
        return delegate.getPrincipalManager();
    }

    @Override
    public UserManager getUserManager() throws RepositoryException {
        UserManager internal = delegate.getUserManager();
        return new UserManagerFacade(internal);
    }

    @Override
    public Item getItemOrNull(final String absPath) throws RepositoryException {
        Item internal = delegate.getItemOrNull(absPath);
        if (internal != null) {
            return ItemFacade.ensureBestWrapper(internal, this);
        }
        return null;
    }

    @Override
    public Property getPropertyOrNull(final String absPath) throws RepositoryException {
        Property internal = delegate.getPropertyOrNull(absPath);
        if (internal != null) {
            return new PropertyFacade<>(internal, this);
        }
        return null;
    }

    @Override
    public Node getNodeOrNull(final String absPath) throws RepositoryException {
        Node internal = delegate.getNodeOrNull(absPath);
        if (internal != null) {
            return NodeFacade.wrap(internal, this);
        }
        return null;
    }
}
