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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class PropertyFacadeTest {

    PropertyFacade<Session> getFacade(final @NotNull Property mockProperty) {
        return new PropertyFacade<>(mockProperty, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test
    public void testGetValue() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final Value value = mock(Value.class);
        when(delegate.getValue()).thenReturn(value);
        assertSame("same value", value, facade.getValue());
    }

    @Test
    public void testGetValues() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final Value[] value = new Value[0];
        when(delegate.getValues()).thenReturn(value);
        assertSame("same value", value, facade.getValues());
    }

    @Test
    public void testGetString() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final String value = "value";
        when(delegate.getString()).thenReturn(value);
        assertSame("same value", value, facade.getString());

    }

    @SuppressWarnings("deprecated")
    @Test
    public void testGetStream() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final InputStream value = mock(InputStream.class);
        when(delegate.getStream()).thenReturn(value);
        assertSame("same value", value, facade.getStream());
    }

    @Test
    public void testGetBinary() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final Binary value = mock(Binary.class);
        when(delegate.getBinary()).thenReturn(value);
        assertSame("same value", value, facade.getBinary());

    }

    @Test
    public void testGetLong() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final long value = 10L;
        when(delegate.getLong()).thenReturn(value);
        assertEquals("same value", value, facade.getLong());

    }

    @Test
    public void testGetDouble() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final double value = 10.0D;
        when(delegate.getDouble()).thenReturn(value);
        assertEquals("same value", value, facade.getDouble(), 1.0D);
    }

    @Test
    public void testGetDecimal() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final BigDecimal value = new BigDecimal("41.0");
        when(delegate.getDecimal()).thenReturn(value);
        assertSame("same value", value, facade.getDecimal());
    }

    @Test
    public void testGetDate() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final Calendar value = Calendar.getInstance();
        when(delegate.getDate()).thenReturn(value);
        assertSame("same value", value, facade.getDate());
    }

    @Test
    public void testGetBoolean() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        when(delegate.getBoolean()).thenReturn(true);
        assertTrue("is true", facade.getBoolean());
    }

    @Test
    public void testGetNode() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final Node value = mock(Node.class);
        when(delegate.getNode()).thenReturn(value);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        final Node fromFacade = facade.getNode();
        assertEquals("same path", path, fromFacade.getPath());
        assertTrue("is facade", fromFacade instanceof NodeFacade);
    }

    @Test
    public void testGetProperty() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final Property value = mock(Property.class);
        when(delegate.getProperty()).thenReturn(value);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        final Property fromFacade = facade.getProperty();
        assertEquals("same path", path, fromFacade.getPath());
        assertTrue("is facade", fromFacade instanceof PropertyFacade);
    }

    @Test
    public void testGetLength() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final long value = 7L;
        when(delegate.getLength()).thenReturn(value);
        assertEquals("same length", value, facade.getLength());
    }

    @Test
    public void testGetLengths() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final long[] value = new long[0];
        when(delegate.getLengths()).thenReturn(value);
        assertEquals("same lengths", value, facade.getLengths());
    }

    @Test
    public void testGetDefinition() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final PropertyDefinition value = mock(PropertyDefinition.class);
        when(delegate.getDefinition()).thenReturn(value);
        assertSame("same value", value, facade.getDefinition());
    }

    @Test
    public void testGetType() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        final int type = PropertyType.WEAKREFERENCE;
        when(delegate.getType()).thenReturn(type);
        assertEquals("same type", type, facade.getType());
    }

    @Test
    public void testIsMultiple() throws Exception {
        Property delegate = mock(Property.class);
        PropertyFacade<Session> facade = getFacade(delegate);
        when(delegate.isMultiple()).thenReturn(true);
        assertTrue("is true", facade.isMultiple());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueValue() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue((Value) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueValues() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(new Value[0]);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueString() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueStrings() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(new String[0]);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueInputStream() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue((InputStream) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueBinary() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue((Binary) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueLong() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(0L);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueDouble() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(0.0D);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueBigDecimal() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(new BigDecimal("0.0"));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueCalendar() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(Calendar.getInstance());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueBoolean() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue(true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetValueNode() throws Exception {
        PropertyFacade<Session> facade = getFacade(mock(Property.class));
        facade.setValue((Node) null);
    }

}