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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.jetbrains.annotations.Nullable;

/**
 * Wraps a {@link Repository} to prevent login.
 */
public final class RepositoryFacade implements Repository {

    private final @Nullable Repository delegate;

    @SuppressWarnings("WeakerAccess")
    public RepositoryFacade(final @Nullable Repository delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDescriptorKeys() {
        return delegate != null ? delegate.getDescriptorKeys() : new String[0];
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        return delegate != null && delegate.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        return delegate != null && delegate.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue(String key) {
        return delegate != null ? delegate.getDescriptorValue(key) : null;
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        return delegate != null ? delegate.getDescriptorValues(key) : null;
    }

    @Override
    public String getDescriptor(String key) {
        return delegate != null ? delegate.getDescriptor(key) : null;
    }

    @Override
    public Session login(Credentials credentials, String workspaceName) throws LoginException {
        throw new LoginException("Login not allowed through facade.");
    }

    @Override
    public Session login(Credentials credentials) throws LoginException {
        throw new LoginException("Login not allowed through facade.");
    }

    @Override
    public Session login(String workspaceName) throws LoginException {
        throw new LoginException("Login not allowed through facade.");
    }

    @Override
    public Session login() throws LoginException {
        throw new LoginException("Login not allowed through facade.");
    }
}
