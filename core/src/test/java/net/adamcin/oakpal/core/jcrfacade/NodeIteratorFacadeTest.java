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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class NodeIteratorFacadeTest {

    NodeIteratorFacade<Session> getFacade(final @NotNull NodeIterator delegate) {
        return new NodeIteratorFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testNextNode() throws Exception {
        NodeIterator delegate = mock(NodeIterator.class);
        NodeIteratorFacade<Session> facade = getFacade(delegate);
        final Node value = mock(Node.class);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        when(delegate.nextNode()).thenReturn(value);
        final Node fromFacade = facade.nextNode();
        assertEquals("same path", path, fromFacade.getPath());
        assertTrue("is facade", fromFacade instanceof NodeFacade);
    }

    @Test
    public void testNext() throws Exception {
        NodeIterator delegate = mock(NodeIterator.class);
        NodeIteratorFacade<Session> facade = getFacade(delegate);
        final Node value = mock(Node.class);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        when(delegate.nextNode()).thenReturn(value);
        final Object fromFacade = facade.next();
        assertTrue("is node", fromFacade instanceof Node);
        final Node nodeFromFacade = (Node) fromFacade;
        assertEquals("same path", path, nodeFromFacade.getPath());
        assertTrue("is facade", nodeFromFacade instanceof NodeFacade);
    }
}