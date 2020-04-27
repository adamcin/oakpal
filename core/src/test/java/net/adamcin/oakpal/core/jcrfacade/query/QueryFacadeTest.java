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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class QueryFacadeTest {

    QueryFacade<Query, Session> getFacade(final @NotNull Query mockQuery) {
        return new QueryFacade<>(mockQuery, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test
    public void testExecute() throws Exception {
        new FacadeGetterMapping.Tester<>(Query.class, this::getFacade)
                .testFacadeGetter(QueryResult.class, QueryResultFacade.class, Query::execute);
    }

    @Test
    public void testSetLimit() {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        CompletableFuture<Long> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete(invoked.getArgument(0))).when(delegate).setLimit(anyLong());
        final long value = 42L;
        facade.setLimit(value);
        assertEquals("same value in latch", (Long) value, latch.getNow(0L));
    }

    @Test
    public void testSetOffset() {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        CompletableFuture<Long> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete(invoked.getArgument(0))).when(delegate).setOffset(anyLong());
        final long value = 42L;
        facade.setOffset(value);
        assertEquals("same value in latch", (Long) value, latch.getNow(0L));
    }

    @Test
    public void testBindValue() throws Exception {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        CompletableFuture<Map.Entry<String, Value>> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete(Fun.toEntry(invoked.getArgument(0), invoked.getArgument(1))))
                .when(delegate).bindValue(anyString(), any(Value.class));
        final String varName = "foo";
        final Value value = mock(Value.class);
        facade.bindValue(varName, value);
        final Map.Entry<String, Value> fromLatch = latch.getNow(Fun.toEntry("", mock(Value.class)));
        assertSame("same key in latch", varName, fromLatch.getKey());
        assertSame("same value in latch", value, fromLatch.getValue());
    }

    @Test
    public void testGetStatement() {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        final String value = "value";
        when(delegate.getStatement()).thenReturn(value);
        assertSame("same value", value, facade.getStatement());
    }

    @Test
    public void testGetLanguage() {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        final String value = "value";
        when(delegate.getLanguage()).thenReturn(value);
        assertSame("same value", value, facade.getLanguage());
    }

    @Test
    public void testGetStoredQueryPath() throws Exception {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        final String value = "value";
        when(delegate.getStoredQueryPath()).thenReturn(value);
        assertSame("same value", value, facade.getStoredQueryPath());
    }

    @Test
    public void testGetBindVariableNames() throws Exception {
        Query delegate = mock(Query.class);
        QueryFacade<Query, Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getBindVariableNames()).thenReturn(value);
        assertSame("same value", value, facade.getBindVariableNames());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROStoreAsNode() throws Exception {
        QueryFacade<Query, Session> facade = getFacade(mock(Query.class));
        facade.storeAsNode("");
    }
}