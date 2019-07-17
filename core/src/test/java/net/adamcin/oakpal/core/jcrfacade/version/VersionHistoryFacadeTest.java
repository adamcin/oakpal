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

package net.adamcin.oakpal.core.jcrfacade.version;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeIteratorFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class VersionHistoryFacadeTest {

    VersionHistoryFacade<Session> getFacade(final @NotNull VersionHistory delegate) {
        return new VersionHistoryFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(VersionHistory.class, this::getFacade)
                .testFacadeGetter(Version.class, VersionFacade.class, VersionHistory::getRootVersion)
                .testFacadeGetter(Version.class, VersionFacade.class, delegate -> delegate.getVersion(""))
                .testFacadeGetter(Version.class, VersionFacade.class, delegate -> delegate.getVersionByLabel(""))
                .testFacadeGetter(VersionIterator.class, VersionIteratorFacade.class, VersionHistory::getAllLinearVersions)
                .testFacadeGetter(VersionIterator.class, VersionIteratorFacade.class, VersionHistory::getAllVersions)
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, VersionHistory::getAllLinearFrozenNodes)
                .testFacadeGetter(NodeIterator.class, NodeIteratorFacade.class, VersionHistory::getAllFrozenNodes);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetVersionableUUID() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        final String value = "expected";
        when(delegate.getVersionableUUID()).thenReturn(value);
        assertSame("is same value", value, facade.getVersionableUUID());
    }

    @Test
    public void testGetVersionableIdentifier() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        final String value = "expected";
        when(delegate.getVersionableIdentifier()).thenReturn(value);
        assertSame("is same value", value, facade.getVersionableIdentifier());
    }

    @Test
    public void testHasVersionLabel1() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        final String arg = "arg";
        when(delegate.hasVersionLabel(arg)).thenReturn(true);
        assertTrue("is true", facade.hasVersionLabel(arg));
    }

    @Test
    public void testHasVersionLabel2() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        final Version version = mock(Version.class);
        final String arg = "arg";
        when(delegate.hasVersionLabel(version, arg)).thenReturn(true);
        assertTrue("is true", facade.hasVersionLabel(version, arg));
    }

    @Test
    public void testGetVersionLabels1() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        when(delegate.getVersionLabels()).thenReturn(value);
        assertSame("is same value", value, facade.getVersionLabels());
    }

    @Test
    public void testGetVersionLabels2() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        final String[] value = new String[0];
        final Version version = mock(Version.class);
        when(delegate.getVersionLabels(version)).thenReturn(value);
        assertSame("is same value", value, facade.getVersionLabels(version));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddVersionLabel() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        facade.addVersionLabel("", "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveVersionLabel() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        facade.removeVersionLabel("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveVersion() throws Exception {
        VersionHistory delegate = mock(VersionHistory.class);
        VersionHistoryFacade<Session> facade = getFacade(delegate);
        facade.removeVersion("");
    }
}