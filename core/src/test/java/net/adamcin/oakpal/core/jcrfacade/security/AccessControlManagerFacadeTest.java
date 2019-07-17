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

import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class AccessControlManagerFacadeTest {

    AccessControlManagerFacade<AccessControlManager> getFacade(final @NotNull AccessControlManager delegate) {
        return new AccessControlManagerFacade<>(delegate);
    }

    @Test
    public void testFindBestWrapper() {
        assertNull("just null", AccessControlManagerFacade.findBestWrapper(null));
        AccessControlManager jcrDelegate = mock(AccessControlManager.class);
        AccessControlManager jcrFacade = AccessControlManagerFacade.findBestWrapper(jcrDelegate);
        assertTrue("is facade", jcrFacade instanceof AccessControlManagerFacade);
        assertFalse("is not jackrabbit facade", jcrFacade instanceof JackrabbitAccessControlManagerFacade);
        AccessControlManager jackDelegate = mock(JackrabbitAccessControlManager.class);
        AccessControlManager jackFacade = AccessControlManagerFacade.findBestWrapper(jackDelegate);
        assertTrue("is jackrabbit facade", jackFacade instanceof JackrabbitAccessControlManagerFacade);
    }

    @Test
    public void testGetSupportedPrivileges() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final Privilege[] value = new Privilege[0];
        final String path = "/correct/path";
        when(delegate.getSupportedPrivileges(path)).thenReturn(value);
        assertSame("is same value", value, facade.getSupportedPrivileges(path));
    }

    @Test
    public void testGetPrivileges() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final Privilege[] value = new Privilege[0];
        final String path = "/correct/path";
        when(delegate.getPrivileges(path)).thenReturn(value);
        assertSame("is same value", value, facade.getPrivileges(path));

    }

    @Test
    public void testPrivilegeFromName() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final Privilege value = mock(Privilege.class);
        final String name = "name";
        when(delegate.privilegeFromName(name)).thenReturn(value);
        assertSame("is same value", value, facade.privilegeFromName(name));
    }

    @Test
    public void testHasPrivileges() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final Privilege[] value = new Privilege[0];
        final String path = "/correct/path";
        when(delegate.hasPrivileges(path, value)).thenReturn(true);
        assertTrue("is true", facade.hasPrivileges(path, value));
    }

    @Test
    public void testGetPolicies() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final AccessControlPolicy[] value = new AccessControlPolicy[0];
        final String path = "/correct/path";
        when(delegate.getPolicies(path)).thenReturn(value);
        assertSame("is same value", value, facade.getPolicies(path));
    }

    @Test
    public void testGetEffectivePolicies() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final AccessControlPolicy[] value = new AccessControlPolicy[0];
        final String path = "/correct/path";
        when(delegate.getEffectivePolicies(path)).thenReturn(value);
        assertSame("is same value", value, facade.getEffectivePolicies(path));
    }

    @Test
    public void testGetApplicablePolicies() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        final AccessControlPolicyIterator value = mock(AccessControlPolicyIterator.class);
        final String path = "/correct/path";
        when(delegate.getApplicablePolicies(path)).thenReturn(value);
        assertSame("is same value", value, facade.getApplicablePolicies(path));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetPolicy() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        facade.setPolicy("", null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemovePolicy() throws Exception {
        AccessControlManager delegate = mock(AccessControlManager.class);
        AccessControlManagerFacade<AccessControlManager> facade = getFacade(delegate);
        facade.removePolicy("", null);
    }
}