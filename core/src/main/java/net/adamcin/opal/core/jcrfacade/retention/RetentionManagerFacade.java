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

package net.adamcin.opal.core.jcrfacade.retention;

import javax.jcr.RepositoryException;
import javax.jcr.retention.Hold;
import javax.jcr.retention.RetentionManager;
import javax.jcr.retention.RetentionPolicy;

import net.adamcin.opal.core.HandlerReadOnlyException;

/**
 * Wraps {@link RetentionManager} to prevent writes by handlers.
 */
public class RetentionManagerFacade implements RetentionManager {
    private final RetentionManager delegate;

    public RetentionManagerFacade(RetentionManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Hold[] getHolds(String absPath) throws RepositoryException {
        return delegate.getHolds(absPath);
    }

    @Override
    public Hold addHold(String absPath, String name, boolean isDeep) throws RepositoryException {
        throw new HandlerReadOnlyException();
    }

    @Override
    public void removeHold(String absPath, Hold hold) throws RepositoryException {
        throw new HandlerReadOnlyException();
    }

    @Override
    public RetentionPolicy getRetentionPolicy(String absPath) throws RepositoryException {
        return delegate.getRetentionPolicy(absPath);
    }

    @Override
    public void setRetentionPolicy(String absPath, RetentionPolicy retentionPolicy) throws RepositoryException {
        throw new HandlerReadOnlyException();
    }

    @Override
    public void removeRetentionPolicy(String absPath) throws RepositoryException {
        throw new HandlerReadOnlyException();
    }
}
