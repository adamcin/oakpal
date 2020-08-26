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

package net.adamcin.oakpal.core.sling;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.core.ErrorListener;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Noop implementation of a SlingSimulator.
 */
public final class NoopSlingSimulator implements SlingSimulatorBackend, SlingSimulator {
    public static SlingSimulatorBackend instance() {
        return new NoopSlingSimulator();
    }

    @Override
    public void setSession(final Session session) {
        /* do nothing */
    }

    @Override
    public void setPackageManager(final JcrPackageManager packageManager) {
        /* do nothing */
    }

    @Override
    public void setErrorListener(final ErrorListener errorListener) {
        /* do nothing */
    }

    @Override
    public @Nullable SlingInstallable<?> dequeueInstallable() {
        return null;
    }

    @Override
    public @NotNull <InstallableType> Fun.ThrowingSupplier<InstallableType> open(@NotNull final SlingInstallable<InstallableType> installable) {
        return () -> {
            throw new IllegalStateException("Cannot install sling resources using using noop simulator.");
        };
    }

    @Override
    public @Nullable SlingInstallable<?> prepareInstallableNode(final @NotNull PackageId parentPackageId,
                                                                final @NotNull Node node) {
        return null;
    }

}
