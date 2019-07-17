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

package net.adamcin.oakpal.core.jcrfacade.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class JackrabbitAccessControlManagerFacadeTest {

    JackrabbitAccessControlManagerFacade getFacade(final @NotNull JackrabbitAccessControlManager delegate) {
        return new JackrabbitAccessControlManagerFacade(delegate);
    }

    @Test
    public void testGetApplicablePolicies() throws Exception {
        JackrabbitAccessControlManager delegate = mock(JackrabbitAccessControlManager.class);
        final Principal principal = EveryonePrincipal.getInstance();
        JackrabbitAccessControlPolicy[] policies = new JackrabbitAccessControlPolicy[0];
        when(delegate.getApplicablePolicies(principal)).thenReturn(policies);
        JackrabbitAccessControlManagerFacade facade = getFacade(delegate);
        assertSame("should return same array", policies, facade.getApplicablePolicies(principal));
    }

    @Test
    public void testGetPolicies() throws Exception {
        JackrabbitAccessControlManager delegate = mock(JackrabbitAccessControlManager.class);
        final Principal principal = EveryonePrincipal.getInstance();
        JackrabbitAccessControlPolicy[] policies = new JackrabbitAccessControlPolicy[0];
        when(delegate.getPolicies(principal)).thenReturn(policies);
        JackrabbitAccessControlManagerFacade facade = getFacade(delegate);
        assertSame("should return same array", policies, facade.getPolicies(principal));
    }

    @Test
    public void testGetEffectivePolicies() throws Exception {
        JackrabbitAccessControlManager delegate = mock(JackrabbitAccessControlManager.class);
        final Set<Principal> principals = Collections.singleton(EveryonePrincipal.getInstance());
        AccessControlPolicy[] policies = new AccessControlPolicy[0];
        when(delegate.getEffectivePolicies(principals)).thenReturn(policies);
        JackrabbitAccessControlManagerFacade facade = getFacade(delegate);
        assertSame("should return same array", policies, facade.getEffectivePolicies(principals));
    }

    @Test
    public void testHasPrivileges() throws Exception {
        JackrabbitAccessControlManager delegate = mock(JackrabbitAccessControlManager.class);
        JackrabbitAccessControlManagerFacade facade = getFacade(delegate);
        final Set<Principal> principals = Collections.singleton(EveryonePrincipal.getInstance());
        final Privilege[] privileges = new Privilege[0];
        when(delegate.hasPrivileges("/foo", principals, privileges)).thenReturn(true);
        assertFalse("should not hasPrivileges", facade.hasPrivileges("", principals, privileges));
        assertTrue("should hasPrivileges", facade.hasPrivileges("/foo", principals, privileges));
    }

    @Test
    public void testGetPrivileges() throws Exception {
        JackrabbitAccessControlManager delegate = mock(JackrabbitAccessControlManager.class);
        JackrabbitAccessControlManagerFacade facade = getFacade(delegate);
        final Set<Principal> principals = Collections.singleton(EveryonePrincipal.getInstance());
        final Privilege[] privileges = new Privilege[0];
        when(delegate.getPrivileges("/foo", principals)).thenReturn(privileges);
        assertNull("should be null in wrong path", facade.getPrivileges("", principals));
        assertSame("should hasPrivileges", privileges, facade.getPrivileges("/foo", principals));
    }
}