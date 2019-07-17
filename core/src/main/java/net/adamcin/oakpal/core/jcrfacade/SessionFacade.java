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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Base class for wrapping a {@link Session} to guard against writes by listeners.
 *
 * @param <S> type parameter for session
 */
public class SessionFacade<S extends Session> implements Session {

    protected final @NotNull S delegate;
    private final boolean notProtected;

    public SessionFacade(final @NotNull S delegate, final boolean notProtected) {
        this.delegate = delegate;
        this.notProtected = notProtected;
    }

    public static @Nullable Session findBestWrapper(final @Nullable Session session, final boolean notProtected) {
        if (session instanceof JackrabbitSession) {
            return new JackrabbitSessionFacade((JackrabbitSession) session, notProtected);
        } else if (session != null) {
            return new JcrSessionFacade(session, notProtected);
        } else {
            return null;
        }
    }

    @Override
    public final Repository getRepository() {
        Repository internal = delegate.getRepository();
        return new RepositoryFacade(internal);
    }

    @Override
    public final AccessControlManager getAccessControlManager() throws RepositoryException {
        AccessControlManager internal = delegate.getAccessControlManager();
        return AccessControlManagerFacade.findBestWrapper(internal);
    }

    @Override
    public final RetentionManager getRetentionManager() throws RepositoryException {
        RetentionManager internal = delegate.getRetentionManager();
        return new RetentionManagerFacade(internal);
    }

    @Override
    public final Workspace getWorkspace() {
        Workspace internal = delegate.getWorkspace();
        return WorkspaceFacade.findBestWrapper(internal, this);
    }

    @Override
    public final String getUserID() {
        return delegate.getUserID();
    }

    @Override
    public final String[] getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public final Object getAttribute(final String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public final Node getRootNode() throws RepositoryException {
        return NodeFacade.wrap(delegate.getRootNode(), this);
    }

    @Override
    public final Session impersonate(Credentials credentials) throws RepositoryException {
        Session impersonateDelegate = delegate.impersonate(credentials);
        return SessionFacade.findBestWrapper(impersonateDelegate, true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Node getNodeByUUID(String uuid) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNodeByUUID(uuid), this);
    }

    @Override
    public final Node getNodeByIdentifier(String id) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNodeByIdentifier(id), this);
    }

    @Override
    public final Item getItem(String absPath) throws RepositoryException {
        return ItemFacade.ensureBestWrapper(delegate.getItem(absPath), this);
    }

    @Override
    public final Node getNode(String absPath) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNode(absPath), this);
    }

    @Override
    public final Property getProperty(String absPath) throws RepositoryException {
        Property internal = delegate.getProperty(absPath);
        return new PropertyFacade<>(internal, this);
    }

    @Override
    public final boolean itemExists(String absPath) throws RepositoryException {
        return delegate.itemExists(absPath);
    }

    @Override
    public final boolean nodeExists(String absPath) throws RepositoryException {
        return delegate.nodeExists(absPath);
    }

    @Override
    public final boolean propertyExists(String absPath) throws RepositoryException {
        return delegate.propertyExists(absPath);
    }

    @Override
    public final void refresh(boolean keepChanges) throws RepositoryException {
        if (notProtected) {
            delegate.refresh(keepChanges);
        }
    }

    @Override
    public final void logout() {
        if (notProtected) {
            delegate.logout();
        }
    }

    @Override
    public final boolean isLive() {
        return delegate.isLive();
    }

    @Override
    public final boolean hasPendingChanges() throws RepositoryException {
        return delegate.hasPendingChanges();
    }

    @Override
    public final ValueFactory getValueFactory() throws RepositoryException {
        return delegate.getValueFactory();
    }

    @Override
    public final boolean hasPermission(String absPath, String actions) throws RepositoryException {
        return delegate.hasPermission(absPath, actions);
    }

    @Override
    public final void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        delegate.checkPermission(absPath, actions);
    }

    @Override
    public final boolean hasCapability(String methodName, Object target, Object[] arguments) throws RepositoryException {
        return delegate.hasCapability(methodName, target, arguments);
    }

    @Override
    public final void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
            throws SAXException, RepositoryException {
        delegate.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public final void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, RepositoryException {
        delegate.exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    public final void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
            throws SAXException, RepositoryException {
        delegate.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public final void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, RepositoryException {
        delegate.exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    public final String[] getNamespacePrefixes() throws RepositoryException {
        return delegate.getNamespacePrefixes();
    }

    @Override
    public final String getNamespaceURI(String prefix) throws RepositoryException {
        return delegate.getNamespaceURI(prefix);
    }

    @Override
    public final String getNamespacePrefix(String uri) throws RepositoryException {
        return delegate.getNamespacePrefix(uri);
    }

    @Override
    public final void addLockToken(String lt) {
        // do nothing, since this signature doesn't throw RepositoryException
    }

    @SuppressWarnings("deprecation")
    @Override
    public final String[] getLockTokens() {
        return delegate.getLockTokens();
    }

    @Override
    public final void removeLockToken(String lt) {
        // do nothing, since this signature doesn't throw RepositoryException
    }

    @Override
    public final void move(String srcAbsPath, String destAbsPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void removeItem(String absPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void save() throws RepositoryException {
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
    public final void setNamespacePrefix(String prefix, String uri) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
