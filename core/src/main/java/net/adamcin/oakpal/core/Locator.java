/*
 * Copyright 2018 Mark Adamcin
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

import java.net.URL;

/**
 * Unified class path locator for PackageLister classes and scripts.
 */
public final class Locator {

    /**
     * No instantiation.
     */
    private Locator() {
        // prevent instantiation
    }

    /**
     * Attempt to load a {@link PackageCheck} from the class path.
     *
     * @param name className or resourceName
     * @return a new {@link PackageCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static PackageCheck loadPackageCheck(final String name) throws Exception {
        return loadPackageCheck(name, Locator.class.getClassLoader());
    }

    /**
     * Attempt to load a {@link PackageCheck} from a particular class loader.
     *
     * @param name className or resourceName
     * @param classLoader a specific classLoader to use
     * @return a new {@link PackageCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static PackageCheck loadPackageCheck(final String name, final ClassLoader classLoader) throws Exception {
        if (!name.contains("/") && !name.contains("\\")) {
            Class<?> clazz = classLoader.loadClass(name);
            return PackageCheck.class.cast(clazz.getConstructor().newInstance());
        } else {
            final URL resourceUrl = classLoader.getResource(name);
            if (resourceUrl != null) {
                return ScriptPackageCheck.createScriptListener(resourceUrl);
            } else {
                throw new Exception("Failed to find class path resource by name: " + name);
            }
        }
    }
}
