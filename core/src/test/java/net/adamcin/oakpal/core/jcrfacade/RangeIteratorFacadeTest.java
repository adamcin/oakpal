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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import javax.jcr.RangeIterator;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class RangeIteratorFacadeTest {

    RangeIteratorFacade<RangeIterator> getFacade(final @NotNull RangeIterator delegate) {
        return new RangeIteratorFacade<>(delegate);
    }

    @Test
    public void testSkip() {
        RangeIterator delegate = mock(RangeIterator.class);
        RangeIteratorFacade<RangeIterator> facade = getFacade(delegate);
        CompletableFuture<Long> latch = new CompletableFuture<>();
        doAnswer(invoked -> latch.complete(invoked.getArgument(0))).when(delegate).skip(anyLong());
        final long expected = 7L;
        assertFalse("should not be done", latch.isDone());
        facade.skip(expected);
        assertEquals("should be equal", (Long) expected, latch.getNow(-1L));
    }

    @Test
    public void testGetSize() {
        RangeIterator delegate = mock(RangeIterator.class);
        RangeIteratorFacade<RangeIterator> facade = getFacade(delegate);
        final long size = 5L;
        when(delegate.getSize()).thenReturn(size);
        assertEquals("should be equal", size, facade.getSize());
    }

    @Test
    public void testGetPosition() {
        RangeIterator delegate = mock(RangeIterator.class);
        RangeIteratorFacade<RangeIterator> facade = getFacade(delegate);
        final long position = 10L;
        when(delegate.getPosition()).thenReturn(position);
        assertEquals("should be equal", position, facade.getPosition());
    }

    @Test
    public void testHasNext() {
        RangeIterator delegate = mock(RangeIterator.class);
        RangeIteratorFacade<RangeIterator> facade = getFacade(delegate);
        when(delegate.hasNext()).thenReturn(true);
        assertTrue("should be true", facade.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNextThrows() {
        RangeIterator delegate = mock(RangeIterator.class);
        RangeIteratorFacade<RangeIterator> facade = getFacade(delegate);
        doThrow(new IllegalArgumentException("base facade class illegally called delegate next() method")).when(delegate).next();
        facade.next();
    }
}