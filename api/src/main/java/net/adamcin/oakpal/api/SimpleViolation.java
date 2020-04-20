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

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.api.JavaxJson.optArray;

/**
 * Simple implementation of a {@link Violation}.
 */
public final class SimpleViolation implements Violation {
    private final Severity severity;
    private final String description;
    private final List<PackageId> packages;

    public SimpleViolation(final Severity severity, final String description, final PackageId... packages) {
        this(severity, description, packages != null ? Arrays.asList(packages) : null);
    }

    public SimpleViolation(final Severity severity, final String description, final List<PackageId> packages) {
        this.severity = severity;
        this.description = description;
        this.packages = Collections.unmodifiableList(
                packages != null ? new ArrayList<>(packages) : Collections.emptyList());
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
