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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class UserManagerFacadeTest {

    UserManagerFacade getFacade(final @NotNull UserManager delegate) {
        return new UserManagerFacade(delegate);
    }

    @Test
    public void testFacadeGetters() throws Exception {
        final Query mockQuery = mock(Query.class);
        new FacadeGetterMapping.Tester<>(UserManager.class, this::getFacade)
                .testFacadeGetter(Authorizable.class, AuthorizableFacade.class, delegate -> delegate.getAuthorizable(""))
                .testFacadeGetter(Authorizable.class, AuthorizableFacade.class, delegate -> delegate.getAuthorizable(EveryonePrincipal.getInstance()))
                .testFacadeGetter(Authorizable.class, AuthorizableFacade.class, delegate -> delegate.getAuthorizableByPath(""))
                .testFacadeIteratorGetter(Authorizable.class, AuthorizableFacade.class,
                        delegate -> delegate.findAuthorizables(mockQuery))
                .testFacadeIteratorGetter(Authorizable.class, AuthorizableFacade.class,
                        delegate -> delegate.findAuthorizables("", ""))
                .testFacadeIteratorGetter(Authorizable.class, AuthorizableFacade.class,
                        delegate -> delegate.findAuthorizables("", "", UserManager.SEARCH_TYPE_AUTHORIZABLE));
    }

    @Test
    public void testIsAutoSave() {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        assertFalse("is false", facade.isAutoSave());
    }

    @Test(expected = AuthorizableTypeException.class)
    public void testGetAuthorizableThrows() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        final AuthorizableFacadeTest.SuperAuthz superValue = mock(AuthorizableFacadeTest.SuperAuthz.class);
        when(delegate.getAuthorizable("foo", AuthorizableFacadeTest.SuperAuthz.class)).thenReturn(superValue);
        facade.getAuthorizable("foo", AuthorizableFacadeTest.SuperAuthz.class);
    }

    @Test
    public void testGetAuthorizable() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        final User userValue = mock(User.class);
        when(userValue.getID()).thenReturn("foo");
        final Group groupValue = mock(Group.class);
        when(groupValue.getID()).thenReturn("bar");
        when(delegate.getAuthorizable(anyString(), isNull())).thenReturn(null);
        when(delegate.getAuthorizable("foo", User.class)).thenReturn(userValue);
        when(delegate.getAuthorizable("foo", Authorizable.class)).thenReturn(userValue);
        when(delegate.getAuthorizable("bar", Group.class)).thenReturn(groupValue);
        when(delegate.getAuthorizable("bar", Authorizable.class)).thenReturn(groupValue);

        assertNull("is null", facade.getAuthorizable("", null));
        Authorizable userFacadeAuthz = facade.getAuthorizable("foo", Authorizable.class);
        assertTrue("is user facade", userFacadeAuthz instanceof UserFacade);
        assertEquals("is same id", "foo", userFacadeAuthz.getID());
        User userFacade = facade.getAuthorizable("foo", User.class);
        assertTrue("is user facade", userFacade instanceof UserFacade);
        assertEquals("is same id", "foo", userFacade.getID());
        Authorizable groupFacadeAuthz = facade.getAuthorizable("bar", Authorizable.class);
        assertTrue("is group facade", groupFacadeAuthz instanceof GroupFacade);
        assertEquals("is same id", "bar", groupFacadeAuthz.getID());
        Group groupFacade = facade.getAuthorizable("bar", Group.class);
        assertTrue("is group facade", groupFacade instanceof GroupFacade);
        assertEquals("is same id", "bar", groupFacade.getID());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateUser2() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createUser("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateUser4() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createUser("", "", EveryonePrincipal.getInstance(), "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateSystemUser() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createSystemUser("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateGroupString() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createGroup("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateGroupPrincipal() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createGroup(EveryonePrincipal.getInstance());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateGroupPrincipal2() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createGroup(EveryonePrincipal.getInstance(), "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateGroupStringPrincipal() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.createGroup("", EveryonePrincipal.getInstance(), "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAutoSave() throws Exception {
        UserManager delegate = mock(UserManager.class);
        UserManagerFacade facade = getFacade(delegate);
        facade.autoSave(true);
    }
}