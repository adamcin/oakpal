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

import org.jetbrains.annotations.NotNull;

/**
 * Hosts constants for interface types as static singleton getter methods defined by associated interfaces.
 * This reduces the impact on semantic versioning rules of adding or modifying interface constants. A similar pattern is
 * followed on concrete classes when they must define JSON keys, but each concrete class owns its own constants
 * interfaces.
 */
public final class CoreConstants {
    private CoreConstants() {
        /* no constructor */
    }

    /**
     * Json keys for CheckReport. Use {@link CoreConstants#checkReportKeys()} to access singleton.
     */
    public interface CheckReportKeys {
        String checkName();

        String violations();
    }

    private static final CheckReportKeys CHECK_REPORT_KEYS = new CheckReportKeys() {
        @Override
        public String checkName() {
            return "checkName";
        }

        @Override
        public String violations() {
            return "violations";
        }
    };

    @NotNull
    public static CheckReportKeys checkReportKeys() {
        return CHECK_REPORT_KEYS;
    }
}
