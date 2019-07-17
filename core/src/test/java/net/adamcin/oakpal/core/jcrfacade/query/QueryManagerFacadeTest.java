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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModelFactory;

import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.query.qom.QueryObjectModelFactoryFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class QueryManagerFacadeTest {

    QueryManagerFacade<Session> getFacade(final @NotNull QueryManager delegate) {
        return new QueryManagerFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        final Node node = mock(Node.class);
        new FacadeGetterMapping.Tester<>(QueryManager.class, this::getFacade)
                .testFacadeGetter(Query.class, QueryFacade.class, delegate -> delegate.createQuery("", ""))
                .testFacadeGetter(Query.class, QueryFacade.class, delegate -> delegate.getQuery(node))
                .testFacadeGetter(QueryObjectModelFactory.class, QueryObjectModelFactoryFacade.class,
                        QueryManager::getQOMFactory);
    }

    @Test
    public void testGetSupportedQueryLanguages() throws Exception {
        QueryManager delegate = mock(QueryManager.class);
        QueryManagerFacade<Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getSupportedQueryLanguages()).thenReturn(value);
        assertSame("same value", value, facade.getSupportedQueryLanguages());
    }
}