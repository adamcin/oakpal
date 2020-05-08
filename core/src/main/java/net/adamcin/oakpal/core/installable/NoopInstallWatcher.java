/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.core.installable;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Session;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * NOOP implementation of an JCR Install Watcher.
 */
public final class NoopInstallWatcher implements JcrInstallWatcher {
    private static final JcrInstallWatcher INSTANCE = new NoopInstallWatcher();

    public static JcrInstallWatcher instance() {
        return INSTANCE;
    }

    /**
     * Private constructor.
     */
    private NoopInstallWatcher() {
        /* no construct */
    }

    @Override
    public String getCheckName() {
        return NoopInstallWatcher.class.getSimpleName();
    }

    @Override
    public @Nullable PathInstallable dequeueInstallable() {
        return null;
    }

    @Override
    public @NotNull Iterable<Fun.ThrowingSupplier<Reader>>
    openRepoInitInstallable(@NotNull final RepoInitInstallable installable,
                            @NotNull final Session session) {
        return Collections.emptyList();
    }

    @Override
    public @NotNull Optional<Fun.ThrowingSupplier<JcrPackage>>
    openSubpackageInstallable(@NotNull final SubpackageInstallable installable,
                              @NotNull final Session session,
                              @NotNull final JcrPackageManager packageManager) {
        return Optional.empty();
    }

    @Override
    public void setSilenced(final boolean isSilenced) {
        /* do nothing */
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return Collections.emptyList();
    }
}
