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

package net.adamcin.opal.core.jcrfacade;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Wraps a {@link Repository} to prevent login.
 */
public class RepositoryFacade implements Repository {

    private final Repository delegate;

    public RepositoryFacade(Repository delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDescriptorKeys() {
        return delegate.getDescriptorKeys();
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        return delegate.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        return delegate.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue(String key) {
        return delegate.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        return delegate.getDescriptorValues(key);
    }

    @Override
    public String getDescriptor(String key) {
        return delegate.getDescriptor(key);
    }

    @Override
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        throw new LoginException("Login not allowed through facade.");
    }

    @Override
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        throw new LoginException("Login not allowed through facade.");
    }

    @Override
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        throw new LoginException("Login not allowed through facade.");
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        throw new LoginException("Login not allowed through facade.");
    }
}
