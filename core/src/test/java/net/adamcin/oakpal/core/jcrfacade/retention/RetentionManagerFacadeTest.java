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

package net.adamcin.oakpal.core.jcrfacade.retention;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.retention.Hold;
import javax.jcr.retention.RetentionManager;
import javax.jcr.retention.RetentionPolicy;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class RetentionManagerFacadeTest {

    RetentionManagerFacade getFacade(final @NotNull RetentionManager mockManager) {
        return new RetentionManagerFacade(mockManager);
    }

    @Test
    public void testGetHolds() throws Exception {
        RetentionManager delegate = mock(RetentionManager.class);
        RetentionManagerFacade facade = getFacade(delegate);
        final String path = "/correct/path";
        final Hold[] value = new Hold[0];
        when(delegate.getHolds(path)).thenReturn(value);
        assertSame("same value", value, facade.getHolds(path));
    }

    @Test
    public void testGetRetentionPolicy() throws Exception {
        RetentionManager delegate = mock(RetentionManager.class);
        RetentionManagerFacade facade = getFacade(delegate);
        final String path = "/correct/path";
        final RetentionPolicy value = mock(RetentionPolicy.class);
        when(delegate.getRetentionPolicy(path)).thenReturn(value);
        assertSame("same value", value, facade.getRetentionPolicy(path));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROAddHold() throws Exception {
        RetentionManagerFacade facade = getFacade(mock(RetentionManager.class));
        facade.addHold("", "", true);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveHold() throws Exception {
        RetentionManagerFacade facade = getFacade(mock(RetentionManager.class));
        facade.removeHold("", null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROSetRetentionPolicy() throws Exception {
        RetentionManagerFacade facade = getFacade(mock(RetentionManager.class));
        facade.setRetentionPolicy("", null);
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORemoveRetentionPolicy() throws Exception {
        RetentionManagerFacade facade = getFacade(mock(RetentionManager.class));
        facade.removeRetentionPolicy("");
    }
}