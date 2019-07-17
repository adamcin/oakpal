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
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps {@link Node} to prevent writes.
 */
public class NodeFacade<N extends Node, S extends Session> extends ItemFacade<N, S> implements Node {

    public NodeFacade(final @NotNull N delegate, final @NotNull SessionFacade<S> session) {
        super(delegate, session);
    }

    public static @NotNull Node wrap(final @NotNull Node primaryItem, final @NotNull SessionFacade<?> session) {
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

    public static @Nullable Node unwrap(final @Nullable Node node) {
        if (node instanceof NodeFacade) {
            return (Node) ((NodeFacade) node).delegate;
        } else {
            return node;
        }
    }

    @Override
    public final Node getNode(String relPath) throws RepositoryException {
        Node internalNode = delegate.getNode(relPath);
        return wrap(internalNode, session);
    }

    @Override
    public final NodeIterator getNodes() throws RepositoryException {
        NodeIterator internal = delegate.getNodes();
        return new NodeIteratorFacade<>(internal, session);
    }

    @Override
    public final NodeIterator getNodes(String namePattern) throws RepositoryException {
        NodeIterator internal = delegate.getNodes(namePattern);
        return new NodeIteratorFacade<>(internal, session);
    }

    @Override
    public final NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        NodeIterator internal = delegate.getNodes(nameGlobs);
        return new NodeIteratorFacade<>(internal, session);
    }

    @Override
    public final Property getProperty(String relPath) throws RepositoryException {
        Property internal = delegate.getProperty(relPath);
        return new PropertyFacade<>(internal, session);
    }

    @Override
    public final PropertyIterator getProperties() throws RepositoryException {
        PropertyIterator internal = delegate.getProperties();
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final PropertyIterator getProperties(String namePattern) throws RepositoryException {
        PropertyIterator internal = delegate.getProperties(namePattern);
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        PropertyIterator internal = delegate.getProperties(nameGlobs);
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final Item getPrimaryItem() throws RepositoryException {
        return ensureBestWrapper(delegate.getPrimaryItem(), session);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final String getUUID() throws RepositoryException {
        return delegate.getUUID();
    }

    @Override
    public final String getIdentifier() throws RepositoryException {
        return delegate.getIdentifier();
    }

    @Override
    public final int getIndex() throws RepositoryException {
        return delegate.getIndex();
    }

    @Override
    public final PropertyIterator getReferences() throws RepositoryException {
        PropertyIterator internal = delegate.getReferences();
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final PropertyIterator getReferences(String name) throws RepositoryException {
        PropertyIterator internal = delegate.getReferences(name);
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final PropertyIterator getWeakReferences() throws RepositoryException {
        PropertyIterator internal = delegate.getWeakReferences();
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final PropertyIterator getWeakReferences(String name) throws RepositoryException {
        PropertyIterator internal = delegate.getWeakReferences(name);
        return new PropertyIteratorFacade<>(internal, session);
    }

    @Override
    public final boolean hasNode(String relPath) throws RepositoryException {
        return delegate.hasNode(relPath);
    }

    @Override
    public final boolean hasProperty(String relPath) throws RepositoryException {
        return delegate.hasProperty(relPath);
    }

    @Override
    public final boolean hasNodes() throws RepositoryException {
        return delegate.hasNodes();
    }

    @Override
    public final boolean hasProperties() throws RepositoryException {
        return delegate.hasProperties();
    }

    @Override
    public final NodeType getPrimaryNodeType() throws RepositoryException {
        return delegate.getPrimaryNodeType();
    }

    @Override
    public final NodeType[] getMixinNodeTypes() throws RepositoryException {
        return delegate.getMixinNodeTypes();
    }

    @Override
    public final boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return delegate.isNodeType(nodeTypeName);
    }

    @Override
    public final boolean canAddMixin(String mixinName) throws RepositoryException {
        return delegate.canAddMixin(mixinName);
    }

    @Override
    public final NodeDefinition getDefinition() throws RepositoryException {
        return delegate.getDefinition();
    }

    @Override
    public final String getCorrespondingNodePath(String workspaceName) throws RepositoryException {
        return delegate.getCorrespondingNodePath(workspaceName);
    }

    @Override
    public final NodeIterator getSharedSet() throws RepositoryException {
        NodeIterator internal = delegate.getSharedSet();
        return new NodeIteratorFacade<>(internal, session);
    }

    @Override
    public final boolean isCheckedOut() throws RepositoryException {
        return delegate.isCheckedOut();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final VersionHistory getVersionHistory() throws RepositoryException {
        return new VersionHistoryFacade<>(delegate.getVersionHistory(), session);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Version getBaseVersion() throws RepositoryException {
        return new VersionFacade<>(delegate.getBaseVersion(), session);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final Lock getLock() throws RepositoryException {
        Lock internal = delegate.getLock();
        return new LockFacade<>(internal, session);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final boolean holdsLock() throws RepositoryException {
        return delegate.holdsLock();
    }

    @Override
    public final boolean isLocked() throws RepositoryException {
        return delegate.isLocked();
    }

    @Override
    public final String[] getAllowedLifecycleTransistions() throws RepositoryException {
        return delegate.getAllowedLifecycleTransistions();
    }

    //************************
    // force read-only methods
    //************************

    @Override
    public final Node addNode(String relPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Node addNode(String relPath, String primaryNodeTypeName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void orderBefore(String srcChildRelPath, String destChildRelPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Value value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Value value, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Value[] values) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Value[] values, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, String[] values) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, String[] values, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, String value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, String value, int type) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, InputStream value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Binary value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, boolean value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, double value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, BigDecimal value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, long value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Calendar value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Property setProperty(String name, Node value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void setPrimaryType(String nodeTypeName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void addMixin(String mixinName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void removeMixin(String mixinName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Version checkin() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void checkout() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void doneMerge(Version version) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void cancelMerge(Version version) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void update(String srcWorkspace) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final NodeIterator merge(String srcWorkspace, boolean bestEffort) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void removeSharedSet() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void removeShare() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void restore(String versionName, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void restore(Version version, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void restore(Version version, String relPath, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void restoreByLabel(String versionLabel, boolean removeExisting) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final Lock lock(boolean isDeep, boolean isSessionScoped) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void unlock() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void followLifecycleTransition(String transition) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
