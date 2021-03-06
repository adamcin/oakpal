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

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Base facade for {@link Item} and its subtypes.
 */
public class ItemFacade<J extends Item, S extends Session> implements Item {

    protected final @NotNull J delegate;
    protected final @NotNull SessionFacade<S> session;

    @SuppressWarnings("WeakerAccess")
    public ItemFacade(final @NotNull J delegate, final @NotNull SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @SuppressWarnings("WeakerAccess")
    public static @NotNull Item
    ensureBestWrapper(final @NotNull Item primaryItem, final @NotNull SessionFacade<?> session) {
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

    static @Nullable Item unwrap(final @Nullable Item item) {
        if (item instanceof ItemFacade) {
            return ((ItemFacade) item).delegate;
        } else {
            return item;
        }
    }

    @Override
    public final String getPath() throws RepositoryException {
        return delegate.getPath();
    }

    @Override
    public final String getName() throws RepositoryException {
        return delegate.getName();
    }

    @Override
    public final Item getAncestor(int depth) throws RepositoryException {
        return ItemFacade.ensureBestWrapper(delegate.getAncestor(depth), session);
    }

    @Override
    public final Node getParent() throws RepositoryException {
        return NodeFacade.wrap(delegate.getParent(), session);
    }

    @Override
    public final int getDepth() throws RepositoryException {
        return delegate.getDepth();
    }

    @Override
    public final Session getSession() {
        return session;
    }

    @Override
    public final boolean isNode() {
        return delegate.isNode();
    }

    @Override
    public final boolean isNew() {
        return delegate.isNew();
    }

    @Override
    public final boolean isModified() {
        return delegate.isModified();
    }

    @Override
    public final boolean isSame(Item otherItem) throws RepositoryException {
        return delegate.isSame(unwrap(otherItem));
    }

    @Override
    public final void accept(ItemVisitor visitor) throws RepositoryException {
        delegate.accept(new ItemWrappingVisitor<>(visitor, session));
    }

    @Override
    public final void refresh(boolean keepChanges) throws RepositoryException {
        session.refresh(keepChanges);
    }

    @Override
    public final void save() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public final void remove() throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}

