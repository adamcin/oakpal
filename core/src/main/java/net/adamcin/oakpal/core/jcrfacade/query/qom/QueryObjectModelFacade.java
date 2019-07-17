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

import javax.jcr.Session;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;

import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import net.adamcin.oakpal.core.jcrfacade.query.QueryFacade;
import org.jetbrains.annotations.NotNull;

/**
 * Extends {@link QueryFacade} to wrap {@link QueryObjectModel}, providing additional methods.
 *
 * @param <S> the session type parameter
 */
public final class QueryObjectModelFacade<S extends Session> extends QueryFacade<QueryObjectModel, S> implements QueryObjectModel {

    @SuppressWarnings("WeakerAccess")
    public QueryObjectModelFacade(final @NotNull QueryObjectModel delegate, final @NotNull SessionFacade<S> session) {
        super(delegate, session);
    }

    @Override
    public Source getSource() {
        return delegate.getSource();
    }

    @Override
    public Constraint getConstraint() {
        return delegate.getConstraint();
    }

    @Override
    public Ordering[] getOrderings() {
        return delegate.getOrderings();
    }

    @Override
    public Column[] getColumns() {
        return delegate.getColumns();
    }
}
