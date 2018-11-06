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

import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;

/**
 * Wraps {@link Version} to prevent writes.
 */
public class VersionFacade<V extends Version> extends NodeFacade<V> implements Version {

    public VersionFacade(V delegate, SessionFacade session) {
        super(delegate, session);
    }

    @Override
    public VersionHistory getContainingHistory() throws RepositoryException {
        VersionHistory internalHistory = delegate.getContainingHistory();
        return new VersionHistoryFacade<>(internalHistory, session);
    }

    @Override
    public Calendar getCreated() throws RepositoryException {
        return delegate.getCreated();
    }

    @Override
    public Version getLinearSuccessor() throws RepositoryException {
        Version internalVersion = delegate.getLinearSuccessor();
        return new VersionFacade<>(internalVersion, session);
    }

    @Override
    public Version[] getSuccessors() throws RepositoryException {
        Version[] internalVersions = delegate.getSuccessors();
        if (internalVersions == null) {
            return null;
        }
        for (int i = 0; i < internalVersions.length; i++) {
            internalVersions[i] = new VersionFacade<>(internalVersions[i], session);
        }
        return internalVersions;
    }

    @Override
    public Version getLinearPredecessor() throws RepositoryException {
        Version internalVersion = delegate.getLinearPredecessor();
        return new VersionFacade<>(internalVersion, session);
    }

    @Override
    public Version[] getPredecessors() throws RepositoryException {
        Version[] internalVersions = delegate.getPredecessors();
        if (internalVersions == null) {
            return null;
        }
        for (int i = 0; i < internalVersions.length; i++) {
            internalVersions[i] = new VersionFacade<>(internalVersions[i], session);
        }
        return internalVersions;
    }

    @Override
    public Node getFrozenNode() throws RepositoryException {
        Node internalNode = delegate.getFrozenNode();
        return new NodeFacade<>(internalNode, session);
    }
}
