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

package net.adamcin.oakpal.core.jcrfacade.nodetype;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class NodeTypeManagerFacadeTest {

    NodeTypeManagerFacade getFacade(final @NotNull NodeTypeManager mockManager) {
        return new NodeTypeManagerFacade(mockManager);
    }

    @Test
    public void testGetNodeType() throws Exception {
        NodeTypeManager delegate = mock(NodeTypeManager.class);
        NodeTypeManagerFacade facade = getFacade(delegate);
        final String type = "type";
        final NodeType value = mock(NodeType.class);
        when(delegate.getNodeType(type)).thenReturn(value);
        assertSame("same value", value, facade.getNodeType(type));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORegisterNodeType() throws Exception {
        NodeTypeManagerFacade facade = getFacade(mock(NodeTypeManager.class));
        facade.registerNodeType(null, true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORegisterNodeTypes() throws Exception {
        NodeTypeManagerFacade facade = getFacade(mock(NodeTypeManager.class));
        facade.registerNodeTypes(new NodeTypeDefinition[0], true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROUnregisterNodeType() throws Exception {
        NodeTypeManagerFacade facade = getFacade(mock(NodeTypeManager.class));
        facade.unregisterNodeType("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROUnregisterNodeTypes() throws Exception {
        NodeTypeManagerFacade facade = getFacade(mock(NodeTypeManager.class));
        facade.unregisterNodeTypes(new String[0]);
    }
}