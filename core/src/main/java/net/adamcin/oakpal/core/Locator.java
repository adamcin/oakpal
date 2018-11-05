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

import org.json.JSONObject;

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
     * Attempt to load a {@link ProgressCheck} from the class path.
     *
     * @param impl className or resourceName
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static ProgressCheck loadPackageCheck(final String impl) throws Exception {
        return loadPackageCheck(impl, null);
    }

    /**
     * Attempt to load a {@link ProgressCheck} from a particular class loader.
     *
     * @param impl   className or resourceName
     * @param config provide an optional config object (may be ignored by the check.)
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static ProgressCheck loadPackageCheck(final String impl, final JSONObject config) throws Exception {
        return loadPackageCheck(impl, config, Util.getDefaultClassLoader());
    }

    /**
     * Attempt to load a {@link ProgressCheck} from a particular class loader.
     *
     * @param impl        className or resourceName
     * @param config      provide an optional config object (may be ignored by the check.)
     * @param classLoader a specific classLoader to use
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static ProgressCheck loadPackageCheck(final String impl, final JSONObject config,
                                                 final ClassLoader classLoader) throws Exception {
        if (!impl.contains("/") && !impl.contains("\\")) {
            try {
                Class<?> clazz = classLoader.loadClass(impl);
                if (PackageCheckFactory.class.isAssignableFrom(clazz)) {
                    return PackageCheckFactory.class.cast(clazz.getConstructor().newInstance())
                            .newInstance(config == null ? new JSONObject() : config);
                } else if (ProgressCheck.class.isAssignableFrom(clazz)) {
                    return ProgressCheck.class.cast(clazz.getConstructor().newInstance());
                } else {
                    throw new Exception("impl names class that does not implement PackageCheckFactory or PackageCheck: " +
                            clazz.getName());
                }
            } catch (ClassNotFoundException e) {
                final URL resourceUrl = classLoader.getResource(impl);
                if (resourceUrl != null) {
                    return ScriptProgressCheck.createScriptCheckFactory(resourceUrl)
                            .newInstance(config == null ? new JSONObject() : config);
                } else {
                    throw e;
                }
            }
        } else {
            final URL resourceUrl = classLoader.getResource(impl);
            if (resourceUrl != null) {
                return ScriptProgressCheck.createScriptCheckFactory(resourceUrl)
                        .newInstance(config == null ? new JSONObject() : config);
            } else {
                throw new Exception("Failed to find class path resource by name: " + impl);
            }
        }
    }

    /**
     * Rename the provided package check with the provided alias.
     *
     * @param progressCheck the check to rename.
     * @param alias        the name to override {@link ProgressCheck#getCheckName()}.
     * @return the renamed package check.
     */
    public static ProgressCheck wrapWithAlias(final ProgressCheck progressCheck, final String alias) {
        return new ProgressCheckAliasFacade(progressCheck, alias);
    }
}
