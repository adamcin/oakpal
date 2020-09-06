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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Extended interface marking progress checks that respect a mid-scan mode that expects oakpal to temporarily silence
 * violations triggered by scan events. {@link ProgressCheck}s that do not implement this interface are simply blocked
 * from receiving scan events for the duration of the silent period.
 *
 * @since 2.2.0
 */
@ConsumerType
public interface SilenceableCheck extends ProgressCheck {

    /**
     * If silenced is true, this check must DISABLE the collection of violations until this method is called again
     * with silenced=false.
     *
     * @param silenced true to silence violation reporting
     */
    void setSilenced(boolean silenced);
}
