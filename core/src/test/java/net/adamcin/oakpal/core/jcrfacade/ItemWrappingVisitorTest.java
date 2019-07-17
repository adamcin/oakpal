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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ItemWrappingVisitorTest {

    ItemWrappingVisitor<Session> getFacade(final @NotNull ItemVisitor visitor) {
        return new ItemWrappingVisitor<>(visitor, new JcrSessionFacade(mock(Session.class), true));
    }

    class FacadeTestingItemVisitor implements ItemVisitor {
        final String path;

        FacadeTestingItemVisitor(final String path) {
            this.path = path;
        }

        @Override
        public void visit(final Property property) throws RepositoryException {
            assertTrue("is facade", property instanceof PropertyFacade);
            assertEquals("path is same", path, property.getPath());
        }

        @Override
        public void visit(final Node node) throws RepositoryException {
            assertTrue("is facade", node instanceof NodeFacade);
            assertEquals("path is same", path, node.getPath());
        }
    }

    @Test
    public void testVisitProperty() throws Exception {
        final String path = "/correct/path";
        ItemVisitor visitor = new FacadeTestingItemVisitor(path);
        ItemWrappingVisitor<Session> facade = getFacade(visitor);
        Property delegate = mock(Property.class);
        when(delegate.getPath()).thenReturn(path);
        facade.visit(delegate);
    }

    @Test
    public void testVisitNode() throws Exception {
        final String path = "/correct/path";
        ItemVisitor visitor = new FacadeTestingItemVisitor(path);
        ItemWrappingVisitor<Session> facade = getFacade(visitor);
        Node delegate = mock(Node.class);
        when(delegate.getPath()).thenReturn(path);
        facade.visit(delegate);
    }
}