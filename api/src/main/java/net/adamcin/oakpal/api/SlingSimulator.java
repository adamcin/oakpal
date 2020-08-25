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

package net.adamcin.oakpal.api;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;

/**
 * Simulates aspects of the Sling runtime.
 */
@ProviderType
public interface SlingSimulator {

    /**
     * Submit resource path for installation as an embedded FileVault package, generally located under {@code /apps/}.
     * This should not be used to request traditional installation of {@code subpackages} under {@code /etc/packages}.
     * <p>
     * Ideally, this method should be called by a check during
     * {@link ProgressCheck#importedPath(PackageId, String, Node, PathAction)}.
     *
     * @param parentPackageId the parent package ID (not the embedded package ID)
     * @param node            the JCR node of the embedded JCR package
     * @return a handle for the installable path or null
     */
    @Nullable SlingInstallable<?> prepareInstallableNode(@NotNull PackageId parentPackageId,
                                                         @NotNull Node node);

    /**
     * Submit a resource path for installation as a list of raw repoinit scripts. The best real-world example at this
     * time are
     * May be called by a check during
     * {@link ProgressCheck#importedPath(PackageId, String, Node, PathAction)}.
     *
     * @param installable the installable
     * @return a handle for the installable path or null
     */
    @Nullable SlingInstallable<?> submitInstallable(@NotNull SlingInstallable<?> installable);
}
