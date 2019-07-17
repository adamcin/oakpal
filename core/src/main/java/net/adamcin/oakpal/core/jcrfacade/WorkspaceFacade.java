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

import java.io.InputStream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.lock.LockManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.nodetype.NodeTypeManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.observation.ObservationManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.query.QueryManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionManagerFacade;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.ContentHandler;

/**
 * Base class for wrapping a {@link Workspace} to guards against writes by listeners.
 */
public class WorkspaceFacade<W extends Workspace, S extends Session> implements Workspace {

    private final @NotNull SessionFacade<S> session;
    protected final @NotNull W delegate;

    @SuppressWarnings("WeakerAccess")
    public WorkspaceFacade(final @NotNull W delegate, final @NotNull SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @SuppressWarnings("WeakerAccess")
    public static <S extends Session> @Nullable Workspace
    findBestWrapper(final @Nullable Workspace workspace,
                    final @NotNull SessionFacade<S> sessionFacade) {
        if (workspace instanceof JackrabbitWorkspace) {
            return new JackrabbitWorkspaceFacade<>((JackrabbitWorkspace) workspace, sessionFacade);
        } else if (workspace != null) {
            return new JcrWorkspaceFacade<>(workspace, sessionFacade);
        } else {
            return null;
        }
    }

    @Override
    public final Session getSession() {
        return this.session;
    }

    @Override
    public final String getName() {
        return delegate.getName();
    }

    @Override
    public final String[] getAccessibleWorkspaceNames() throws RepositoryException {
        return delegate.getAccessibleWorkspaceNames();
    }

    @Override
    public final LockManager getLockManager() throws RepositoryException {
        LockManager internal = delegate.getLockManager();
        return new LockManagerFacade<>(internal, session);
    }

    @Override
    public final QueryManager getQueryManager() throws RepositoryException {
        QueryManager internal = delegate.getQueryManager();
        return new QueryManagerFacade<>(internal, session);
    }

    @Override
    public final NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        NamespaceRegistry internal = delegate.getNamespaceRegistry();
        return new NamespaceRegistryFacade(internal);
    }

    @Override
    public final NodeTypeManager getNodeTypeManager() throws RepositoryException {
        NodeTypeManager internal = delegate.getNodeTypeManager();
        return new NodeTypeManagerFacade(internal);
    }

    @Override
    public final ObservationManager getObservationManager() throws RepositoryException {
        ObservationManager internal = delegate.getObservationManager();
        return new ObservationManagerFacade(internal);
    }

    @Override
    public final VersionManager getVersionManager() throws RepositoryException {
        VersionManager internal = delegate.getVersionManager();
        return new VersionManagerFacade<>(internal, session);
    }

    @Override
    public final void copy(String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void move(String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void restore(Version[] versions, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void createWorkspace(String name) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void createWorkspace(String name, String srcWorkspace) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void deleteWorkspace(String name) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
