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

package net.adamcin.oakpal.core.jcrfacade;

import static org.mockito.Mockito.mock;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.security.authorization.PrivilegeManagerFacade;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.xml.sax.InputSource;

public class JackrabbitWorkspaceFacadeTest {

    JackrabbitWorkspaceFacade<JackrabbitSession> getFacade(final @NotNull JackrabbitWorkspace delegate) {
        return new JackrabbitWorkspaceFacade<>(delegate, new JackrabbitSessionFacade(mock(JackrabbitSession.class), true));
    }

    @Test
    public void testGetPrivilegeManager() throws Exception {
        new FacadeGetterMapping.Tester<>(JackrabbitWorkspace.class, this::getFacade)
                .testFacadeGetter(PrivilegeManager.class, PrivilegeManagerFacade.class, JackrabbitWorkspace::getPrivilegeManager);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testCreateWorkspace() throws Exception {
        JackrabbitWorkspaceFacade<JackrabbitSession> facade = getFacade(mock(JackrabbitWorkspace.class));
        facade.createWorkspace("", (InputSource) null);
    }
}