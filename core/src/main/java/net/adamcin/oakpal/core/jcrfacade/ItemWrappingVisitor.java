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

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Wraps an {@link ItemVisitor} to wrap {@link Node} and {@link Property} instances.
 */
public class ItemWrappingVisitor<S extends Session> implements ItemVisitor {
    private final ItemVisitor delegate;
    private final SessionFacade<S> session;

    public ItemWrappingVisitor(ItemVisitor delegate, SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public void visit(Property property) throws RepositoryException {
        delegate.visit(new PropertyFacade<>(property, session));
    }

    @Override
    public void visit(Node node) throws RepositoryException {
        delegate.visit(new NodeFacade<>(node, session));
    }
}
