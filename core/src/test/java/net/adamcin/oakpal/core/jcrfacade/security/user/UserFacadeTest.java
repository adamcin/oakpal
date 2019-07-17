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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Credentials;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class UserFacadeTest {

    UserFacade getFacade(final @NotNull User delegate) {
        return new UserFacade(delegate);
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(User.class, this::getFacade)
                .testFacadeGetter(Impersonation.class, ImpersonationFacade.class, User::getImpersonation);
    }

    @Test
    public void testIsAdmin() {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        when(delegate.isAdmin()).thenReturn(true);
        assertTrue("is true", facade.isAdmin());
    }

    @Test
    public void testIsSystemUser() {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        when(delegate.isSystemUser()).thenReturn(true);
        assertTrue("is true", facade.isSystemUser());
    }

    @Test
    public void testIsDisabled() throws Exception {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        when(delegate.isDisabled()).thenReturn(true);
        assertTrue("is true", facade.isDisabled());
    }

    @Test
    public void testGetCredentials() throws Exception {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        final Credentials value = mock(Credentials.class);
        when(delegate.getCredentials()).thenReturn(value);
        assertSame("same value", value, facade.getCredentials());
    }

    @Test
    public void testGetDisabledReason() throws Exception {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        final String value = "reason";
        when(delegate.getDisabledReason()).thenReturn(value);
        assertSame("same value", value, facade.getDisabledReason());
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRODisable() throws Exception {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        facade.disable("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROChangePassword() throws Exception {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        facade.changePassword("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROChangePassword2() throws Exception {
        User delegate = mock(User.class);
        UserFacade facade = getFacade(delegate);
        facade.changePassword("", "");
    }
}