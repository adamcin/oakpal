/*
 * Copyright 2018 Mark Adamcin
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

import java.util.NoSuchElementException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

/**
 * Just an empty implementation.
 */
public final class EmptyEventListenerIterator implements EventListenerIterator {

    @Override
    public EventListener nextEventListener() {
        throw new NoSuchElementException();
    }

    @Override
    public void skip(long skipNum) {
        if (skipNum > 0L) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public long getPosition() {
        return 0;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Object next() {
        return nextEventListener();
    }
}
