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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import net.adamcin.oakpal.core.jcrfacade.NodeIteratorFacade;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps {@link VersionHistory} to prevent writes and to ensure only facades are returned.
 */
public final class VersionHistoryFacade<S extends Session> extends NodeFacade<VersionHistory, S>
        implements VersionHistory {

    public VersionHistoryFacade(final @NotNull VersionHistory delegate, final @NotNull SessionFacade<S> session) {
        super(delegate, session);
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getVersionableUUID() throws RepositoryException {
        return delegate.getVersionableUUID();
    }

    @Override
    public String getVersionableIdentifier() throws RepositoryException {
        return delegate.getVersionableIdentifier();
    }

    @Override
    public boolean hasVersionLabel(String label) throws RepositoryException {
        return delegate.hasVersionLabel(label);
    }

    @Override
    public boolean hasVersionLabel(Version version, String label) throws RepositoryException {
        return delegate.hasVersionLabel(version, label);
    }

    @Override
    public String[] getVersionLabels() throws RepositoryException {
        return delegate.getVersionLabels();
    }

    @Override
    public String[] getVersionLabels(Version version) throws RepositoryException {
        return delegate.getVersionLabels(version);
    }

    @Override
    public Version getRootVersion() throws RepositoryException {
        Version internal = delegate.getRootVersion();
        return new VersionFacade<>(internal, session);
    }

    @Override
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        VersionIterator internal = delegate.getAllLinearVersions();
        return new VersionIteratorFacade<>(internal, session);
    }

    @Override
    public VersionIterator getAllVersions() throws RepositoryException {
        VersionIterator internal = delegate.getAllVersions();
        return new VersionIteratorFacade<>(internal, session);
    }

    @Override
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        NodeIterator internal = delegate.getAllLinearFrozenNodes();
        return new NodeIteratorFacade<>(internal, session);
    }

    @Override
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        NodeIterator internal = delegate.getAllFrozenNodes();
        return new NodeIteratorFacade<>(internal, session);
    }

    @Override
    public Version getVersion(String versionName) throws RepositoryException {
        Version internal = delegate.getVersion(versionName);
        return new VersionFacade<>(internal, session);
    }

    @Override
    public Version getVersionByLabel(String label) throws RepositoryException {
        Version internal = delegate.getVersionByLabel(label);
        return new VersionFacade<>(internal, session);
    }

    @Override
    public void addVersionLabel(String versionName, String label, boolean moveLabel) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void removeVersionLabel(String label) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void removeVersion(String versionName) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
