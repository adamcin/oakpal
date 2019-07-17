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

import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class QueryObjectModelFactoryFacadeTest {

    QueryObjectModelFactoryFacade<Session> getFacade(final @NotNull QueryObjectModelFactory delegate) {
        return new QueryObjectModelFactoryFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testCreateQuery() throws Exception {
        final Source source = mock(Source.class);
        final Constraint constraint = mock(Constraint.class);
        final Ordering[] orderings = new Ordering[0];
        final Column[] columns = new Column[0];
        new FacadeGetterMapping.Tester<>(QueryObjectModelFactory.class, this::getFacade)
                .testFacadeGetter(QueryObjectModel.class, QueryObjectModelFacade.class,
                        delegate -> delegate.createQuery(source, constraint, orderings, columns));
    }

    @Test
    public void testSelector() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Selector value = mock(Selector.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.selector(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.selector(arg1, arg2));
    }

    @Test
    public void testJoin() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Join value = mock(Join.class);
        final Source arg1 = mock(Source.class);
        final Source arg2 = mock(Source.class);
        final String arg3 = "arg3";
        final JoinCondition arg4 = mock(JoinCondition.class);
        when(delegate.join(arg1, arg2, arg3, arg4)).thenReturn(value);
        assertSame("is same value", value, facade.join(arg1, arg2, arg3, arg4));
    }

    @Test
    public void testEquiJoinCondition() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final EquiJoinCondition value = mock(EquiJoinCondition.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        final String arg3 = "arg3";
        final String arg4 = "arg4";
        when(delegate.equiJoinCondition(arg1, arg2, arg3, arg4)).thenReturn(value);
        assertSame("is same value", value, facade.equiJoinCondition(arg1, arg2, arg3, arg4));
    }

    @Test
    public void testSameNodeJoinCondition() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final SameNodeJoinCondition value = mock(SameNodeJoinCondition.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        final String arg3 = "arg3";
        when(delegate.sameNodeJoinCondition(arg1, arg2, arg3)).thenReturn(value);
        assertSame("is same value", value, facade.sameNodeJoinCondition(arg1, arg2, arg3));
    }

    @Test
    public void testChildNodeJoinCondition() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final ChildNodeJoinCondition value = mock(ChildNodeJoinCondition.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.childNodeJoinCondition(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.childNodeJoinCondition(arg1, arg2));
    }

    @Test
    public void testDescendantNodeJoinCondition() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final DescendantNodeJoinCondition value = mock(DescendantNodeJoinCondition.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.descendantNodeJoinCondition(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.descendantNodeJoinCondition(arg1, arg2));
    }

    @Test
    public void testAnd() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final And value = mock(And.class);
        final Constraint arg1 = mock(Constraint.class);
        final Constraint arg2 = mock(Constraint.class);
        when(delegate.and(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.and(arg1, arg2));
    }

    @Test
    public void testOr() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Or value = mock(Or.class);
        final Constraint arg1 = mock(Constraint.class);
        final Constraint arg2 = mock(Constraint.class);
        when(delegate.or(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.or(arg1, arg2));
    }

    @Test
    public void testNot() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Not value = mock(Not.class);
        final Constraint arg1 = mock(Constraint.class);
        when(delegate.not(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.not(arg1));
    }

    @Test
    public void testComparison() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Comparison value = mock(Comparison.class);
        final DynamicOperand arg1 = mock(DynamicOperand.class);
        final String arg2 = "arg2";
        final StaticOperand arg3 = mock(StaticOperand.class);
        when(delegate.comparison(arg1, arg2, arg3)).thenReturn(value);
        assertSame("is same value", value, facade.comparison(arg1, arg2, arg3));
    }

    @Test
    public void testPropertyExistence() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final PropertyExistence value = mock(PropertyExistence.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.propertyExistence(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.propertyExistence(arg1, arg2));
    }

    @Test
    public void testFullTextSearch() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final FullTextSearch value = mock(FullTextSearch.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        final StaticOperand arg3 = mock(StaticOperand.class);
        when(delegate.fullTextSearch(arg1, arg2, arg3)).thenReturn(value);
        assertSame("is same value", value, facade.fullTextSearch(arg1, arg2, arg3));
    }

    @Test
    public void testSameNode() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final SameNode value = mock(SameNode.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.sameNode(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.sameNode(arg1, arg2));
    }

    @Test
    public void testChildNode() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final ChildNode value = mock(ChildNode.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.childNode(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.childNode(arg1, arg2));
    }

    @Test
    public void testDescendantNode() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final DescendantNode value = mock(DescendantNode.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.descendantNode(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.descendantNode(arg1, arg2));
    }

    @Test
    public void testPropertyValue() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final PropertyValue value = mock(PropertyValue.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        when(delegate.propertyValue(arg1, arg2)).thenReturn(value);
        assertSame("is same value", value, facade.propertyValue(arg1, arg2));
    }

    @Test
    public void testLength() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Length value = mock(Length.class);
        final PropertyValue arg1 = mock(PropertyValue.class);
        when(delegate.length(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.length(arg1));
    }

    @Test
    public void testNodeName() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final NodeName value = mock(NodeName.class);
        final String arg1 = "arg1";
        when(delegate.nodeName(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.nodeName(arg1));
    }

    @Test
    public void testNodeLocalName() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final NodeLocalName value = mock(NodeLocalName.class);
        final String arg1 = "arg1";
        when(delegate.nodeLocalName(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.nodeLocalName(arg1));
    }

    @Test
    public void testFullTextSearchScore() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final FullTextSearchScore value = mock(FullTextSearchScore.class);
        final String arg1 = "arg1";
        when(delegate.fullTextSearchScore(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.fullTextSearchScore(arg1));
    }

    @Test
    public void testLowerCase() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final LowerCase value = mock(LowerCase.class);
        final DynamicOperand arg1 = mock(DynamicOperand.class);
        when(delegate.lowerCase(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.lowerCase(arg1));
    }

    @Test
    public void testUpperCase() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final UpperCase value = mock(UpperCase.class);
        final DynamicOperand arg1 = mock(DynamicOperand.class);
        when(delegate.upperCase(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.upperCase(arg1));
    }

    @Test
    public void testBindVariable() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final BindVariableValue value = mock(BindVariableValue.class);
        final String arg1 = "arg1";
        when(delegate.bindVariable(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.bindVariable(arg1));
    }

    @Test
    public void testLiteral() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Literal value = mock(Literal.class);
        final Value arg1 = mock(Value.class);
        when(delegate.literal(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.literal(arg1));
    }

    @Test
    public void testAscending() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Ordering value = mock(Ordering.class);
        final DynamicOperand arg1 = mock(DynamicOperand.class);
        when(delegate.ascending(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.ascending(arg1));
    }

    @Test
    public void testDescending() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Ordering value = mock(Ordering.class);
        final DynamicOperand arg1 = mock(DynamicOperand.class);
        when(delegate.descending(arg1)).thenReturn(value);
        assertSame("is same value", value, facade.descending(arg1));
    }

    @Test
    public void testColumn() throws RepositoryException {
        QueryObjectModelFactory delegate = mock(QueryObjectModelFactory.class);
        QueryObjectModelFactoryFacade<Session> facade = getFacade(delegate);
        final Column value = mock(Column.class);
        final String arg1 = "arg1";
        final String arg2 = "arg2";
        final String arg3 = "arg3";
        when(delegate.column(arg1, arg2, arg3)).thenReturn(value);
        assertSame("is same value", value, facade.column(arg1, arg2, arg3));
    }
}