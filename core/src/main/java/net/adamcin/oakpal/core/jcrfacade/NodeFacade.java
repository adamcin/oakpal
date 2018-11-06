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
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.lock.LockFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionHistoryFacade;

/**
 * Wraps {@link Node} to prevent writes.
 */
public class NodeFacade<N extends Node> extends ItemFacade<N> implements Node {

    public NodeFacade(N delegate, SessionFacade session) {
        super(delegate, session);
    }

    @Override
    public Node addNode(String relPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Node addNode(String relPath, String primaryNodeTypeName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Value value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Value value, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Value[] values) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Value[] values, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, String[] values) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, String[] values, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, String value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, String value, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, InputStream value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Binary value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, boolean value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, double value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, BigDecimal value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, long value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Calendar value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Property setProperty(String name, Node value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Node getNode(String relPath) throws RepositoryException {
        Node internalNode = delegate.getNode(relPath);
        return new NodeFacade<>(internalNode, session);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        NodeIterator internal = delegate.getNodes();
        return new NodeIteratorFacade(internal, session);
    }

    @Override
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        NodeIterator internal = delegate.getNodes();
        return new NodeIteratorFacade(internal, session);
    }

    @Override
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        NodeIterator internal = delegate.getNodes();
        return new NodeIteratorFacade(internal, session);
    }

    @Override
    public Property getProperty(String relPath) throws RepositoryException {
        Property internal = delegate.getProperty(relPath);
        return new PropertyFacade<>(internal, session);
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        PropertyIterator internal = delegate.getProperties();
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        PropertyIterator internal = delegate.getProperties(namePattern);
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        PropertyIterator internal = delegate.getProperties(nameGlobs);
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public Item getPrimaryItem() throws RepositoryException {
        return ensureBestWrapper(delegate.getPrimaryItem(), session);
    }

    @Override
    public String getUUID() throws RepositoryException {
        return delegate.getUUID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return delegate.getIdentifier();
    }

    @Override
    public int getIndex() throws RepositoryException {
        return delegate.getIndex();
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        PropertyIterator internal = delegate.getReferences();
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public PropertyIterator getReferences(String name) throws RepositoryException {
        PropertyIterator internal = delegate.getReferences(name);
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        PropertyIterator internal = delegate.getWeakReferences();
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        PropertyIterator internal = delegate.getWeakReferences(name);
        return new PropertyIteratorFacade(internal, session);
    }

    @Override
    public boolean hasNode(String relPath) throws RepositoryException {
        return delegate.hasNode(relPath);
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return delegate.hasProperty(relPath);
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return delegate.hasNodes();
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return delegate.hasProperties();
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return delegate.getPrimaryNodeType();
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return delegate.getMixinNodeTypes();
    }

    @Override
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return delegate.isNodeType(nodeTypeName);
    }

    @Override
    public void setPrimaryType(String nodeTypeName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void addMixin(String mixinName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void removeMixin(String mixinName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        return delegate.canAddMixin(mixinName);
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        return delegate.getDefinition();
    }

    @Override
    public Version checkin() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void checkout() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void doneMerge(Version version) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void cancelMerge(Version version) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void update(String srcWorkspace) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return delegate.getCorrespondingNodePath(workspaceName);
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        NodeIterator internal = delegate.getSharedSet();
        return new NodeIteratorFacade(internal, session);
    }

    @Override
    public void removeSharedSet() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void removeShare() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        return delegate.isCheckedOut();
    }

    @Override
    public void restore(String versionName, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void restore(Version version, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void restore(Version version, String relPath, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public VersionHistory getVersionHistory() throws RepositoryException {
        return new VersionHistoryFacade<>(delegate.getVersionHistory(), session);
    }

    @Override
    public Version getBaseVersion() throws RepositoryException {
        return new VersionFacade<>(delegate.getBaseVersion(), session);
    }

    @Override
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Lock getLock() throws RepositoryException {
        Lock internal = delegate.getLock();
        return new LockFacade(internal, session);
    }

    @Override
    public void unlock() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean holdsLock() throws RepositoryException {
        return delegate.holdsLock();
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        return delegate.isLocked();
    }

    @Override
    public void followLifecycleTransition(String transition) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public String[] getAllowedLifecycleTransistions() throws RepositoryException {
        return delegate.getAllowedLifecycleTransistions();
    }

    public static Node wrap(Node primaryItem, SessionFacade session) {
        if (primaryItem instanceof NodeFacade) {
            return primaryItem;
        } else if (primaryItem instanceof VersionHistory) {
            return new VersionHistoryFacade<>((VersionHistory) primaryItem, session);
        } else if (primaryItem instanceof Version) {
            return new VersionFacade<>((Version) primaryItem, session);
        } else {
            return new NodeFacade<>(primaryItem, session);
        }
    }

    public static Node unwrap(Node node) {
        if (node instanceof NodeFacade) {
            return (Node) ((NodeFacade) node).delegate;
        } else {
            return node;
        }
    }
}
