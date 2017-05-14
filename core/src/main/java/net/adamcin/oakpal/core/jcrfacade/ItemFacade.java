/*
 * Copyright 2017 Mark Adamcin
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
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.adamcin.oakpal.core.ListenerReadOnlyException;

/**
 * Base facade for {@link Item} and its subtypes.
 */
public class ItemFacade<J extends Item> implements Item {

    protected final J delegate;
    protected final SessionFacade session;

    public ItemFacade(J delegate, SessionFacade session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public String getPath() throws RepositoryException {
        return delegate.getPath();
    }

    @Override
    public String getName() throws RepositoryException {
        return delegate.getName();
    }

    @Override
    public Item getAncestor(int depth) throws RepositoryException {
        return ItemFacade.ensureBestWrapper(delegate.getAncestor(depth), session);
    }

    @Override
    public Node getParent() throws RepositoryException {
        return NodeFacade.wrap(delegate.getParent(), session);
    }

    @Override
    public int getDepth() throws RepositoryException {
        return delegate.getDepth();
    }

    @Override
    public Session getSession() throws RepositoryException {
        return session;
    }

    @Override
    public boolean isNode() {
        return delegate.isNode();
    }

    @Override
    public boolean isNew() {
        return delegate.isNew();
    }

    @Override
    public boolean isModified() {
        return delegate.isModified();
    }

    @Override
    public boolean isSame(Item otherItem) throws RepositoryException {
        return delegate.isSame(unwrap(otherItem));
    }

    @Override
    public void accept(ItemVisitor visitor) throws RepositoryException {
        delegate.accept(new ItemWrappingVisitor(visitor, session));
    }

    @Override
    public void save() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void refresh(boolean keepChanges) throws RepositoryException {
        session.refresh(keepChanges);
    }

    @Override
    public void remove() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    public static Item ensureBestWrapper(Item primaryItem, SessionFacade session) {
        if (primaryItem instanceof ItemFacade) {
            return primaryItem;
        } else if (primaryItem instanceof Node) {
            return NodeFacade.wrap((Node) primaryItem, session);
        } else if (primaryItem instanceof Property) {
            return new PropertyFacade<>((Property) primaryItem, session);
        } else {
            return new ItemFacade<>(primaryItem, session);
        }
    }

    public static Item unwrap(Item item) {
        if (item instanceof ItemFacade) {
            return ((ItemFacade) item).delegate;
        } else {
            return item;
        }
    }
}

