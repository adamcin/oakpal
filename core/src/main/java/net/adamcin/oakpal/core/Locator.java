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

import static net.adamcin.oakpal.core.Util.isEmpty;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

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
    public static ProgressCheck loadProgressCheck(final String impl) throws Exception {
        return loadProgressCheck(impl, (JsonObject) null);
    }

    /**
     * Attempt to load a {@link ProgressCheck} from a particular class loader.
     *
     * @param impl   className or resourceName
     * @param config provide an optional config object (may be ignored by the check.)
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     * @deprecated 1.2.0 Please use {@link #loadProgressCheck(String, JsonObject)} instead
     */
    @Deprecated
    public static ProgressCheck loadProgressCheck(final String impl, final JSONObject config) throws Exception {
        return loadProgressCheck(impl, config, Util.getDefaultClassLoader());
    }

    /**
     * Attempt to load a {@link ProgressCheck} from a particular class loader.
     *
     * @param impl   className or resourceName
     * @param config provide an optional config object (may be ignored by the check.)
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static ProgressCheck loadProgressCheck(final String impl, final JsonObject config) throws Exception {
        return loadProgressCheck(impl, config, Util.getDefaultClassLoader());
    }

    /**
     * Attempt to load a {@link ProgressCheck} from a particular class loader. The {@code impl} value is first tried as
     * a fully-qualified class name, and if a {@code Class<?>} is found, it is first checked for the
     * {@link ProgressCheckFactory} interface, and then for the {@link ProgressCheck} interface. If a class is not
     * found, {@link Locator} assumes the {@code impl} value represents a resource name for an
     * {@link javax.script.Invocable} script, and attempts to create a {@link ScriptProgressCheck} from it.
     *
     * @param impl        className or resourceName
     * @param config      provide an optional config object (may be ignored by the check.)
     * @param classLoader a specific classLoader to use
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     * @deprecated 1.2.0 Please use {@link #loadProgressCheck(String, JsonObject, ClassLoader)} instead
     */
    @Deprecated
    public static ProgressCheck loadProgressCheck(final String impl, final JSONObject config,
                                                  final ClassLoader classLoader) throws Exception {
        return loadProgressCheck(impl,
                config != null ? Json.createObjectBuilder(config.toMap()).build() : null,
                classLoader);
    }

    /**
     * Attempt to load a {@link ProgressCheck} from a particular class loader. The {@code impl} value is first tried as
     * a fully-qualified class name, and if a {@code Class<?>} is found, it is first checked for the
     * {@link ProgressCheckFactory} interface, and then for the {@link ProgressCheck} interface. If a class is not
     * found, {@link Locator} assumes the {@code impl} value represents a resource name for an
     * {@link javax.script.Invocable} script, and attempts to create a {@link ScriptProgressCheck} from it.
     *
     * @param impl        className or resourceName
     * @param config      provide an optional config object (may be ignored by the check.)
     * @param classLoader a specific classLoader to use
     * @return a new {@link ProgressCheck} instance for the given name
     * @throws Exception on any error or failure to find a resource for given name.
     */
    public static ProgressCheck loadProgressCheck(final String impl, final JsonObject config,
                                                  final ClassLoader classLoader) throws Exception {
        if (!impl.contains("/") && !impl.contains("\\")) {
            try {
                Class<?> clazz = classLoader.loadClass(impl);
                if (ProgressCheckFactory.class.isAssignableFrom(clazz)) {
                    return ProgressCheckFactory.class.cast(clazz.getConstructor().newInstance())
                            .newInstance(config == null ? JsonValue.EMPTY_JSON_OBJECT : config);
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
                            .newInstance(config == null ? JsonValue.EMPTY_JSON_OBJECT : config);
                } else {
                    throw e;
                }
            }
        } else {
            final URL resourceUrl = classLoader.getResource(impl);
            if (resourceUrl != null) {
                return ScriptProgressCheck.createScriptCheckFactory(resourceUrl)
                        .newInstance(config == null ? JsonValue.EMPTY_JSON_OBJECT : config);
            } else {
                throw new Exception("Failed to find class path resource by name: " + impl);
            }
        }
    }

    /**
     * Rename the provided package check with the provided alias.
     *
     * @param progressCheck the check to rename.
     * @param alias         the name to override {@link ProgressCheck#getCheckName()}.
     * @return the renamed package check.
     */
    public static ProgressCheck wrapWithAlias(final ProgressCheck progressCheck, final String alias) {
        return new ProgressCheckAliasFacade(progressCheck, alias);
    }

    /**
     *
     * @param checkSpecs
     * @return
     * @throws Exception
     */
    public static List<ProgressCheck> loadFromCheckSpecs(final List<CheckSpec> checkSpecs) throws Exception {
        return loadFromCheckSpecs(checkSpecs, Util.getDefaultClassLoader());
    }

    /**
     *
     * @param checkSpecs
     * @param checkLoader
     * @return
     * @throws Exception
     */
    public static List<ProgressCheck> loadFromCheckSpecs(final List<CheckSpec> checkSpecs,
                                                         final ClassLoader checkLoader) throws Exception {
        final List<ProgressCheck> allChecks = new ArrayList<>();
        for (CheckSpec checkSpec : checkSpecs) {
            if (checkSpec.isAbstract()) {
                throw new Exception("Please provide an 'impl' value for " + checkSpec.getName());
            }
            try {
                ProgressCheck progressCheck;
                if (!isEmpty(checkSpec.getImpl())) {
                    progressCheck = Locator.loadProgressCheck(checkSpec.getImpl(), checkSpec.getConfig(), checkLoader);
                } else {
                    progressCheck = ScriptProgressCheck
                            .createInlineScriptCheckFactory(checkSpec.getInlineScript(), checkSpec.getInlineEngine())
                            .newInstance(checkSpec.getConfig());
                }
                if (checkSpec.getName() != null && !checkSpec.getName().isEmpty()) {
                    progressCheck = wrapWithAlias(progressCheck, checkSpec.getName());
                }
                allChecks.add(progressCheck);
            } catch (final Exception e) {
                throw new Exception(String.format("Failed to load package check %s. (impl: %s)",
                        Optional.ofNullable(checkSpec.getName()).orElse(""), checkSpec.getImpl()), e);
            }
        }
        return allChecks;

    }
}
