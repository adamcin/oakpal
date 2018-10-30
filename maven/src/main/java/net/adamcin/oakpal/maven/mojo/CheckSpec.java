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

package net.adamcin.oakpal.maven.mojo;

import net.adamcin.oakpal.core.PackageCheck;
import net.adamcin.oakpal.core.PackageCheckFactory;
import org.json.JSONObject;

/**
 * DTO for full-featured check spec.
 */
public class CheckSpec {
    private String impl;
    private String name;
    private JSONObject config;

    /**
     * The direct classpath lookup name for a particular check. If not provided, indicates that a check should be
     * looked up by name from a catalog on the classpath.
     *
     * @return className or script resource name of a {@link PackageCheck} or {@link PackageCheckFactory}.
     */
    public String getImpl() {
        return impl;
    }

    public void setImpl(final String impl) {
        this.impl = impl;
    }

    /**
     * The display name for the check. If "impl" is provided, and represents a script package check or a class that
     * implements {@link PackageCheckFactory} this is treated as an alias for the check during
     * this execution.
     * <p>
     * If "impl" is not provided, this is used to lookup a catalog check.
     * </p>
     * <p>
     * If "impl" is not a {@link PackageCheckFactory}, this value is ignored.
     * </p>
     *
     * @return the checkName
     */
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * If {@code impl} references a script check or a {@link PackageCheckFactory},
     * or if the check loaded from a catalog by {@code name} is a script check or a
     * {@link PackageCheckFactory}, this is used to configure the check.
     *
     * @return the JSONObject configuration for the package check
     */
    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(final JSONObject config) {
        this.config = config;
    }
}
