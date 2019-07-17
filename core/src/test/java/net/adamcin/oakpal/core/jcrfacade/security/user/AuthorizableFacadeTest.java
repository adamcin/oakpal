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

package net.adamcin.oakpal.core.jcrfacade.security.user;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import javax.jcr.Value;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class AuthorizableFacadeTest {

    AuthorizableFacade<Authorizable> getFacade(final @NotNull Authorizable delegate) {
        return new AuthorizableFacade<>(delegate);
    }


    interface SuperAuthz extends Authorizable {
        boolean isSuperAuthz();
    }

    @Test
    public void testEnsureBestWrapper() {
        assertNull("just null", AuthorizableFacade.ensureBestWrapper(null));
        Authorizable justDelegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> justFacade = getFacade(justDelegate);
        assertSame("same facade", justFacade, AuthorizableFacade.ensureBestWrapper(justFacade));
        assertTrue("is facade", AuthorizableFacade.ensureBestWrapper(justDelegate) instanceof AuthorizableFacade);
        User userDelegate = mock(User.class);
        assertTrue("is facade", AuthorizableFacade.ensureBestWrapper(userDelegate) instanceof UserFacade);
        Group groupDelegate = mock(Group.class);
        assertTrue("is facade", AuthorizableFacade.ensureBestWrapper(groupDelegate) instanceof GroupFacade);
        SuperAuthz superDelegate = mock(SuperAuthz.class);
        assertTrue("is facade", AuthorizableFacade.ensureBestWrapper(superDelegate) instanceof AuthorizableFacade);
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Authorizable.class, this::getFacade)
                .testFacadeIteratorGetter(Group.class, GroupFacade.class, Authorizable::memberOf)
                .testFacadeIteratorGetter(Group.class, GroupFacade.class, Authorizable::declaredMemberOf);
    }

    @Test
    public void testGetID() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final String value = "foo";
        when(delegate.getID()).thenReturn(value);
        assertSame("same value", value, facade.getID());
    }

    @Test
    public void testGetPath() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final String value = "foo";
        when(delegate.getPath()).thenReturn(value);
        assertSame("same value", value, facade.getPath());
    }

    @Test
    public void testIsGroup() {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        when(delegate.isGroup()).thenReturn(true);
        assertTrue("is true", facade.isGroup());
    }

    @Test
    public void testGetPrincipal() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final Principal value = EveryonePrincipal.getInstance();
        when(delegate.getPrincipal()).thenReturn(value);
        assertSame("same value", value, facade.getPrincipal());
    }

    @Test
    public void testGetPropertyNames() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final Iterator<String> value = Collections.emptyIterator();
        when(delegate.getPropertyNames()).thenReturn(value);
        assertSame("same value", value, facade.getPropertyNames());
    }

    @Test
    public void testGetPropertyNamesRelPath() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final String path = "correct/path";
        final Iterator<String> value = Collections.emptyIterator();
        when(delegate.getPropertyNames(path)).thenReturn(value);
        assertSame("same value", value, facade.getPropertyNames(path));
    }

    @Test
    public void testHasProperty() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final String path = "correct/path";
        when(delegate.hasProperty(path)).thenReturn(true);
        assertTrue("same value", facade.hasProperty(path));
    }

    @Test
    public void testGetProperty() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        final String path = "correct/path";
        final Value[] value = new Value[0];
        when(delegate.getProperty(path)).thenReturn(value);
        assertSame("same value", value, facade.getProperty(path));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyValue() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        facade.setProperty("", (Value) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPropertyValues() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        facade.setProperty("", new Value[0]);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemove() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        facade.remove();
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveProperty() throws Exception {
        Authorizable delegate = mock(Authorizable.class);
        AuthorizableFacade<Authorizable> facade = getFacade(delegate);
        facade.removeProperty("");
    }

}