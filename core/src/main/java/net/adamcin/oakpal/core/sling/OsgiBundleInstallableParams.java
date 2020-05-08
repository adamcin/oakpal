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

import net.adamcin.oakpal.api.OsgiBundleInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.jar.Manifest;

public class OsgiBundleInstallableParams implements SlingInstallableParams<OsgiBundleInstallable> {
    private final @Nullable String installationHint;
    private final @NotNull Manifest manifest;
    private final @NotNull String symbolicName;
    private final @NotNull String version;
    private final @Nullable String activationPolicy;

    public OsgiBundleInstallableParams(@Nullable final String installationHint,
                                       @NotNull final Manifest manifest,
                                       @NotNull final String symbolicName,
                                       @NotNull final String version,
                                       @Nullable final String activationPolicy) {
        this.installationHint = installationHint;
        this.manifest = manifest;
        this.symbolicName = symbolicName;
        this.version = version;
        this.activationPolicy = activationPolicy;
    }

    @NotNull
    @Override
    public OsgiBundleInstallable createInstallable(final PackageId parentPackageId, final String jcrPath) {
        return new OsgiBundleInstallable(parentPackageId, jcrPath, installationHint,
                manifest, symbolicName, version, activationPolicy);
    }
}
