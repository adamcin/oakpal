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
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps {@link Query} to ensure returned objects are wrapped with appropriate facades.
 *
 * @param <Q> the query type, which may be either {@link Query} or {@link javax.jcr.query.qom.QueryObjectModel}
 * @param <S> the session type, which may be either {@link Session} or {@link org.apache.jackrabbit.api.JackrabbitSession}
 */
public class QueryFacade<Q extends Query, S extends Session> implements Query {
    protected final @NotNull Q delegate;
    private final @NotNull SessionFacade<S> session;

    public QueryFacade(final @NotNull Q delegate, final @NotNull SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public QueryResult execute() throws RepositoryException {
        QueryResult internal = delegate.execute();
        return new QueryResultFacade<>(internal, session);
    }

    @Override
    public void setLimit(long limit) {
        delegate.setLimit(limit);
    }

    @Override
    public void setOffset(long offset) {
        delegate.setOffset(offset);
    }

    @Override
    public String getStatement() {
        return delegate.getStatement();
    }

    @Override
    public String getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public String getStoredQueryPath() throws RepositoryException {
        return delegate.getStoredQueryPath();
    }

    @Override
    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        delegate.bindValue(varName, value);
    }

    @Override
    public String[] getBindVariableNames() throws RepositoryException {
        return delegate.getBindVariableNames();
    }

    @Override
    public final Node storeAsNode(String absPath) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
