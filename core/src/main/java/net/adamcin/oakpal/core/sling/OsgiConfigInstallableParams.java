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

import net.adamcin.oakpal.api.OsgiConfigInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OsgiConfigInstallableParams implements SlingInstallableParams<OsgiConfigInstallable> {
    private final @NotNull Map<String, Object> properties;
    private final @NotNull String servicePid;
    private final @Nullable String factoryPid;

    public OsgiConfigInstallableParams(@NotNull final Map<String, Object> properties,
                                       @NotNull final String servicePid,
                                       @Nullable final String factoryPid) {
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
        this.servicePid = servicePid;
        this.factoryPid = factoryPid;
    }

    public String getServicePid() {
        return servicePid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @NotNull
    @Override
    public OsgiConfigInstallable createInstallable(final PackageId parentPackageId, final String jcrPath) {
        return new OsgiConfigInstallable(parentPackageId, jcrPath, properties, servicePid, factoryPid);
    }
}
