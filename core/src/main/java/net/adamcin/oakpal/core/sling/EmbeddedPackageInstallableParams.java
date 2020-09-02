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

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;

public class EmbeddedPackageInstallableParams implements SlingInstallableParams<EmbeddedPackageInstallable> {
    private final @NotNull PackageId embeddedId;

    public EmbeddedPackageInstallableParams(@NotNull final PackageId embeddedId) {
        this.embeddedId = embeddedId;
    }

    public PackageId getEmbeddedId() {
        return embeddedId;
    }

    @NotNull
    @Override
    public EmbeddedPackageInstallable createInstallable(final PackageId parentPackageId, final String jcrPath) {
        return new EmbeddedPackageInstallable(parentPackageId, jcrPath, embeddedId);
    }

    @Override
    public @NotNull Class<EmbeddedPackageInstallable> getInstallableType() {
        return EmbeddedPackageInstallable.class;
    }
}
