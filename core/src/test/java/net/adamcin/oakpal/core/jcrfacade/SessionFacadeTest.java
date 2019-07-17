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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.jcrfacade.retention.RetentionManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.security.JackrabbitAccessControlManagerFacade;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.xml.sax.helpers.DefaultHandler;

public class SessionFacadeTest {
    /**
     * Functional interface for testing.
     */
    @FunctionalInterface
    public interface TestInspectBody<E extends Throwable> {
        void tryAccept(final Session admin, final Session facade) throws E;
    }

    <E extends Throwable> void
    inspectForTest(final @NotNull TestInspectBody<E> body)
            throws RepositoryException, E {
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            Session facade = SessionFacade.findBestWrapper(session, true);
            body.tryAccept(session, facade);
        });
    }

    SessionFacade<Session> getFacade(final @NotNull Session delegate) {
        return new JcrSessionFacade(delegate, true);
    }

    @Test
    public void testFindBestWrapper() {
        final Session session = mock(Session.class);
        Session jcrFacade = SessionFacade.findBestWrapper(session, true);
        assertFalse("find best wrapper jcr", jcrFacade instanceof JackrabbitSession);
        final Session jackSession = mock(JackrabbitSession.class);
        Session jackrabbitFacade = SessionFacade.findBestWrapper(jackSession, true);
        assertTrue("find best wrapper jackrabbit", jackrabbitFacade instanceof JackrabbitSession);
        assertNull("find best wrapper null", SessionFacade.findBestWrapper(null, true));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRootNode() throws Exception {
        final Session session = mock(Session.class);
        doThrow(new IllegalStateException("expect me")).when(session).getRootNode();
        new JcrSessionFacade(session, false).getRootNode();
    }

    @Test
    public void testGetAttributeNames() {
        final String[] names = new String[]{"test"};
        final Session session = mock(Session.class);
        when(session.getAttributeNames()).thenReturn(names);
        assertArrayEquals(names, new JcrSessionFacade(session, false).getAttributeNames());
    }

    @Test
    public void testGetAttribute() {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String attrName = "myAttr";
        final String attrValue = "value";
        when(delegate.getAttribute(attrName)).thenReturn(attrValue);
        assertNull("unknown attr return null", facade.getAttribute("some other attr"));
        assertSame("get value for known attr", attrValue, facade.getAttribute(attrName));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Session.class, session -> new JcrSessionFacade(session, true))
                .testFacadeGetter(Repository.class, RepositoryFacade.class, Session::getRepository)
                .testFacadeGetter(Workspace.class, JcrWorkspaceFacade.class, Session::getWorkspace)
                .testFacadeGetter(JackrabbitWorkspace.class, JackrabbitWorkspaceFacade.class, Session::getWorkspace)
                .testFacadeGetter(AccessControlManager.class, AccessControlManager.class, Session::getAccessControlManager)
                .testFacadeGetter(JackrabbitAccessControlManager.class, JackrabbitAccessControlManagerFacade.class, Session::getAccessControlManager)
                .testFacadeGetter(RetentionManager.class, RetentionManagerFacade.class, Session::getRetentionManager);
    }

    @Test
    public void testGetUserID() throws Exception {
        inspectForTest((admin, facade) -> {
            assertEquals("getUserID is admin", "admin", facade.getUserID());
            assertEquals("getUserID is equal", admin.getUserID(), facade.getUserID());
        });
    }

    @Test
    public void testImpersonate() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        SimpleCredentials credentials = new SimpleCredentials("foo", "bar".toCharArray());
        JackrabbitSession jackSession = mock(JackrabbitSession.class);
        when(delegate.impersonate(credentials)).thenReturn(jackSession);
        assertNull("some other credentials should impersonate null",
                facade.impersonate(new SimpleCredentials("bar", "foo".toCharArray())));
        assertTrue("jackrabbit session impersonation should work",
                facade.impersonate(credentials) instanceof JackrabbitSession);
    }

    @SuppressWarnings("deprecated")
    @Test
    public void testGetNodeByUUID() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        Node mockNode = mock(Node.class);
        final String path = "/correct";
        final String uuid = "some uuid";
        when(mockNode.getPath()).thenReturn(path);
        when(delegate.getNodeByUUID(uuid)).thenReturn(mockNode);
        assertEquals("/correct path with correct uuid", path, facade.getNodeByUUID(uuid).getPath());
    }

    @Test
    public void testGetNodeByIdentifier() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        Node mockNode = mock(Node.class);
        final String path = "/correct";
        final String uuid = "some uuid";
        when(mockNode.getPath()).thenReturn(path);
        when(delegate.getNodeByIdentifier(uuid)).thenReturn(mockNode);
        assertEquals("/correct path with correct id", path, facade.getNodeByIdentifier(uuid).getPath());
    }

    @Test
    public void testGetItem() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        Item mockNode = mock(Item.class);
        final String path = "/correct/path";
        when(mockNode.getPath()).thenReturn(path);
        when(delegate.getItem(path)).thenReturn(mockNode);
        assertEquals("/correct/path with correct id", path, facade.getItem(path).getPath());
    }

    @Test
    public void testGetNode() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        Node mockNode = mock(Node.class);
        final String path = "/correct/path";
        when(mockNode.getPath()).thenReturn(path);
        when(delegate.getNode(path)).thenReturn(mockNode);
        assertEquals("/correct/path with correct id", path, facade.getNode(path).getPath());
    }

    @Test
    public void testGetProperty() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        Property mockItem = mock(Property.class);
        final String path = "/correct/path";
        when(mockItem.getPath()).thenReturn(path);
        when(delegate.getProperty(path)).thenReturn(mockItem);
        assertEquals("/correct/path with correct id", path, facade.getProperty(path).getPath());
    }

    @Test
    public void testItemExists() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.itemExists(anyString())).thenReturn(false);
        when(delegate.itemExists(path)).thenReturn(true);
        assertFalse("not exists at empty path", facade.itemExists(""));
        assertTrue("exists at correct path", facade.itemExists(path));
    }

    @Test
    public void testNodeExists() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.nodeExists(anyString())).thenReturn(false);
        when(delegate.nodeExists(path)).thenReturn(true);
        assertFalse("not exists at empty path", facade.nodeExists(""));
        assertTrue("exists at correct path", facade.nodeExists(path));
    }

    @Test
    public void testPropertyExists() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.propertyExists(anyString())).thenReturn(false);
        when(delegate.propertyExists(path)).thenReturn(true);
        assertFalse("not exists at empty path", facade.propertyExists(""));
        assertTrue("exists at correct path", facade.propertyExists(path));
    }

    @Test
    public void testIsLive() {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        when(delegate.isLive()).thenReturn(true);
        assertTrue("should be live", facade.isLive());
    }

    @Test
    public void testHasPendingChanges() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        when(delegate.hasPendingChanges()).thenReturn(true);
        assertTrue("should have pending changes", facade.hasPendingChanges());
    }

    @Test
    public void testGetValueFactory() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        ValueFactory mockVf = mock(ValueFactory.class);
        when(delegate.getValueFactory()).thenReturn(mockVf);
        assertSame("should be same", mockVf, facade.getValueFactory());
    }

    @Test
    public void testHasPermission() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        when(delegate.hasPermission(anyString(), anyString())).thenReturn(false);
        when(delegate.hasPermission("/correct/path", Session.ACTION_READ)).thenReturn(true);
        assertFalse("should not have permission", facade.hasPermission("", ""));
        assertTrue("should have permission",
                facade.hasPermission("/correct/path", Session.ACTION_READ));
    }

    @Test
    public void testCheckPermission() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final CompletableFuture<String> latch = new CompletableFuture<>();
        doAnswer(invoke -> latch.complete("done"))
                .when(delegate).checkPermission("/correct/path", Session.ACTION_READ);
        facade.checkPermission("", "");
        assertFalse("should not be done", latch.isDone());
        facade.checkPermission("/correct/path", Session.ACTION_READ);
        assertEquals("should be done", "done", latch.getNow(""));
    }

    @Test
    public void testHasCapability() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        Object target = new Object();
        final String method = "doStuff";
        final Object[] args = new Object[]{new Object()};
        when(delegate.hasCapability(anyString(), any(), any(Object[].class))).thenReturn(false);
        when(delegate.hasCapability(method, target, args)).thenReturn(true);
        assertFalse("should not have capability", facade.hasCapability("", "", new Object[0]));
        assertTrue("should have capability",
                facade.hasCapability(method, target, args));
    }

    @Test
    public void testExportSystemViewContentHandler() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final DefaultHandler handler = new DefaultHandler();
        final String path = "/correct/path";
        final CompletableFuture<String> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete("done")).when(delegate)
                .exportSystemView(path, handler, true, true);
        assertFalse("should not be done", latch.isDone());
        facade.exportSystemView(path, handler, true, true);
        assertEquals("should be done", "done", latch.getNow(""));
    }

    @Test
    public void testExportSystemViewOutputStream() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final OutputStream handler = mock(OutputStream.class);
        final String path = "/correct/path";
        final CompletableFuture<String> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete("done")).when(delegate)
                .exportSystemView(path, handler, true, true);
        assertFalse("should not be done", latch.isDone());
        facade.exportSystemView(path, handler, true, true);
        assertEquals("should be done", "done", latch.getNow(""));
    }

    @Test
    public void testExportDocumentViewContentHandler() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final DefaultHandler handler = new DefaultHandler();
        final String path = "/correct/path";
        final CompletableFuture<String> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete("done")).when(delegate)
                .exportDocumentView(path, handler, true, true);
        assertFalse("should not be done", latch.isDone());
        facade.exportDocumentView(path, handler, true, true);
        assertEquals("should be done", "done", latch.getNow(""));
    }

    @Test
    public void testExportDocumentViewOutputStream() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final OutputStream handler = mock(OutputStream.class);
        final String path = "/correct/path";
        final CompletableFuture<String> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete("done")).when(delegate)
                .exportDocumentView(path, handler, true, true);
        assertFalse("should not be done", latch.isDone());
        facade.exportDocumentView(path, handler, true, true);
        assertEquals("should be done", "done", latch.getNow(""));
    }

    @Test
    public void testGetNamespacePrefixes() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String[] prefixes = new String[]{"jcr", "nt", "oak"};
        when(delegate.getNamespacePrefixes()).thenReturn(prefixes);
        assertSame("should be same", prefixes, facade.getNamespacePrefixes());
    }

    @Test
    public void testGetNamespaceURI() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String prefix = NamespaceRegistry.PREFIX_JCR;
        final String uri = NamespaceRegistry.NAMESPACE_JCR;
        when(delegate.getNamespaceURI(anyString())).thenReturn(null);
        when(delegate.getNamespaceURI(prefix)).thenReturn(uri);
        assertNull("should be null", facade.getNamespaceURI(""));
        assertSame("should be same", uri, facade.getNamespaceURI(prefix));
    }

    @Test
    public void testGetNamespacePrefix() throws Exception {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String prefix = NamespaceRegistry.PREFIX_JCR;
        final String uri = NamespaceRegistry.NAMESPACE_JCR;
        when(delegate.getNamespacePrefix(anyString())).thenReturn(null);
        when(delegate.getNamespacePrefix(uri)).thenReturn(prefix);
        assertNull("should be null", facade.getNamespacePrefix(""));
        assertSame("should be same", prefix, facade.getNamespacePrefix(uri));
    }

    @Test
    public void testGetLockTokens() {
        Session delegate = mock(Session.class);
        SessionFacade<Session> facade = getFacade(delegate);
        final String[] tokens = new String[]{"some"};
        when(delegate.getLockTokens()).thenReturn(tokens);
        assertSame("should be same", tokens, facade.getLockTokens());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testMove() throws Exception {
        inspectForTest((admin, facade) -> {
            facade.move("", "");
        });
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRemoveItem() throws Exception {
        inspectForTest((admin, facade) -> {
            facade.removeItem("");
        });
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testSave() throws Exception {
        inspectForTest((admin, facade) -> {
            facade.save();
        });
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testGetImportContentHandler() throws Exception {
        inspectForTest((admin, facade) -> {
            facade.getImportContentHandler("", -1);
        });
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testImportXml() throws Exception {
        inspectForTest((admin, facade) -> {
            facade.importXML("", null, -1);
        });
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testSetNamespacePrefix() throws Exception {
        inspectForTest((admin, facade) -> {
            facade.setNamespacePrefix("", "");
        });
    }

    @Test
    public void testAddLockToken() {
        Session session = mock(Session.class);
        doThrow(new IllegalStateException("should not be thrown at all")).when(session).addLockToken(anyString());
        JcrSessionFacade facade = new JcrSessionFacade(session, true);
        facade.addLockToken("");
    }

    @Test
    public void testRemoveLockToken() {
        Session session = mock(Session.class);
        doThrow(new IllegalStateException("should not be thrown at all")).when(session).removeLockToken(anyString());
        JcrSessionFacade facade = new JcrSessionFacade(session, true);
        facade.removeLockToken("");
    }

    @Test
    public void testRefresh() throws RepositoryException {
        Session session = mock(Session.class);
        doThrow(new RepositoryException("should not be thrown when protected")).when(session).refresh(anyBoolean());
        JcrSessionFacade facade = new JcrSessionFacade(session, false);
        facade.refresh(true);
        facade.refresh(false);
    }

    @Test(expected = RepositoryException.class)
    public void testRefreshThrows() throws RepositoryException {
        Session session = mock(Session.class);
        doThrow(new RepositoryException("should not be thrown when protected")).when(session).refresh(anyBoolean());
        JcrSessionFacade facade = new JcrSessionFacade(session, true);
        facade.refresh(true);
    }

    @Test
    public void testLogout() {
        Session session = mock(Session.class);
        doThrow(new IllegalStateException("should not be thrown when protected")).when(session).logout();
        JcrSessionFacade facade = new JcrSessionFacade(session, false);
        facade.logout();
    }

    @Test(expected = IllegalStateException.class)
    public void testLogoutThrows() {
        Session session = mock(Session.class);
        doThrow(new IllegalStateException("should not be thrown when protected")).when(session).logout();
        JcrSessionFacade facade = new JcrSessionFacade(session, true);
        facade.logout();
    }


}
