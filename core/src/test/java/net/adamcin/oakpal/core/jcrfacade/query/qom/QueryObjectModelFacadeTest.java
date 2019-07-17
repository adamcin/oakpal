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

package net.adamcin.oakpal.core.jcrfacade.query.qom;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;

import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class QueryObjectModelFacadeTest {

    QueryObjectModelFacade<Session> getFacade(final @NotNull QueryObjectModel delegate) {
        return new QueryObjectModelFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testGetSource() {
        QueryObjectModel delegate = mock(QueryObjectModel.class);
        QueryObjectModelFacade<Session> facade = getFacade(delegate);
        final Source value = mock(Source.class);
        when(delegate.getSource()).thenReturn(value);
        assertSame("same value", value, facade.getSource());
    }

    @Test
    public void testGetConstraint() {
        QueryObjectModel delegate = mock(QueryObjectModel.class);
        QueryObjectModelFacade<Session> facade = getFacade(delegate);
        final Constraint value = mock(Constraint.class);
        when(delegate.getConstraint()).thenReturn(value);
        assertSame("same value", value, facade.getConstraint());
    }

    @Test
    public void testGetOrderings() {
        QueryObjectModel delegate = mock(QueryObjectModel.class);
        QueryObjectModelFacade<Session> facade = getFacade(delegate);
        final Ordering[] value = new Ordering[0];
        when(delegate.getOrderings()).thenReturn(value);
        assertSame("same value", value, facade.getOrderings());
    }

    @Test
    public void testGetColumns() {
        QueryObjectModel delegate = mock(QueryObjectModel.class);
        QueryObjectModelFacade<Session> facade = getFacade(delegate);
        final Column[] value = new Column[0];
        when(delegate.getColumns()).thenReturn(value);
        assertSame("same value", value, facade.getColumns());
    }
}