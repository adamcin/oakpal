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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;

import javax.json.JsonObject;
import java.util.Collection;

/**
 * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Violation}
 */
@Deprecated
public final class Violation {

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Severity}
     */
    @Deprecated
    public enum Severity {
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Severity#MINOR}
         */
        @Deprecated
        MINOR(net.adamcin.oakpal.api.Severity.MINOR),
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Severity#MAJOR}
         */
        @Deprecated
        MAJOR(net.adamcin.oakpal.api.Severity.MAJOR),
        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Severity#SEVERE}
         */
        @Deprecated
        SEVERE(net.adamcin.oakpal.api.Severity.SEVERE);

        private final net.adamcin.oakpal.api.Severity severity;

        Severity(final net.adamcin.oakpal.api.Severity severity) {
            this.severity = severity;
        }

        public net.adamcin.oakpal.api.Severity getSeverity() {
            return severity;
        }

        static Severity forSeverity(final @NotNull net.adamcin.oakpal.api.Severity severity) {
            for (Severity value : values()) {
                if (value.severity == severity) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown severity level: " + severity.name());
        }

        /**
         * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Severity#byName(String)}
         */
        @Deprecated
        public static Severity byName(final @NotNull String name) {
            final net.adamcin.oakpal.api.Severity severity = net.adamcin.oakpal.api.Severity.byName(name);
            return forSeverity(severity);
        }
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Violation#getDescription()}
     */
    @Deprecated
    public static String getDescription(net.adamcin.oakpal.api.Violation violation) {
        return violation.getDescription();
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Violation#getPackages()}
     */
    @Deprecated
    public static Collection<PackageId> getPackages(net.adamcin.oakpal.api.Violation violation) {
        return violation.getPackages();
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Violation#getSeverity()}
     */
    @Deprecated
    public static Severity getSeverity(net.adamcin.oakpal.api.Violation violation) {
        return Severity.forSeverity(violation.getSeverity());
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Violation#toString()}
     */
    @Deprecated
    public static String toString(net.adamcin.oakpal.api.Violation violation) {
        return violation.toString();
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.Violation#toJson()}
     */
    @Deprecated
    public static JsonObject toJson(net.adamcin.oakpal.api.Violation violation) {
        return violation.toJson();
    }

}
