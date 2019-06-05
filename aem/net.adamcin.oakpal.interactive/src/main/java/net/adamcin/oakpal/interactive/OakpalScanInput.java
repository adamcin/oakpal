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

package net.adamcin.oakpal.interactive;

import java.util.List;

import net.adamcin.oakpal.core.CheckSpec;

/**
 * Common interface for providing input to an OakPAL interactive scan.
 */
public interface OakpalScanInput {

    /**
     * Get the selected checklists.
     *
     * @return the selected checklists
     */
    List<String> getChecklists();

    /**
     * Get the list of repository paths for packages to preinstall.
     *
     * @return the list of repository paths for packages to preinstall
     */
    List<String> getPreInstallPackagePaths();

    /**
     * Get the list of repository paths to packages to scan.
     *
     * @return the list of repository paths to packages to scan
     */
    List<String> getPackagePaths();

    /**
     * Get the list of check specs.
     *
     * @return the list of check specs
     */
    List<CheckSpec> getChecks();

    /**
     * return true to install the platform nodetypes prior to scan.
     *
     * @return true to install the platform nodetypes prior to scan
     */
    boolean isInstallPlatformNodetypes();
}
