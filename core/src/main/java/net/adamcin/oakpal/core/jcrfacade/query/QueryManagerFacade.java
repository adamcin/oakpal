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

package net.adamcin.oakpal.core.jcrfacade.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModelFactory;

import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeFacade;

/**
 * Wraps {@link QueryManager} to ensure returned items are wrapped with appropriate facades.
 */
public class QueryManagerFacade<S extends Session> implements QueryManager {
    private final QueryManager delegate;
    private final SessionFacade<S> session;

    public QueryManagerFacade(QueryManager delegate, SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public Query createQuery(String statement, String language) throws RepositoryException {
        Query internal = delegate.createQuery(statement, language);
        return new QueryFacade<>(internal, session);
    }

    @Override
    public QueryObjectModelFactory getQOMFactory() {
        return delegate.getQOMFactory();
    }

    @Override
    public Query getQuery(Node node) throws RepositoryException {
        Query internal = delegate.getQuery(NodeFacade.unwrap(node));
        return new QueryFacade(internal, session);
    }

    @Override
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return delegate.getSupportedQueryLanguages();
    }
}
