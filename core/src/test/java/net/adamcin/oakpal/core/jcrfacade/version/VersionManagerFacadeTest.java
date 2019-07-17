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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class VersionManagerFacadeTest {

    VersionManagerFacade<Session> getFacade(final @NotNull VersionManager mockVersionManager) {
        return new VersionManagerFacade<>(mockVersionManager, new JcrSessionFacade(mock(Session.class), false));
    }

    @Test
    public void testIsCheckedOut() throws Exception {
        VersionManager delegate = mock(VersionManager.class);
        VersionManagerFacade<Session> facade = getFacade(delegate);
        final String path = "/correct/path";
        when(delegate.isCheckedOut(anyString())).thenReturn(false);
        when(delegate.isCheckedOut(path)).thenReturn(true);
        assertTrue("is true", facade.isCheckedOut(path));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(VersionManager.class, this::getFacade)
                .testFacadeGetter(VersionHistory.class, VersionHistoryFacade.class, delegate -> delegate.getVersionHistory("/path"))
                .testFacadeGetter(Version.class, VersionFacade.class, delegate -> delegate.getBaseVersion("/path"))
                .testFacadeGetter(Node.class, NodeFacade.class, VersionManager::getActivity);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCheckin() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.checkin("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCheckout() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.checkout("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCheckpoint() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.checkpoint("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreVersions() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.restore(new Version[0], true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreStringString() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.restore("", "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreVersion() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.restore((Version) null, true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreStringVersion() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.restore("", (Version) null, true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORestoreByLabel() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.restoreByLabel("", "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROMerge3() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.merge("", "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROMerge4() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.merge("", "", true, true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROMergeNode() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.merge((Node) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRODoneMerge() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.doneMerge("", (Version) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCancelMerge() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.cancelMerge("", (Version) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateConfiguration() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.createConfiguration("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetActivity() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.setActivity((Node) null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROCreateActivity() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.createActivity("");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveActivity() throws Exception {
        VersionManagerFacade<Session> facade = getFacade(mock(VersionManager.class));
        facade.removeActivity((Node) null);
    }
}