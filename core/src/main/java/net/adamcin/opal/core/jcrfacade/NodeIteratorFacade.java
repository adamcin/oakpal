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

package net.adamcin.opal.core.jcrfacade;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Wraps {@link NodeIterator} to return {@link NodeFacade}-wrapped nodes.
 */
public class NodeIteratorFacade extends RangeIteratorFacade<NodeIterator> implements NodeIterator {

    private final SessionFacade session;

    public NodeIteratorFacade(NodeIterator delegate, SessionFacade session) {
        super(delegate);
        this.session = session;
    }

    @Override
    public Node nextNode() {
        Node internalNode = delegate.nextNode();
        return new NodeFacade<>(internalNode, session);
    }

    @Override
    public Object next() {
        return nextNode();
    }
}
