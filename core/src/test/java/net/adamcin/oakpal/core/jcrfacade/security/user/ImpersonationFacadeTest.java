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

import javax.security.auth.Subject;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ImpersonationFacadeTest {

    ImpersonationFacade getFacade(final @NotNull Impersonation delegate) {
        return new ImpersonationFacade(delegate);
    }

    @Test
    public void testGetImpersonators() throws Exception {
        Impersonation delegate = mock(Impersonation.class);
        ImpersonationFacade facade = getFacade(delegate);
        final PrincipalIterator value = mock(PrincipalIterator.class);
        when(delegate.getImpersonators()).thenReturn(value);
        assertSame("is same value", value, facade.getImpersonators());
    }

    @Test
    public void testAllows() throws Exception {
        Impersonation delegate = mock(Impersonation.class);
        ImpersonationFacade facade = getFacade(delegate);
        final Subject subject = new Subject();
        when(delegate.allows(subject)).thenReturn(true);
        assertTrue("is true", facade.allows(subject));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROGrantImpersonation() throws Exception {
        Impersonation delegate = mock(Impersonation.class);
        ImpersonationFacade facade = getFacade(delegate);
        facade.grantImpersonation(null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORevokeImpersonation() throws Exception {
        Impersonation delegate = mock(Impersonation.class);
        ImpersonationFacade facade = getFacade(delegate);
        facade.revokeImpersonation(null);
    }
}