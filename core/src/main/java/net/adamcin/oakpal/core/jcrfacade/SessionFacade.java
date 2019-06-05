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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.retention.RetentionManagerFacade;
import net.adamcin.oakpal.core.jcrfacade.security.AccessControlManagerFacade;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Base class for wrapping a {@link Session} to guards against writes by listeners.
 *
 * @param <S> type parameter for session
 */
public abstract class SessionFacade<S extends Session> implements Session {

    protected final S delegate;
    private final Workspace workspace;
    private final RepositoryFacade repository;
    private boolean notProtected;

    protected SessionFacade(S delegate, boolean notProtected) {
        this.delegate = delegate;
        this.workspace = WorkspaceFacade.findBestWrapper(delegate.getWorkspace(), this);
        this.repository = new RepositoryFacade(delegate.getRepository());
        this.notProtected = notProtected;
    }

    public static Session findBestWrapper(final Session session, final boolean notProtected) {
        if (session instanceof JackrabbitSession) {
            return new JackrabbitSessionFacade((JackrabbitSession) session, notProtected);
        } else if (session != null) {
            return new JcrSessionFacade(session, notProtected);
        } else {
            return null;
        }
    }

    @SuppressWarnings("uncheckVoid")
    public static <T extends Session> SessionFacade<T> wrap(final T session, final Class<T> sessionType,
                                                            final boolean notProtected) {
        if (sessionType.isAssignableFrom(JackrabbitSessionFacade.class)) {
            return (SessionFacade<T>) new JackrabbitSessionFacade((JackrabbitSession) session, notProtected);
        } else if (sessionType.isAssignableFrom(JcrSessionFacade.class)) {
            return (SessionFacade<T>) new JcrSessionFacade(session, notProtected);
        } else {
            throw new IllegalArgumentException("Invalid Session type: " + sessionType.getName());
        }
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public String getUserID() {
        return delegate.getUserID();
    }

    @Override
    public String[] getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public Object getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public Node getRootNode() throws RepositoryException {
        return NodeFacade.wrap(delegate.getRootNode(), this);
    }

    @Override
    public Session impersonate(Credentials credentials) throws RepositoryException {
        Session impersonateDelegate = delegate.impersonate(credentials);
        return SessionFacade.findBestWrapper(impersonateDelegate, true);
    }

    @Override
    public Node getNodeByUUID(String uuid) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNodeByUUID(uuid), this);
    }

    @Override
    public Node getNodeByIdentifier(String id) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNodeByIdentifier(id), this);
    }

    @Override
    public Item getItem(String absPath) throws RepositoryException {
        return ItemFacade.ensureBestWrapper(delegate.getItem(absPath), this);
    }

    @Override
    public Node getNode(String absPath) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNode(absPath), this);
    }

    @Override
    public Property getProperty(String absPath) throws RepositoryException {
        Property internal = delegate.getProperty(absPath);
        return new PropertyFacade<>(internal, this);
    }

    @Override
    public boolean itemExists(String absPath) throws RepositoryException {
        return delegate.itemExists(absPath);
    }

    @Override
    public boolean nodeExists(String absPath) throws RepositoryException {
        return delegate.nodeExists(absPath);
    }

    @Override
    public boolean propertyExists(String absPath) throws RepositoryException {
        return delegate.propertyExists(absPath);
    }

    @Override
    public void move(String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void removeItem(String absPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void save() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void refresh(boolean keepChanges) throws RepositoryException {
        if (notProtected) {
            delegate.refresh(keepChanges);
        }
    }

    @Override
    public boolean hasPendingChanges() throws RepositoryException {
        return delegate.hasPendingChanges();
    }

    @Override
    public ValueFactory getValueFactory() throws RepositoryException {
        return delegate.getValueFactory();
    }

    @Override
    public boolean hasPermission(String absPath, String actions) throws RepositoryException {
        return delegate.hasPermission(absPath, actions);
    }

    @Override
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        delegate.checkPermission(absPath, actions);
    }

    @Override
    public boolean hasCapability(String methodName, Object target, Object[] arguments) throws RepositoryException {
        return delegate.hasCapability(methodName, target, arguments);
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
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
            throws SAXException, RepositoryException {
        delegate.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, RepositoryException {
        delegate.exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
            throws SAXException, RepositoryException {
        delegate.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);

    }

    @Override
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, RepositoryException {
        delegate.exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    public void setNamespacePrefix(String prefix, String uri) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        return delegate.getNamespacePrefixes();
    }

    @Override
    public String getNamespaceURI(String prefix) throws RepositoryException {
        return delegate.getNamespaceURI(prefix);
    }

    @Override
    public String getNamespacePrefix(String uri) throws RepositoryException {
        return delegate.getNamespacePrefix(uri);
    }

    @Override
    public void logout() {
        if (notProtected) {
            delegate.logout();
        }
    }

    @Override
    public boolean isLive() {
        return delegate.isLive();
    }

    @Override
    public void addLockToken(String lt) {
        // do nothing, since this signature doesn't throw RepositoryException
    }

    @Override
    public String[] getLockTokens() {
        return delegate.getLockTokens();
    }

    @Override
    public void removeLockToken(String lt) {
        // do nothing, since this signature doesn't throw RepositoryException
    }

    @Override
    public AccessControlManager getAccessControlManager() throws RepositoryException {
        AccessControlManager internal = delegate.getAccessControlManager();
        if (internal instanceof JackrabbitAccessControlManager) {
            return new AccessControlManagerFacade((JackrabbitAccessControlManager) internal);
        }
        return null;
    }

    @Override
    public RetentionManager getRetentionManager() throws RepositoryException {
        RetentionManager internal = delegate.getRetentionManager();
        return new RetentionManagerFacade(internal);
    }
}
