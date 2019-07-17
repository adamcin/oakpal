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

package net.adamcin.oakpal.core.jcrfacade.observation;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.junit.Test;

public class EmptyEventListenerIteratorTest {

    @Test(expected = NoSuchElementException.class)
    public void testNext() {
        new EmptyEventListenerIterator().next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextEventListener() {
        new EmptyEventListenerIterator().nextEventListener();
    }

    @Test
    public void testZeros() {
        final EmptyEventListenerIterator iterator = new EmptyEventListenerIterator();
        iterator.skip(0L);
        assertEquals("position should be zero", 0L, iterator.getPosition());
        assertEquals("size should be zero", 0L, iterator.getSize());
    }

    @Test(expected = NoSuchElementException.class)
    public void testSkipNonZero() {
        new EmptyEventListenerIterator().skip(1L);
    }

    @Test
    public void testHasNext() {
        assertFalse("should never have next",
                new EmptyEventListenerIterator().hasNext());
    }
}