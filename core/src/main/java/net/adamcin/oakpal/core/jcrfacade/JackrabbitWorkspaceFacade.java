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

package net.adamcin.oakpal.core.jcrfacade;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.security.authorization.PrivilegeManagerFacade;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Wraps a {@link JackrabbitWorkspace} to guard against writes by listeners.
 *
 * @param <S> Session type, likely to be {@link org.apache.jackrabbit.api.JackrabbitSession}.
 */
public final class JackrabbitWorkspaceFacade<S extends Session>
        extends WorkspaceFacade<JackrabbitWorkspace, S> implements JackrabbitWorkspace {

    @SuppressWarnings("WeakerAccess")
    public JackrabbitWorkspaceFacade(final @NotNull JackrabbitWorkspace delegate,
                                     final @NotNull SessionFacade<S> session) {
        super(delegate, session);
    }

    @Override
    public PrivilegeManager getPrivilegeManager() throws RepositoryException {
        return new PrivilegeManagerFacade(delegate.getPrivilegeManager());
    }

    @Override
    public final void createWorkspace(final String workspaceName, final InputSource workspaceTemplate)
            throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

}
