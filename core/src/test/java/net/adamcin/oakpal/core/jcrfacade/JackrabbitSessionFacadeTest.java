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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.jcrfacade.security.user.UserManagerFacade;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class JackrabbitSessionFacadeTest {

    /**
     * Functional interface for testing.
     */
    @FunctionalInterface
    public interface JackrabbitInspectBody<E extends Throwable> {
        void tryAccept(final JackrabbitSession session) throws E;
    }

    <E extends Throwable> void
    inspectForTest(final @NotNull JackrabbitInspectBody<E> body)
            throws AbortedScanException, RepositoryException, E {
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            assertTrue("should be JackrabbitSession", session instanceof JackrabbitSession);
            body.tryAccept((JackrabbitSession) session);
        });
    }

    @Test
    public void testHasPermission() throws Exception {
        inspectForTest(session -> {
            final JackrabbitSessionFacade facade = new JackrabbitSessionFacade(session, true);
            assertTrue("admin should have read on root",
                    facade.hasPermission("/", Session.ACTION_READ));
            assertTrue("admin should have read and set_property on root",
                    facade.hasPermission("/", Session.ACTION_READ, Session.ACTION_SET_PROPERTY));
        });
    }

    @Test
    public void testGetPrincipalManager() throws Exception {
        inspectForTest(session -> {
            final JackrabbitSessionFacade facade = new JackrabbitSessionFacade(session, true);
            assertNotNull("getPrincipalManager should not be null", facade.getPrincipalManager());
        });
    }

    @Test
    public void testGetUserManager() throws Exception {
        new FacadeGetterMapping.Tester<>(JackrabbitSession.class, session -> new JackrabbitSessionFacade(session, true))
                .testFacadeGetter(UserManager.class, UserManagerFacade.class, JackrabbitSession::getUserManager);
    }

    @Test
    public void testGetItemOrNull() throws Exception {
        inspectForTest(session -> {
            final JackrabbitSessionFacade facade = new JackrabbitSessionFacade(session, true);
            assertTrue("root node should be same as /",
                    facade.getRootNode().isSame(facade.getItemOrNull("/")));
            assertNull("/foo node should be null", facade.getItemOrNull("/foo"));
        });
    }

    @Test
    public void testGetPropertyOrNull() throws Exception {
        inspectForTest(session -> {
            final JackrabbitSessionFacade facade = new JackrabbitSessionFacade(session, true);
            assertNull("property should be null", facade.getPropertyOrNull("/jcr:system/someProp"));
            final Node fooNode = session.getRootNode().addNode("foo", JcrConstants.NT_UNSTRUCTURED);
            fooNode.setProperty("someProp", "someVal");
            assertEquals("someProp should have someVal value",
                    "someVal", facade.getPropertyOrNull("/foo/someProp").getString());
        });
    }

    @Test
    public void testGetNodeOrNull() throws Exception {
        inspectForTest(session -> {
            final JackrabbitSessionFacade facade = new JackrabbitSessionFacade(session, true);
            assertTrue("root node should be same as /",
                    facade.getRootNode().isSame(facade.getNodeOrNull("/")));
            assertNull("/foo node should be null", facade.getNodeOrNull("/foo"));
        });
    }
}