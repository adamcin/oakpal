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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Hosts constants as static singleton getter methods defined by interfaces. This reduces the impact on semantic
 * versioning rules of adding or modifying interface constants.
 */
public final class ApiConstants {
    private ApiConstants() {
        /* no construction */
    }

    private static final ViolationKeys VIOLATION_KEYS = new ViolationKeys() {
        @Override
        public String description() {
            return "description";
        }

        @Override
        public String severity() {
            return "severity";
        }

        @Override
        public String packages() {
            return "packages";
        }
    };

    /**
     * Json key constant accessors for violations.
     */
    @ProviderType
    public interface ViolationKeys {
        String description();

        String severity();

        String packages();
    }

    @NotNull
    public static ViolationKeys violationKeys() {
        return VIOLATION_KEYS;
    }
}
