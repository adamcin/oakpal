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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents an installable entity which was detected during a package scan which should be subject to
 * a scan.
 */
@ProviderType
public interface PathInstallable<EntityType> {

    /**
     * The id of the containing package.
     *
     * @return the package id
     */
    @NotNull PackageId getParentId();

    /**
     * The JCR path for the installable entity.
     *
     * @return the path of the entity
     */
    @NotNull String getJcrPath();
}
