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

package net.adamcin.oakpal.api;

import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Base interface for violation reporters.
 */
@ConsumerType
public interface ViolationReporter {

    /**
     * Get the resource bundle base name for loading the default resource bundle for this violation reporter. Returns
     * {@code getClass().getName()} by default. If this method is overridden to return null, the oakpal framework will
     * not attempt to load a parent resource bundle specific to this violation reporter when creating a resource bundle
     * during initialization.
     *
     * @return the resource bundle base name
     */
    @Nullable
    default String getResourceBundleBaseName() {
        return getClass().getName();
    }

    /**
     * Called by the framework before a scan to provide a resource bundle for immediate localization of strings.
     *
     * @param resourceBundle the resource bundle
     */
    default void setResourceBundle(ResourceBundle resourceBundle) {

    }

    /**
     * Called at the end of execution to collect any detected violations.
     *
     * @return any reported violations.
     */
    Collection<Violation> getReportedViolations();
}
