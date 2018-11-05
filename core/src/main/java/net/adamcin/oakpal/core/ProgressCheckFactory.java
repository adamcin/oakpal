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

import org.json.JSONObject;

/**
 * A factory for creating {@link ProgressCheck}s that accepts a config map.
 *
 * @since 0.5.0
 */
public interface ProgressCheckFactory {

    /**
     * Returns a new check with the provided config.
     *
     * @param config an arbitrary config object
     * @return new PackageCheck, optionally taking the provided config into account.
     * @throws Exception for whatever reason instance creation fails.
     */
    ProgressCheck newInstance(final JSONObject config) throws Exception;
}
