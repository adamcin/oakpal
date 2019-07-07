/*
 * Copyright 2019 Mark Adamcin
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

package net.adamcin.oakpal.core;

import org.jetbrains.annotations.Nullable;

/**
 * Enumeration of policies for dealing with InstallHook processing during a scan.
 */
public enum InstallHookPolicy {
    /**
     * Report a violation if any hook is registered successfully. Implies {@link #REPORT}, and {@link #SKIP},
     * insofar as the hook will not be executed after registration. Use this level if your policy is to disallow install
     * hooks in your content packages.
     */
    PROHIBIT,

    /**
     * Report a violation if any hook fails to register. This is likely a class loading issue, or an issue with Jar or
     * Vault packaging. Similar to {@link #ABORT}, except scan will proceed.
     */
    REPORT,

    /**
     * Abort the scan if any hook fails to register. This is likely a class loading issue, or an issue with Jar or
     * Vault packaging.
     */
    ABORT,

    /**
     * Disable install hook processing for scanned packages.
     */
    SKIP;

    public static final InstallHookPolicy DEFAULT = REPORT;

    public static @Nullable InstallHookPolicy forName(final @Nullable String name) {
        for (InstallHookPolicy value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
