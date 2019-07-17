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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.lock.LockFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionFacade;
import net.adamcin.oakpal.core.jcrfacade.version.VersionHistoryFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NodeFacadeTest {

    @Mock
    Session session;

    JcrSessionFacade sessionFacade;

    @Before
    public void setUp() throws Exception {
        sessionFacade = new JcrSessionFacade(session, true);
    }

    NodeFacade<Node, Session> getFacade(final @NotNull Node mockNode) {
        return new NodeFacade<>(mockNode, sessionFacade);
    }

    @Test
    public void testWrap() {
        Node alreadyFacade = getFacade(mock(Node.class));
        assertSame("same if already facade", alreadyFacade, NodeFacade.wrap(alreadyFacade, sessionFacade));
        Node versionHistory = mock(VersionHistory.class);
        assertTrue("is VersionHistory", NodeFacade.wrap(versionHistory, sessionFacade) instanceof VersionHistoryFacade);
        Node version = mock(Version.class);
        assertTrue("is Version", NodeFacade.wrap(version, sessionFacade) instanceof VersionFacade);
        Node justNode = mock(Node.class);
        assertTrue("is Node", NodeFacade.wrap(justNode, sessionFacade) instanceof NodeFacade);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testUnwrap() {
        assertNull("unwrap null to null", NodeFacade.unwrap(null));
        Node justNodeDelegate = mock(Node.class);
        assertSame("same Node delegate", justNodeDelegate, NodeFacade.unwrap(justNodeDelegate));
        Node justNodeFacade = getFacade(justNodeDelegate);
        assertSame("same Node delegate", justNodeDelegate, NodeFacade.unwrap(justNodeFacade));
        Node versionDelegate = mock(Version.class);
        Node versionFacade = getFacade(versionDelegate);
        assertSame("same Version delegate", versionDelegate, NodeFacade.unwrap(versionFacade));
        Node versionHistoryDelegate = mock(VersionHistory.class);
        Node versionHistoryFacade = getFacade(versionHistoryDelegate);
        assertSame("same Version delegate", versionHistoryDelegate, NodeFacade.unwrap(versionHistoryFacade));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Node.class, this::getFacade)
                .testFacadeGetter(Node.class, NodeFacade.class, node -> node.getNode(""))
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, Node::getNodes)
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, node -> node.getNodes(""))
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, node -> node.getNodes(new String[0]))
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, Node::getSharedSet)
                .testFacadeGetter(Property.class, PropertyFacade.class, node -> node.getProperty(""))
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, Node::getProperties)
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, node -> node.getProperties(""))
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, node -> node.getProperties(new String[0]))
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, Node::getReferences)
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, node -> node.getReferences(""))
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, Node::getWeakReferences)
                .testFacadeGetter(PropertyIterator.class, PropertyIteratorFacade.class, node -> node.getWeakReferences(""))
                .testFacadeGetter(Item.class, ItemFacade.class, Node::getPrimaryItem)
                .testFacadeGetter(Node.class, NodeFacade.class, Node::getPrimaryItem)
                .testFacadeGetter(Property.class, PropertyFacade.class, Node::getPrimaryItem)
                .testFacadeGetter(Version.class, VersionFacade.class, Node::getPrimaryItem)
                .testFacadeGetter(VersionHistory.class, VersionHistoryFacade.class, Node::getPrimaryItem)
                .testFacadeGetter(VersionHistory.class, VersionHistoryFacade.class, Node::getVersionHistory)
                .testFacadeGetter(Version.class, VersionFacade.class, Node::getBaseVersion)
                .testFacadeGetter(Lock.class, LockFacade.class, Node::getLock);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetUUID() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String value = "uuid";
        when(delegate.getUUID()).thenReturn(value);
        assertSame("same value", value, facade.getUUID());
    }

    @Test
    public void testGetIdentifier() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String value = "uuid";
        when(delegate.getIdentifier()).thenReturn(value);
        assertSame("same value", value, facade.getIdentifier());
    }

    @Test
    public void testGetIndex() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final int value = 42;
        when(delegate.getIndex()).thenReturn(value);
        assertEquals("same value", value, facade.getIndex());
    }

    @Test
    public void testHasNode() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.hasNode(path)).thenReturn(true);
        assertTrue("is true", facade.hasNode(path));
    }

    @Test
    public void testHasProperty() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.hasProperty(path)).thenReturn(true);
        assertTrue("is true", facade.hasProperty(path));
    }

    @Test
    public void testHasNodes() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        when(delegate.hasNodes()).thenReturn(true);
        assertTrue("is true", facade.hasNodes());
    }

    @Test
    public void testHasProperties() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        when(delegate.hasProperties()).thenReturn(true);
        assertTrue("is true", facade.hasProperties());
    }

    @Test
    public void testGetPrimaryNodeType() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final NodeType value = mock(NodeType.class);
        when(delegate.getPrimaryNodeType()).thenReturn(value);
        assertSame("same value", value, facade.getPrimaryNodeType());
    }

    @Test
    public void testGetMixinNodeTypes() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final NodeType[] value = new NodeType[0];
        when(delegate.getMixinNodeTypes()).thenReturn(value);
        assertSame("same value", value, facade.getMixinNodeTypes());
    }

    @Test
    public void testIsNodeType() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String value = "type";
        when(delegate.isNodeType(value)).thenReturn(true);
        assertTrue("is true", facade.isNodeType(value));
    }

    @Test
    public void testCanAddMixin() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String value = "type";
        when(delegate.canAddMixin(value)).thenReturn(true);
        assertTrue("is true", facade.canAddMixin(value));
    }

    @Test
    public void testGetDefinition() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final NodeDefinition value = mock(NodeDefinition.class);
        when(delegate.getDefinition()).thenReturn(value);
        assertSame("same value", value, facade.getDefinition());
    }

    @Test
    public void testGetCorrespondingNodePath() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String workspace = "workspace";
        final String value = "/correct/path";
        when(delegate.getCorrespondingNodePath(workspace)).thenReturn(value);
        assertSame("same value", value, facade.getCorrespondingNodePath(workspace));
    }

    @Test
    public void testIsCheckedOut() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        when(delegate.isCheckedOut()).thenReturn(true);
        assertTrue("is true", facade.isCheckedOut());

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testHoldsLock() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        when(delegate.holdsLock()).thenReturn(true);
        assertTrue("is true", facade.holdsLock());
    }

    @Test
    public void testIsLocked() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        when(delegate.isLocked()).thenReturn(true);
        assertTrue("is true", facade.isLocked());
    }

    @Test
    public void testGetAllowedLifecycleTransitions() throws Exception {
        Node delegate = mock(Node.class);
        NodeFacade<Node, Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getAllowedLifecycleTransistions()).thenReturn(value);
        assertSame("same value", value, facade.getAllowedLifecycleTransistions());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddNode1() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.addNode("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddNode2() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.addNode("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROOrderBefore() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.orderBefore("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyValue1() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (Value) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyValue2() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (Value) null, PropertyType.UNDEFINED);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyValues1() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", new Value[0]);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyValues2() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", new Value[0], PropertyType.UNDEFINED);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyString1() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (String) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyString2() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (String) null, PropertyType.UNDEFINED);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyStrings1() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", new String[0]);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyStrings2() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", new String[0], PropertyType.UNDEFINED);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyInputStream() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (InputStream) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyBinary() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (Binary) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyBoolean() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", false);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyDouble() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", 0.0D);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyBigDecimal() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", new BigDecimal("0.0"));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyLong() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", 0L);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyCalendar() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", Calendar.getInstance());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyNode() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setProperty("", (Node) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPrimaryType() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.setPrimaryType("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddMixin() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.addMixin("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveMixin() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.removeMixin("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCheckin() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.checkin();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCheckout() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.checkout();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRODoneMerge() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.doneMerge(null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCancelMerge() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.cancelMerge(null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROUpdate() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.update("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROMerge() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.merge("", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveSharedSet() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.removeSharedSet();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveShare() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.removeShare();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreString() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.restore("", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreVersion2() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.restore((Version) null, true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreVersion3() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.restore(null, "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreByLabel() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.restoreByLabel("", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROLock() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.lock(true, true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROUnlock() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.unlock();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROFollowLifecycleTransition() throws Exception {
        NodeFacade<Node, Session> facade = getFacade(mock(Node.class));
        facade.followLifecycleTransition("");
    }
}