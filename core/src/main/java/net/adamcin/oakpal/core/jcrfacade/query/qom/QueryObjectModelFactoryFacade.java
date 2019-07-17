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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.SameNodeJoinCondition;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;

import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps {@link QueryObjectModelFactory} to ensure that {@link QueryObjectModel} instances are wrapped with
 * {@link QueryObjectModelFacade} read-only wrappers.
 *
 * @param <S> the session type parameter
 */
public class QueryObjectModelFactoryFacade<S extends Session> implements QueryObjectModelFactory {
    private final @NotNull QueryObjectModelFactory delegate;
    private final @NotNull SessionFacade<S> session;

    public QueryObjectModelFactoryFacade(@NotNull final QueryObjectModelFactory delegate, @NotNull final SessionFacade<S> session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public QueryObjectModel createQuery(final Source source,
                                        final Constraint constraint,
                                        final Ordering[] orderings,
                                        final Column[] columns) throws RepositoryException {
        QueryObjectModel internal = delegate.createQuery(source, constraint, orderings, columns);
        return new QueryObjectModelFacade<>(internal, session);
    }

    @Override
    public Selector selector(final String nodeTypeName, final String selectorName) throws RepositoryException {
        return delegate.selector(nodeTypeName, selectorName);
    }

    @Override
    public Join join(final Source left, final Source right, final String joinType, final JoinCondition joinCondition) throws RepositoryException {
        return delegate.join(left, right, joinType, joinCondition);
    }

    @Override
    public EquiJoinCondition equiJoinCondition(final String selector1Name,
                                               final String property1Name,
                                               final String selector2Name,
                                               final String property2Name) throws RepositoryException {
        return delegate.equiJoinCondition(selector1Name, property1Name, selector2Name, property2Name);
    }

    @Override
    public SameNodeJoinCondition sameNodeJoinCondition(final String selector1Name, final String selector2Name, final String selector2Path) throws RepositoryException {
        return delegate.sameNodeJoinCondition(selector1Name, selector2Name, selector2Path);
    }

    @Override
    public ChildNodeJoinCondition childNodeJoinCondition(final String childSelectorName, final String parentSelectorName) throws RepositoryException {
        return delegate.childNodeJoinCondition(childSelectorName, parentSelectorName);
    }

    @Override
    public DescendantNodeJoinCondition descendantNodeJoinCondition(final String descendantSelectorName, final String ancestorSelectorName) throws RepositoryException {
        return delegate.descendantNodeJoinCondition(descendantSelectorName, ancestorSelectorName);
    }

    @Override
    public And and(final Constraint constraint1, final Constraint constraint2) throws RepositoryException {
        return delegate.and(constraint1, constraint2);
    }

    @Override
    public Or or(final Constraint constraint1, final Constraint constraint2) throws RepositoryException {
        return delegate.or(constraint1, constraint2);
    }

    @Override
    public Not not(final Constraint constraint) throws RepositoryException {
        return delegate.not(constraint);
    }

    @Override
    public Comparison comparison(final DynamicOperand operand1, final String operator, final StaticOperand operand2) throws RepositoryException {
        return delegate.comparison(operand1, operator, operand2);
    }

    @Override
    public PropertyExistence propertyExistence(final String selectorName, final String propertyName) throws RepositoryException {
        return delegate.propertyExistence(selectorName, propertyName);
    }

    @Override
    public FullTextSearch fullTextSearch(final String selectorName, final String propertyName, final StaticOperand fullTextSearchExpression) throws RepositoryException {
        return delegate.fullTextSearch(selectorName, propertyName, fullTextSearchExpression);
    }

    @Override
    public SameNode sameNode(final String selectorName, final String path) throws RepositoryException {
        return delegate.sameNode(selectorName, path);
    }

    @Override
    public ChildNode childNode(final String selectorName, final String path) throws RepositoryException {
        return delegate.childNode(selectorName, path);
    }

    @Override
    public DescendantNode descendantNode(final String selectorName, final String path) throws RepositoryException {
        return delegate.descendantNode(selectorName, path);
    }

    @Override
    public PropertyValue propertyValue(final String selectorName, final String propertyName) throws RepositoryException {
        return delegate.propertyValue(selectorName, propertyName);
    }

    @Override
    public Length length(final PropertyValue propertyValue) throws RepositoryException {
        return delegate.length(propertyValue);
    }

    @Override
    public NodeName nodeName(final String selectorName) throws RepositoryException {
        return delegate.nodeName(selectorName);
    }

    @Override
    public NodeLocalName nodeLocalName(final String selectorName) throws RepositoryException {
        return delegate.nodeLocalName(selectorName);
    }

    @Override
    public FullTextSearchScore fullTextSearchScore(final String selectorName) throws RepositoryException {
        return delegate.fullTextSearchScore(selectorName);
    }

    @Override
    public LowerCase lowerCase(final DynamicOperand operand) throws RepositoryException {
        return delegate.lowerCase(operand);
    }

    @Override
    public UpperCase upperCase(final DynamicOperand operand) throws RepositoryException {
        return delegate.upperCase(operand);
    }

    @Override
    public BindVariableValue bindVariable(final String bindVariableName) throws RepositoryException {
        return delegate.bindVariable(bindVariableName);
    }

    @Override
    public Literal literal(final Value literalValue) throws RepositoryException {
        return delegate.literal(literalValue);
    }

    @Override
    public Ordering ascending(final DynamicOperand operand) throws RepositoryException {
        return delegate.ascending(operand);
    }

    @Override
    public Ordering descending(final DynamicOperand operand) throws RepositoryException {
        return delegate.descending(operand);
    }

    @Override
    public Column column(final String selectorName, final String propertyName, final String columnName) throws RepositoryException {
        return delegate.column(selectorName, propertyName, columnName);
    }
}
