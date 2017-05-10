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

package net.adamcin.opal.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * Simple implementation of a {@link OpalViolation}.
 */
public final class SimpleViolation implements OpalViolation {
    private final Severity severity;
    private final String description;
    private final List<PackageId> violatingPackages;

    public SimpleViolation(Severity severity, String description, PackageId... violatingPackages) {
        this.severity = severity;
        this.description = description;
        if (violatingPackages != null) {
            this.violatingPackages = Arrays.asList(violatingPackages);
        } else {
            this.violatingPackages = Collections.emptyList();
        }
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public Iterable<PackageId> getViolatingPackages() {
        return Collections.unmodifiableList(violatingPackages);
    }

    @Override
    public String getDescription() {
        return description;
    }
}
