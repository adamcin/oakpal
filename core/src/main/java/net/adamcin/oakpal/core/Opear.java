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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * OPEAR stands for "OakPal Encapsulated ARchive".
 */
public interface Opear {

    String MF_BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    String MF_BUNDLE_VERSION = "Bundle-Version";
    String MF_CLASS_PATH = "Bundle-ClassPath";
    String MF_OAKPAL_VERSION = "Oakpal-Version";
    String MF_OAKPAL_PLAN = "Oakpal-Plan";
    String SIMPLE_DIR_PLAN = "plan.json";

    /**
     * Get the default plan url specfied by the opear manifest, which may be {@link OakpalPlan#EMPTY_PLAN_URL} if no
     * other plan is exported.
     *
     * @return the default plan url
     */
    URL getDefaultPlan();

    /**
     * When the opear exports multiple plans, use this method to request a specific plan other than the default. Will
     * return {@link Result#failure(String)} if the specified plan name is not found.
     *
     * @param planName the specified plan name (relative path within opear)
     * @return a URL if successful, or an error if plan is not found
     */
    Result<URL> getSpecificPlan(final @NotNull String planName);

    /**
     * Get a URL classloader constructed for this opear using the provided classloader as the parent.
     *
     * @param parent the parent classloader
     * @return a classloader with permission to load all classes and resources in the opear
     */
    ClassLoader getPlanClassLoader(final @NotNull ClassLoader parent);


}
