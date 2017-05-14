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

package net.adamcin.oakpal.core.jcrfacade;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.ListenerReadOnlyException;

/**
 * Wraps {@link NamespaceRegistry} to block namespace changes.
 */
public class NamespaceRegistryFacade implements NamespaceRegistry {
    private final NamespaceRegistry delegate;

    public NamespaceRegistryFacade(NamespaceRegistry delegate) {
        this.delegate = delegate;
    }

    @Override
    public void registerNamespace(String prefix, String uri) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void unregisterNamespace(String prefix) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public String[] getPrefixes() throws RepositoryException {
        return delegate.getPrefixes();
    }

    @Override
    public String[] getURIs() throws RepositoryException {
        return delegate.getURIs();
    }

    @Override
    public String getURI(String prefix) throws RepositoryException {
        return delegate.getURI(prefix);
    }

    @Override
    public String getPrefix(String uri) throws RepositoryException {
        return delegate.getPrefix(uri);
    }
}
