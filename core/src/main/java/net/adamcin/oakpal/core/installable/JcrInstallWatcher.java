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
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.core.SilenceableViolationReporter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Session;
import java.io.Reader;
import java.util.Optional;

/**
 * Interface defining methods for collecting Sling JCR installable resources.
 */
@ProviderType
public interface JcrInstallWatcher extends SilenceableViolationReporter, ProgressCheck {

    /**
     * Get the collected installable paths. The consumer of this method is instructed to treat the returned map as an
     * iterable. Each entry gives the path as a key, and the installable type as the value. The type enum should be used
     * by the consumer to call the appropriate get method
     *
     * @return an iterable map of JCR paths to installable types
     */
    @Nullable PathInstallable dequeueInstallable();

    /**
     * Get installable repoinit scripts from the specified JCR path.
     *
     * @param session     the admin session
     * @param installable the installable
     * @return a list of reader suppliers from the provided JCR path
     */
    @NotNull Iterable<Fun.ThrowingSupplier<Reader>>
    openRepoInitInstallable(@NotNull RepoInitInstallable installable,
                            @NotNull Session session);

    /**
     * Get installable JCR package from the specified JCR path.
     *
     * @param installable    the subpackage installable
     * @param session        the admin session
     * @param packageManager the JCR package manager
     * @return an optional subpackage supplier
     */
    @NotNull Optional<Fun.ThrowingSupplier<JcrPackage>>
    openSubpackageInstallable(@NotNull SubpackageInstallable installable,
                              @NotNull Session session,
                              @NotNull JcrPackageManager packageManager);

}
