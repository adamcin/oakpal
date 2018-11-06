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

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import net.adamcin.oakpal.core.ListenerReadOnlyException;

/**
 * Wraps {@link Property} to prevent writes.
 */
public class PropertyFacade<P extends Property> extends ItemFacade<P> implements Property {

    public PropertyFacade(P delegate, SessionFacade session) {
        super(delegate, session);
    }

    @Override
    public void setValue(Value value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(Value[] values) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(String value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(String[] values) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(InputStream value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(Binary value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(long value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(double value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(BigDecimal value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(Calendar value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(boolean value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void setValue(Node value) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public Value getValue() throws RepositoryException {
        return delegate.getValue();
    }

    @Override
    public Value[] getValues() throws RepositoryException {
        return delegate.getValues();
    }

    @Override
    public String getString() throws RepositoryException {
        return delegate.getString();
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        return delegate.getStream();
    }

    @Override
    public Binary getBinary() throws RepositoryException {
        return delegate.getBinary();
    }

    @Override
    public long getLong() throws RepositoryException {
        return delegate.getLong();
    }

    @Override
    public double getDouble() throws RepositoryException {
        return delegate.getDouble();
    }

    @Override
    public BigDecimal getDecimal() throws RepositoryException {
        return delegate.getDecimal();
    }

    @Override
    public Calendar getDate() throws RepositoryException {
        return delegate.getDate();
    }

    @Override
    public boolean getBoolean() throws RepositoryException {
        return delegate.getBoolean();
    }

    @Override
    public Node getNode() throws RepositoryException {
        Node internalNode = delegate.getNode();
        return NodeFacade.wrap(internalNode, session);
    }

    @Override
    public Property getProperty() throws RepositoryException {
        Property internalProperty = delegate.getProperty();
        return new PropertyFacade<>(internalProperty, session);
    }

    @Override
    public long getLength() throws RepositoryException {
        return delegate.getLength();
    }

    @Override
    public long[] getLengths() throws RepositoryException {
        return delegate.getLengths();
    }

    @Override
    public PropertyDefinition getDefinition() throws RepositoryException {
        return delegate.getDefinition();
    }

    @Override
    public int getType() throws RepositoryException {
        return delegate.getType();
    }

    @Override
    public boolean isMultiple() throws RepositoryException {
        return delegate.isMultiple();
    }

}
