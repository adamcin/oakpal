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

package net.adamcin.oakpal.core.jcrfacade.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;

import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class RowFacadeTest {

    RowFacade<Session> getFacade(final @NotNull Row delegate) {
        return new RowFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testGetNode() throws Exception {
        new FacadeGetterMapping.Tester<>(Row.class, this::getFacade)
                .testFacadeGetter(Node.class, NodeFacade.class, Row::getNode)
                .testFacadeGetter(Node.class, NodeFacade.class, row -> row.getNode(""));
    }

    @Test
    public void testGetValues() throws Exception {
        Row delegate = mock(Row.class);
        RowFacade<Session> facade = getFacade(delegate);
        final Value[] value = new Value[0];
        when(delegate.getValues()).thenReturn(value);
        assertSame("same value", value, facade.getValues());
    }

    @Test
    public void testGetValue() throws Exception {
        Row delegate = mock(Row.class);
        RowFacade<Session> facade = getFacade(delegate);
        final Value value = mock(Value.class);
        final String arg1 = "arg1";
        when(delegate.getValue(arg1)).thenReturn(value);
        assertSame("same value", value, facade.getValue(arg1));
    }

    @Test
    public void testGetPath() throws Exception {
        Row delegate = mock(Row.class);
        RowFacade<Session> facade = getFacade(delegate);
        final String value = "/correct/path";
        when(delegate.getPath()).thenReturn(value);
        assertSame("same value", value, facade.getPath());
    }

    @Test
    public void testGetPathSelector() throws Exception {
        Row delegate = mock(Row.class);
        RowFacade<Session> facade = getFacade(delegate);
        final String value = "/correct/path";
        final String arg1 = "arg1";
        when(delegate.getPath(arg1)).thenReturn(value);
        assertSame("same value", value, facade.getPath(arg1));
    }

    @Test
    public void testGetScore() throws Exception {
        Row delegate = mock(Row.class);
        RowFacade<Session> facade = getFacade(delegate);
        final double value = 42.0D;
        when(delegate.getScore()).thenReturn(value);
        assertEquals("same value", value, facade.getScore(), 1.0D);
    }

    @Test
    public void testGetScoreSelector() throws Exception {
        Row delegate = mock(Row.class);
        RowFacade<Session> facade = getFacade(delegate);
        final double value = 42.0D;
        final String arg1 = "arg1";
        when(delegate.getScore(arg1)).thenReturn(value);
        assertEquals("same value", value, facade.getScore(arg1), 1.0D);
    }
}