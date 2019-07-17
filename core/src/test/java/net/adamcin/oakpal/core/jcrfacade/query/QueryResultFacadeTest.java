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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeIteratorFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class QueryResultFacadeTest {

    QueryResultFacade<Session> getFacade(final @NotNull QueryResult delegate) {
        return new QueryResultFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(QueryResult.class, this::getFacade)
                .testFacadeGetter(RowIterator.class, RowIteratorFacade.class, QueryResult::getRows)
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, QueryResult::getNodes);
    }

    @Test
    public void testGetColumnNames() throws Exception {
        QueryResult delegate = mock(QueryResult.class);
        QueryResultFacade<Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getColumnNames()).thenReturn(value);
        assertSame("same value", value, facade.getColumnNames());
    }

    @Test
    public void testGetSelectorNames() throws Exception {
        QueryResult delegate = mock(QueryResult.class);
        QueryResultFacade<Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getSelectorNames()).thenReturn(value);
        assertSame("same value", value, facade.getSelectorNames());
    }
}