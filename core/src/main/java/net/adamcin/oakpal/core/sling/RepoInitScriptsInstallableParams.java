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

import net.adamcin.oakpal.api.RepoInitScriptsInstallable;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class RepoInitScriptsInstallableParams implements SlingInstallableParams<RepoInitScriptsInstallable> {
    public static final String REPO_INIT_FACTORY_PID = "org.apache.sling.jcr.repoinit.RepositoryInitializer";
    public static final String CONFIG_SCRIPTS = "scripts";

    private final List<String> scripts;

    public RepoInitScriptsInstallableParams(final List<String> scripts) {
        this.scripts = scripts;
    }

    public List<String> getScripts() {
        return scripts;
    }

    @NotNull
    @Override
    public RepoInitScriptsInstallable createInstallable(final PackageId parentPackageId, final String jcrPath) {
        return new RepoInitScriptsInstallable(parentPackageId, jcrPath, scripts);
    }

    @Nullable
    static RepoInitScriptsInstallableParams fromOsgiConfigInstallableParams(final @NotNull OsgiConfigInstallableParams params) {
        if (REPO_INIT_FACTORY_PID.equals(params.getFactoryPid()) && params.getProperties().containsKey(CONFIG_SCRIPTS)) {
            return new RepoInitScriptsInstallableParams(Arrays.asList(PropertiesUtil
                    .toStringArray(params.getProperties().get(CONFIG_SCRIPTS), new String[0])));
        }
        return null;
    }
}
