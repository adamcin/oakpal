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
import java.util.Iterator;
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

    @NotNull
    @Override
    public Iterator<PathInstallable> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public @NotNull <EntityType> Iterable<Fun.ThrowingSupplier<EntityType>>
    open(@NotNull PathInstallable<EntityType> installable,
         @NotNull Session session,
         @NotNull JcrPackageManager packageManager) {
        return Collections.emptyList();
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
