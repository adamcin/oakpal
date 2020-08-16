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
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

import javax.jcr.Session;

/**
 * Interface defining methods for collecting Sling JCR installable resources.
 */
@ConsumerType
public interface JcrInstallWatcher extends SilenceableViolationReporter, ProgressCheck, Iterable<PathInstallable<?>> {

    /**
     * Get installable entities from the specified JCR path.
     *
     * @param session     the admin session
     * @param installable the installable
     * @param packageManager the JCR package manager
     * @return a list of entity suppliers from the provided JCR path
     */
    @NotNull <EntityType> Iterable<Fun.ThrowingSupplier<EntityType>>
    open(@NotNull PathInstallable<EntityType> installable,
                            @NotNull Session session,
                            @NotNull JcrPackageManager packageManager);


}
