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

package net.adamcin.oakpal.core.jcrfacade;

import java.util.Iterator;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for wrapping subtypes of {@link RangeIterator}.
 */
public class RangeIteratorFacade<R extends RangeIterator> implements RangeIterator {

    protected final @NotNull R delegate;

    public RangeIteratorFacade(final @NotNull R delegate) {
        this.delegate = delegate;
    }

    @Override
    public final void skip(long skipNum) {
        delegate.skip(skipNum);
    }

    @Override
    public final long getSize() {
        return delegate.getSize();
    }

    @Override
    public final long getPosition() {
        return delegate.getPosition();
    }

    @Override
    public final boolean hasNext() {
        return delegate.hasNext();
    }

    /**
     * This base implementation throws an error when called, because extending classes must override it with logic that
     * wraps each element with an appropriate facade class. And since this is a non-parameterized implementation of
     * {@link Iterator#next()}, it is more likely to be cleaner to simply delegate to the unique parallel {@code nextType}
     * method defined for each {@link RangeIterator} subinterface, such as {@link PropertyIterator#nextProperty()} or
     * {@link NodeIterator#nextNode()}, instead of the reverse.
     *
     * @return a theoretical next element
     * @throws UnsupportedOperationException always.
     */
    @Override
    public Object next() {
        throw new UnsupportedOperationException("override this method to ensure delegate elements are wrapped appropriately with facades.");
    }
}
