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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.version.VersionFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionHistoryFacade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ItemFacadeTest {

    @Mock
    Session session;

    JcrSessionFacade sessionFacade;

    @Before
    public void setUp() throws Exception {
        sessionFacade = new JcrSessionFacade(session, true);
    }

    ItemFacade<Item, Session> getFacade(final Item mockItem) {
        return new ItemFacade<>(mockItem, sessionFacade);
    }

    @Test
    public void testEnsureBestWrapper() throws Exception {
        Item alreadyFacade = getFacade(mock(Node.class));
        assertSame("same if already facade", alreadyFacade, ItemFacade.ensureBestWrapper(alreadyFacade, sessionFacade));
        Item versionHistory = mock(VersionHistory.class);
        assertTrue("is VersionHistory", ItemFacade.ensureBestWrapper(versionHistory, sessionFacade) instanceof VersionHistoryFacade);
        Item version = mock(Version.class);
        assertTrue("is Version", ItemFacade.ensureBestWrapper(version, sessionFacade) instanceof VersionFacade);
        Item justNode = mock(Node.class);
        assertTrue("is Node", ItemFacade.ensureBestWrapper(justNode, sessionFacade) instanceof NodeFacade);
        Item justProperty = mock(Property.class);
        assertTrue("is Property", ItemFacade.ensureBestWrapper(justProperty, sessionFacade) instanceof PropertyFacade);
        Item justItem = mock(Item.class);
        assertTrue("is Item", ItemFacade.ensureBestWrapper(justItem, sessionFacade) instanceof ItemFacade);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testUnwrap() throws Exception {
        assertNull("unwrap null to null", ItemFacade.unwrap(null));
        Item justNodeDelegate = mock(Node.class);
        assertSame("same Item delegate by itself", justNodeDelegate, ItemFacade.unwrap(justNodeDelegate));
        Item justNodeFacade = getFacade(justNodeDelegate);
        assertSame("same Item delegate", justNodeDelegate, ItemFacade.unwrap(justNodeFacade));
        Property propertyDelegate = mock(Property.class);
        Item propertyFacade = getFacade(propertyDelegate);
        assertSame("same Property delegate", propertyDelegate, ItemFacade.unwrap(propertyFacade));
        Node versionDelegate = mock(Version.class);
        Item versionFacade = getFacade(versionDelegate);
        assertSame("same Version delegate", versionDelegate, ItemFacade.unwrap(versionFacade));
        Node versionHistoryDelegate = mock(VersionHistory.class);
        Item versionHistoryFacade = getFacade(versionHistoryDelegate);
        assertSame("same VersionHistory delegate", versionHistoryDelegate, ItemFacade.unwrap(versionHistoryFacade));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Item.class, this::getFacade)
                .testFacadeGetter(Item.class, ItemFacade.class, item -> item.getAncestor(1))
                .testFacadeGetter(Node.class, NodeFacade.class, Item::getParent)
                .testFacadeGetter(Version.class, VersionFacade.class, Item::getParent)
                .testFacadeGetter(VersionHistory.class, VersionHistoryFacade.class, Item::getParent);
    }

    @Test
    public void testGetPath() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        final String value = "/correct/path";
        when(delegate.getPath()).thenReturn(value);
        assertSame("same value", value, facade.getPath());
    }

    @Test
    public void testGetName() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        final String value = "path";
        when(delegate.getName()).thenReturn(value);
        assertSame("same value", value, facade.getName());

    }

    @Test
    public void testGetDepth() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        final int value = 4;
        when(delegate.getDepth()).thenReturn(value);
        assertEquals("same value", value, facade.getDepth());
    }

    @Test
    public void testGetSession() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        assertSame("same value", sessionFacade, facade.getSession());
    }

    @Test
    public void testIsNode() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        when(delegate.isNode()).thenReturn(true);
        assertTrue("is true", facade.isNode());
    }

    @Test
    public void testIsNew() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        when(delegate.isNew()).thenReturn(true);
        assertTrue("is true", facade.isNew());
    }

    @Test
    public void testIsModified() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        when(delegate.isModified()).thenReturn(true);
        assertTrue("is true", facade.isModified());
    }

    @Test
    public void testIsSame() throws Exception {
        Item delegate = mock(Item.class);
        Item otherItem = mock(Item.class);
        when(delegate.isSame(delegate)).thenReturn(true);
        when(delegate.isSame(otherItem)).thenReturn(false);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        ItemFacade<Item, Session> self = getFacade(delegate);
        ItemFacade<Item, Session> otherFacade = getFacade(otherItem);
        assertTrue("same facade", facade.isSame(facade));
        assertTrue("same as self", facade.isSame(self));
        assertFalse("not same as other", facade.isSame(otherFacade));
    }

    @Test
    public void testAccept() throws Exception {
        Node delegate = mock(Node.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        CompletableFuture<String> pathLatch = new CompletableFuture<>();
        final String expectedPath = "/correct/path";
        when(delegate.getPath()).thenReturn(expectedPath);
        doAnswer(invoked -> {
            ((ItemVisitor) invoked.getArgument(0)).visit(delegate);
            return true;
        }).when(delegate).accept(any(ItemVisitor.class));
        facade.accept(new ItemVisitor() {
            @Override
            public void visit(final Property property) throws RepositoryException {
            }

            @Override
            public void visit(final Node node) throws RepositoryException {
                assertTrue("is facade", node instanceof NodeFacade);
                assertSame("is same as delegate", delegate, NodeFacade.unwrap(node));
                pathLatch.complete(node.getPath());
            }
        });
        assertEquals("expect path after visitor", expectedPath, pathLatch.getNow(""));
    }

    @Test
    public void testRefresh() throws Exception {
        CompletableFuture<Boolean> latchDoKeepChanges = new CompletableFuture<>();
        CompletableFuture<Boolean> latchDontKeepChanges = new CompletableFuture<>();
        Session delegateSession = mock(Session.class);
        doAnswer(invoked -> latchDoKeepChanges.complete(invoked.getArgument(0)))
                .when(delegateSession).refresh(true);
        doAnswer(invoked -> latchDontKeepChanges.complete(invoked.getArgument(0)))
                .when(delegateSession).refresh(false);
        JcrSessionFacade sessionFacade = new JcrSessionFacade(delegateSession, true);
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = new ItemFacade<>(delegate, sessionFacade);
        facade.refresh(true);
        facade.refresh(false);
        assertTrue("keepChanges true", latchDoKeepChanges.getNow(false));
        assertFalse("keepChanges false", latchDontKeepChanges.getNow(true));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testSave() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        facade.save();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRemove() throws Exception {
        Item delegate = mock(Item.class);
        ItemFacade<Item, Session> facade = getFacade(delegate);
        facade.remove();
    }
}