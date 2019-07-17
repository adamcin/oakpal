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

package net.adamcin.oakpal.core.jcrfacade.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import net.adamcin.oakpal.core.jcrfacade.FacadeGetterMapping;
import net.adamcin.oakpal.core.jcrfacade.JcrSessionFacade;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class RowIteratorFacadeTest {

    RowIteratorFacade<Session> getFacade(final @NotNull RowIterator delegate) {
        return new RowIteratorFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testNextRow() throws Exception {
        new FacadeGetterMapping.Tester<>(RowIterator.class, this::getFacade)
                .testFacadeGetter(Row.class, RowFacade.class, RowIterator::nextRow);
    }

    @Test
    public void testNext() throws Exception {
        RowIterator delegate = mock(RowIterator.class);
        RowIteratorFacade<Session> facade = getFacade(delegate);
        final Row value = mock(Row.class);
        final String path = "/correct/path";
        when(value.getPath()).thenReturn(path);
        when(delegate.nextRow()).thenReturn(value);
        final Object fromFacade = facade.next();
        assertTrue("is node", fromFacade instanceof Row);
        final Row nodeFromFacade = (Row) fromFacade;
        assertEquals("same path", path, nodeFromFacade.getPath());
        assertTrue("is facade", nodeFromFacade instanceof RowFacade);
    }
}