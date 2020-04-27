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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.Nullable;

import javax.json.JsonObject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.api.JavaxJson.optArray;

/**
 * Simple implementation of a {@link Violation}.
 */
public final class SimpleViolation implements Violation {
    private final Severity severity;
    private final String description;
    private final List<PackageId> packages;

    /**
     * Constructor.
     *
     * @param severity    the severity
     * @param description the description
     * @param packages    the package ids
     */
    public SimpleViolation(final Severity severity, final String description, final PackageId... packages) {
        this(severity, description, packages != null ? Arrays.asList(packages) : null);
    }

    /**
     * Constructor.
     *
     * @param severity    the severity
     * @param description the description
     * @param packages    the package ids
     */
    public SimpleViolation(final Severity severity, final String description, final List<PackageId> packages) {
        this.severity = severity != null ? severity : Severity.MAJOR;
        this.description = description;
        this.packages = packages == null || packages.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(packages));
    }

    /**
     * Use this builder method to more easily construct a violation with MessageFormat arguments.
     *
     * @return a new SimpleViolation Builder
     */
    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * Use this builder method to more easily construct a violation with MessageFormat arguments.
     *
     * @param resourceBundle a ResourceBundle to lookup a localized message format string if available
     * @return a new SimpleViolation Builder
     */
    public static Builder builder(final ResourceBundle resourceBundle) {
        return new Builder(resourceBundle);
    }

    public static final class Builder {
        @Nullable
        private final ResourceBundle resourceBundle;
        private Severity severity;
        private String description;
        private List<Object> arguments = new ArrayList<>();
        private List<PackageId> packages = new ArrayList<>();

        private Builder(@Nullable final ResourceBundle resourceBundle) {
            this.resourceBundle = resourceBundle;
        }

        /**
         * Set severity.
         *
         * @param severity severity value
         * @return this builder
         */
        public Builder withSeverity(final Severity severity) {
            this.severity = severity;
            return this;
        }

        /**
         * Set description.
         *
         * @param description description
         * @return this builder
         */
        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Set arguments.
         *
         * @param arguments arguments
         * @return this builder
         */
        public Builder withArguments(final List<Object> arguments) {
            this.arguments = arguments != null ? new ArrayList<>(arguments) : new ArrayList<>();
            return this;
        }

        /**
         * Add one or more arguments.
         *
         * @param argument vararg argument values
         * @return this builder
         */
        public Builder withArgument(final Object... argument) {
            if (argument != null) {
                this.arguments.addAll(Arrays.asList(argument));
            }
            return this;
        }

        /**
         * Set package ids.
         *
         * @param packages package ids
         * @return this builder
         */
        public Builder withPackages(final List<PackageId> packages) {
            this.packages = packages != null ? new ArrayList<>(packages) : new ArrayList<>();
            return this;
        }

        /**
         * Add one or more packageIds.
         *
         * @param packageId vararg packageId values
         * @return this builder
         */
        public Builder withPackage(final PackageId... packageId) {
            if (packageId != null) {
                this.packages.addAll(Arrays.asList(packageId));
            }
            return this;
        }

        /**
         * Build a simple violation.
         *
         * @return a new violation
         */
        public SimpleViolation build() {
            final String localDescription =
                    description != null && resourceBundle != null && resourceBundle.containsKey(description)
                            ? resourceBundle.getString(description)
                            : description;
            if (localDescription == null || arguments == null || arguments.isEmpty()) {
                return new SimpleViolation(severity, localDescription, packages);
            } else {
                return new SimpleViolation(severity, MessageFormat.format(localDescription,
                        arguments.toArray(new Object[arguments.size()])), packages);
            }
        }
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public Collection<PackageId> getPackages() {
        return packages;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public static SimpleViolation fromReported(final Violation violation) {
        Severity severity = violation.getSeverity();
        String description = violation.getDescription();
        List<PackageId> packages = new ArrayList<>(violation.getPackages());
        return new SimpleViolation(severity, description, packages);
    }

    public static SimpleViolation fromJson(final JsonObject jsonViolation) {
        String vSeverity = jsonViolation.getString(ApiConstants.violationKeys().severity(), Severity.MINOR.name());
        Severity severity = Severity.valueOf(vSeverity);
        String description = jsonViolation.getString(ApiConstants.violationKeys().description(), "");
        List<PackageId> packages = optArray(jsonViolation, ApiConstants.violationKeys().packages())
                .map(array -> mapArrayOfStrings(array, PackageId::fromString, true))
                .orElseGet(Collections::emptyList);

        return new SimpleViolation(severity, description, packages);
    }

    @Override
    public String toString() {
        return "SimpleViolation{" +
                "severity=" + severity +
                ", description='" + description + '\'' +
                ", packages=" + packages +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleViolation that = (SimpleViolation) o;
        return severity == that.severity &&
                Objects.equals(description, that.description) &&
                packages.equals(that.packages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, description, packages);
    }
}
