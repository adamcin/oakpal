/*
 * Copyright 2017 Mark Adamcin
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

package net.adamcin.opal.core.jcrfacade.observation;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import net.adamcin.opal.core.HandlerReadOnlyException;

/**
 * Wraps the {@link ObservationManager} to prevent changes and calls between listeners.
 */
public class ObservationManagerFacade implements ObservationManager {
    private static final EventListenerIterator EMPTY_LISTENER_ITERATOR =
            new EmptyEventListenerIterator();

    private final ObservationManager delegate;

    public ObservationManagerFacade(ObservationManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addEventListener(EventListener listener, int eventTypes, String absPath,
                                 boolean isDeep, String[] uuid, String[] nodeTypeName,
                                 boolean noLocal)
            throws RepositoryException {
        throw new HandlerReadOnlyException();
    }

    @Override
    public void removeEventListener(EventListener listener) throws RepositoryException {
        throw new HandlerReadOnlyException();
    }

    @Override
    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        return EMPTY_LISTENER_ITERATOR;
    }

    @Override
    public void setUserData(String userData) throws RepositoryException { }

    @Override
    public EventJournal getEventJournal() throws RepositoryException {
        return delegate.getEventJournal();
    }

    @Override
    public EventJournal getEventJournal(int eventTypes, String absPath, boolean isDeep,
                                        String[] uuid, String[] nodeTypeName)
            throws RepositoryException {
        return delegate.getEventJournal(eventTypes, absPath, isDeep, uuid, nodeTypeName);
    }
}
