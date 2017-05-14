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

package net.adamcin.oakpal.core.jcrfacade.version;

import javax.jcr.AccessDeniedException;
import javax.jcr.NodeIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.LabelExistsVersionException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import net.adamcin.oakpal.core.jcrfacade.NodeFacade;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;

/**
 * Wraps {@link VersionHistory} to prevent writes and to ensure only facades are returned.
 */
public class VersionHistoryFacade<H extends VersionHistory> extends NodeFacade<H> implements VersionHistory {

    public VersionHistoryFacade(H delegate, SessionFacade session) {
        super(delegate, session);
    }

    @Override
    public String getVersionableUUID() throws RepositoryException {
        return delegate.getVersionableUUID();
    }

    @Override
    public String getVersionableIdentifier() throws RepositoryException {
        return delegate.getVersionableIdentifier();
    }

    @Override
    public Version getRootVersion() throws RepositoryException {
        return null;
    }

    @Override
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        return null;
    }

    @Override
    public VersionIterator getAllVersions() throws RepositoryException {
        return null;
    }

    @Override
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        return null;
    }

    @Override
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        return null;
    }

    @Override
    public Version getVersion(String versionName) throws VersionException, RepositoryException {
        return null;
    }

    @Override
    public Version getVersionByLabel(String label) throws VersionException, RepositoryException {
        return null;
    }

    @Override
    public void addVersionLabel(String versionName, String label, boolean moveLabel) throws LabelExistsVersionException, VersionException, RepositoryException {

    }

    @Override
    public void removeVersionLabel(String label) throws VersionException, RepositoryException {

    }

    @Override
    public boolean hasVersionLabel(String label) throws RepositoryException {
        return false;
    }

    @Override
    public boolean hasVersionLabel(Version version, String label) throws VersionException, RepositoryException {
        return false;
    }

    @Override
    public String[] getVersionLabels() throws RepositoryException {
        return new String[0];
    }

    @Override
    public String[] getVersionLabels(Version version) throws VersionException, RepositoryException {
        return new String[0];
    }

    @Override
    public void removeVersion(String versionName) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {

    }
}
