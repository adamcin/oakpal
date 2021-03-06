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
import net.adamcin.oakpal.api.ScanListener;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingOpenable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.core.ErrorListener;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Session;

/**
 * Interface defining internal-facing behavior for the SlingSimulator, primarily regarding the retrieval and processing
 * of Sling JCR installable resources submitted by ProgressChecks.
 */
@ProviderType
public interface SlingSimulatorBackend extends SlingSimulator, ScanListener {

    /**
     * Provide a JCR session.
     *
     * @param session the jcr session
     */
    void setSession(Session session);

    /**
     * Provide a JCR package manager.
     *
     * @param packageManager the jcr package manager
     */
    void setPackageManager(JcrPackageManager packageManager);

    /**
     * Provide an ErrorListener.
     *
     * @param errorListener the error listener
     */
    void setErrorListener(ErrorListener errorListener);

    /**
     * Get the next installable path and remove from the queue.
     *
     * @return a resource to be installed immediately or null to continue the scan
     */
    @Nullable SlingInstallable dequeueInstallable();

    /**
     * Return a supplier that tries to open an installable resource from the specified installable path.
     *
     * @param installable    the openable installable
     * @param <ResourceType> the concrete resource type
     * @return an entity supplier from the provided JCR path
     */
    @NotNull <ResourceType> Fun.ThrowingSupplier<ResourceType>
    open(@NotNull SlingOpenable<ResourceType> installable);
}
