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

package net.adamcin.oakpal.core.jcrfacade.version;

import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import net.adamcin.oakpal.core.jcrfacade.RangeIteratorFacade;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;

/**
 * Wraps {@link VersionIterator} to return {@link VersionFacade}-wrapped versions.
 */
public class VersionIteratorFacade extends RangeIteratorFacade<VersionIterator> implements VersionIterator {

    private final SessionFacade session;

    public VersionIteratorFacade(VersionIterator delegate, SessionFacade session) {
        super(delegate);
        this.session = session;
    }

    @Override
    public Version nextVersion() {
        Version internalVersion = delegate.nextVersion();
        return new VersionFacade<>(internalVersion, session);
    }

    @Override
    public Object next() {
        return nextVersion();
    }
}
