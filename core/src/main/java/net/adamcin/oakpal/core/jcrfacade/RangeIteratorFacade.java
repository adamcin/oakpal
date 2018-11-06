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

import javax.jcr.RangeIterator;

/**
 * Base class for wrapping subtypes of {@link RangeIterator}.
 */
public abstract class RangeIteratorFacade<R extends RangeIterator> implements RangeIterator {

    protected final R delegate;

    public RangeIteratorFacade(R delegate) {
        this.delegate = delegate;
    }

    @Override
    public void skip(long skipNum) {
        delegate.skip(skipNum);
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public long getPosition() {
        return delegate.getPosition();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

}
