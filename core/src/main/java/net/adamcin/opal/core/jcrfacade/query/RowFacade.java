/*
 * Copyright 2017 Mark Adamcin
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

package net.adamcin.opal.core.jcrfacade.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;

import net.adamcin.opal.core.jcrfacade.NodeFacade;
import net.adamcin.opal.core.jcrfacade.SessionFacade;

/**
 * Wraps {@link Row} to ensure returned nodes are wrapped appropriately.
 */
public class RowFacade implements Row {
    private final Row delegate;
    private final SessionFacade session;

    public RowFacade(Row delegate, SessionFacade session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public Value[] getValues() throws RepositoryException {
        return delegate.getValues();
    }

    @Override
    public Value getValue(String columnName) throws RepositoryException {
        return delegate.getValue(columnName);
    }

    @Override
    public Node getNode() throws RepositoryException {
        return NodeFacade.wrap(delegate.getNode(), session);
    }

    @Override
    public Node getNode(String selectorName) throws RepositoryException {
        return NodeFacade.wrap(delegate.getNode(selectorName), session);
    }

    @Override
    public String getPath() throws RepositoryException {
        return delegate.getPath();
    }

    @Override
    public String getPath(String selectorName) throws RepositoryException {
        return delegate.getPath(selectorName);
    }

    @Override
    public double getScore() throws RepositoryException {
        return delegate.getScore();
    }

    @Override
    public double getScore(String selectorName) throws RepositoryException {
        return delegate.getScore(selectorName);
    }
}
