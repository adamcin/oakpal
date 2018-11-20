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
import org.xml.sax.ContentHandler;

/**
 * Wraps {@link Workspace} to prevent writes.
 */
public class WorkspaceFacade<S extends Session> implements Workspace {

    private final SessionFacade<S> session;
    private final Workspace delegate;

    public WorkspaceFacade(Workspace delegate, SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public Session getSession() {
        return this.session;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void copy(String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void move(String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void restore(Version[] versions, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public LockManager getLockManager() throws RepositoryException {
        LockManager internal = delegate.getLockManager();
        return new LockManagerFacade<>(internal, session);
    }

    @Override
    public QueryManager getQueryManager() throws RepositoryException {
        QueryManager internal = delegate.getQueryManager();
        return new QueryManagerFacade<>(internal, session);
    }

    @Override
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        NamespaceRegistry internal = delegate.getNamespaceRegistry();
        return new NamespaceRegistryFacade(internal);
    }

    @Override
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        NodeTypeManager internal = delegate.getNodeTypeManager();
        return new NodeTypeManagerFacade(internal);
    }

    @Override
    public ObservationManager getObservationManager() throws RepositoryException {
        ObservationManager internal = delegate.getObservationManager();
        return new ObservationManagerFacade(internal);
    }

    @Override
    public VersionManager getVersionManager() throws RepositoryException {
        VersionManager internal = delegate.getVersionManager();
        return new VersionManagerFacade<>(internal, session);
    }

    @Override
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        return delegate.getAccessibleWorkspaceNames();
    }

    @Override
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void createWorkspace(String name) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void createWorkspace(String name, String srcWorkspace) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void deleteWorkspace(String name) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
