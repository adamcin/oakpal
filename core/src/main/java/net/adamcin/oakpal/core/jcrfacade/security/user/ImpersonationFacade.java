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

package net.adamcin.oakpal.core.jcrfacade.security.user;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import javax.security.auth.Subject;
import java.security.Principal;

public final class ImpersonationFacade implements Impersonation {
    private final @NotNull Impersonation delegate;

    @SuppressWarnings("WeakerAccess")
    public ImpersonationFacade(final @NotNull Impersonation delegate) {
        this.delegate = delegate;
    }

    @Override
    public PrincipalIterator getImpersonators() throws RepositoryException {
        return delegate.getImpersonators();
    }

    @Override
    public boolean allows(final Subject subject) throws RepositoryException {
        return delegate.allows(subject);
    }

    @Override
    public boolean grantImpersonation(final Principal principal) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public boolean revokeImpersonation(final Principal principal) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
