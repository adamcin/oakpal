/*
 * Copyright 2017 Mark Adamcin
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import aQute.bnd.annotation.ProviderType;
import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * Simple implementation of a {@link Violation}.
 */
@ProviderType
public final class SimpleViolation implements Violation {
    private final Severity severity;
    private final String description;
    private final List<PackageId> packages;

    public SimpleViolation(Severity severity, String description, PackageId... packages) {
        this(severity, description, packages != null ? Arrays.asList(packages) : null);
    }

    public SimpleViolation(Severity severity, String description, List<PackageId> packages) {
        this.severity = severity;
        this.description = description;
        this.packages = packages != null ? new ArrayList<>(packages) : Collections.emptyList();
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public Collection<PackageId> getPackages() {
        return Collections.unmodifiableList(packages);
    }

    @Override
    public String getDescription() {
        return description;
    }

    public static SimpleViolation fromReported(Violation violation) {
        Severity severity = violation.getSeverity();
        String description = violation.getDescription();
        List<PackageId> packages = new ArrayList<>(violation.getPackages());
        return new SimpleViolation(severity, description, packages);
    }
}
