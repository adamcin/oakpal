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
import net.adamcin.oakpal.api.SlingInstallable;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Locates a jcr path that should be treated as an installable provider of repoinit scripts.
 */
public final class SlingRepoInitScripts {
    public static final String REPO_INIT_FACTORY_PID = "org.apache.sling.jcr.repoinit.RepositoryInitializer";
    public static final String CONFIG_SCRIPTS = "scripts";
    private final @NotNull List<String> scripts;
    private final @NotNull SlingInstallable slingInstallable;

    SlingRepoInitScripts(@NotNull final List<String> scripts, @NotNull final SlingInstallable slingInstallable) {
        this.scripts = scripts;
        this.slingInstallable = slingInstallable;
    }

    @NotNull
    public List<String> getScripts() {
        return scripts;
    }

    public SlingInstallable getSlingInstallable() {
        return slingInstallable;
    }

    @Nullable
    public static SlingRepoInitScripts fromSlingInstallable(final @NotNull SlingInstallable slingInstallable) {
        if (slingInstallable instanceof OsgiConfigInstallable) {
            OsgiConfigInstallable params = (OsgiConfigInstallable) slingInstallable;
            if (REPO_INIT_FACTORY_PID.equals(params.getFactoryPid()) && params.getProperties().containsKey(CONFIG_SCRIPTS)) {
                return new SlingRepoInitScripts(Arrays.asList(PropertiesUtil
                        .toStringArray(params.getProperties().get(CONFIG_SCRIPTS), new String[0])), slingInstallable);
            }
        }
        return null;
    }
}
