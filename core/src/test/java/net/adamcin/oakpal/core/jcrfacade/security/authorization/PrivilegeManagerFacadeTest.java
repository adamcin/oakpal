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

package net.adamcin.oakpal.core.jcrfacade.security.authorization;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.security.Privilege;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class PrivilegeManagerFacadeTest {

    PrivilegeManagerFacade getFacade(final @NotNull PrivilegeManager delegate) {
        return new PrivilegeManagerFacade(delegate);
    }

    @Test
    public void testGetRegisteredPrivilege() throws Exception {
        PrivilegeManager delegate = mock(PrivilegeManager.class);
        PrivilegeManagerFacade facade = getFacade(delegate);
        final Privilege[] value = new Privilege[0];
        when(delegate.getRegisteredPrivileges()).thenReturn(value);
        assertSame("is same value", value, facade.getRegisteredPrivileges());
    }

    @Test
    public void testGetPrivilege() throws Exception {
        PrivilegeManager delegate = mock(PrivilegeManager.class);
        PrivilegeManagerFacade facade = getFacade(delegate);
        final String name = "name";
        final Privilege value = mock(Privilege.class);
        when(delegate.getPrivilege(name)).thenReturn(value);
        assertSame("is same value", value, facade.getPrivilege(name));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORegisterPrivilege() throws Exception {
        PrivilegeManager delegate = mock(PrivilegeManager.class);
        PrivilegeManagerFacade facade = getFacade(delegate);
        facade.registerPrivilege("", true, new String[0]);
    }
}