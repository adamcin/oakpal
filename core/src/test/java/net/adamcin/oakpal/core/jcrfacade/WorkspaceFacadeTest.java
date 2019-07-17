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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.lock.LockManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.nodetype.NodeTypeManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.observation.ObservationManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.query.QueryManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionManagerFacade;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class WorkspaceFacadeTest {

    private WorkspaceFacade<Workspace, Session> getFacade(final Workspace mockWorkspace) {
        return new JcrWorkspaceFacade<>(mockWorkspace, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test
    public void testFindBestWrapper() {
        final SessionFacade<Session> jcrSession = new SessionFacade<>(mock(Session.class), false);
        final Workspace jcrFacade =
                WorkspaceFacade.findBestWrapper(mock(Workspace.class), jcrSession);
        assertTrue("should be JcrWorkspaceFacade", jcrFacade instanceof JcrWorkspaceFacade);
        final SessionFacade<JackrabbitSession> jackSession = new SessionFacade<>(mock(JackrabbitSession.class), false);
        final Workspace jackFacade =
                WorkspaceFacade.findBestWrapper(mock(JackrabbitWorkspace.class), jackSession);
        assertTrue("should be JackrabbitWorkspaceFacade", jackFacade instanceof JackrabbitWorkspaceFacade);
        assertNull("null should return null", WorkspaceFacade.findBestWrapper(null, jcrSession));
    }

    @Test
    public void testGetSession() {
        final Workspace workspace = mock(Workspace.class);
        final SessionFacade<Session> jcrSession = new SessionFacade<>(mock(Session.class), false);
        final WorkspaceFacade<Workspace, Session> facade = new JcrWorkspaceFacade<>(workspace, jcrSession);
        assertSame("should be same session", jcrSession, facade.getSession());
    }

    @Test
    public void testGetName() {
        final Workspace workspace = mock(Workspace.class);
        final String name = "i am a mock";
        when(workspace.getName()).thenReturn(name);
        final WorkspaceFacade<Workspace, Session> facade = getFacade(workspace);
        assertSame("should be same", name, facade.getName());
    }

    @Test
    public void testGetAccessibleWorkspaceNames() throws Exception {
        final Workspace workspace = mock(Workspace.class);
        final String[] names = new String[]{"workspace1", "workspace2"};
        when(workspace.getAccessibleWorkspaceNames()).thenReturn(names);
        final WorkspaceFacade<Workspace, Session> facade = getFacade(workspace);
        assertSame("should be same", names, facade.getAccessibleWorkspaceNames());
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Workspace.class, this::getFacade)
                .testFacadeGetter(LockManager.class, LockManagerFacade.class, Workspace::getLockManager)
                .testFacadeGetter(QueryManager.class, QueryManagerFacade.class, Workspace::getQueryManager)
                .testFacadeGetter(NamespaceRegistry.class, NamespaceRegistryFacade.class, Workspace::getNamespaceRegistry)
                .testFacadeGetter(NodeTypeManager.class, NodeTypeManagerFacade.class, Workspace::getNodeTypeManager)
                .testFacadeGetter(ObservationManager.class, ObservationManagerFacade.class, Workspace::getObservationManager)
                .testFacadeGetter(VersionManager.class, VersionManagerFacade.class, Workspace::getVersionManager);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testCopy2() throws RepositoryException {
        getFacade(mock(Workspace.class)).copy("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testCopy3() throws RepositoryException {
        getFacade(mock(Workspace.class)).copy("", "", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testClone() throws RepositoryException {
        getFacade(mock(Workspace.class)).clone("", "", "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testMove() throws RepositoryException {
        getFacade(mock(Workspace.class)).move("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRestore() throws RepositoryException {
        getFacade(mock(Workspace.class)).restore(new Version[0], true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testGetImportContentHandler() throws RepositoryException {
        getFacade(mock(Workspace.class)).getImportContentHandler("", -1);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testImportXml() throws RepositoryException {
        getFacade(mock(Workspace.class)).importXML("", null, -1);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testCreateWorkspace() throws RepositoryException {
        getFacade(mock(Workspace.class)).createWorkspace("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testCreateWorkspace2() throws RepositoryException {
        getFacade(mock(Workspace.class)).createWorkspace("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testDeleteWorkspace() throws RepositoryException {
        getFacade(mock(Workspace.class)).deleteWorkspace("");
    }
}