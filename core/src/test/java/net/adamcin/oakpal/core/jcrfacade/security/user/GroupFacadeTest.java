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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class GroupFacadeTest {

    GroupFacade getFacade(final @NotNull Group delegate) {
        return new GroupFacade(delegate);
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(Group.class, this::getFacade)
                .testFacadeIteratorGetter(Authorizable.class, AuthorizableFacade.class, Group::getDeclaredMembers)
                .testFacadeIteratorGetter(Authorizable.class, AuthorizableFacade.class, Group::getMembers);
    }

    @Test
    public void testIsDeclaredMember() throws Exception {
        Group delegate = mock(Group.class);
        GroupFacade facade = getFacade(delegate);
        final Authorizable someUser = mock(User.class);
        when(delegate.isDeclaredMember(someUser)).thenReturn(true);
        assertTrue("is true", facade.isDeclaredMember(someUser));
    }

    @Test
    public void testIsMember() throws Exception {
        Group delegate = mock(Group.class);
        GroupFacade facade = getFacade(delegate);
        final Authorizable someUser = mock(User.class);
        when(delegate.isMember(someUser)).thenReturn(true);
        assertTrue("is true", facade.isMember(someUser));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddMember() throws Exception {
        Group delegate = mock(Group.class);
        GroupFacade facade = getFacade(delegate);
        facade.addMember(null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddMembers() throws Exception {
        Group delegate = mock(Group.class);
        GroupFacade facade = getFacade(delegate);
        facade.addMembers("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveMember() throws Exception {
        Group delegate = mock(Group.class);
        GroupFacade facade = getFacade(delegate);
        facade.removeMember(null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveMembers() throws Exception {
        Group delegate = mock(Group.class);
        GroupFacade facade = getFacade(delegate);
        facade.removeMembers("");
    }
}