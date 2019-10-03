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

import java.util.Collection;

/**
 * Simple implementation of a {@link ProgressCheck} with convenient methods for reporting and collecting violations.
 */
public class SimpleProgressCheck implements ProgressCheck {
    protected final ReportCollector collector = new ReportCollector();

    protected void reportViolation(final Violation violation) {
        collector.reportViolation(violation);
    }

    protected final void reportViolation(final Violation.Severity severity,
                                         final String description,
                                         final PackageId... packages) {
        this.reportViolation(new SimpleViolation(severity, description, packages));
    }

    protected final void minorViolation(final String description, final PackageId... packages) {
        this.reportViolation(new SimpleViolation(Violation.Severity.MINOR, description, packages));
    }

    protected final void majorViolation(final String description, final PackageId... packages) {
        this.reportViolation(new SimpleViolation(Violation.Severity.MAJOR, description, packages));
    }

    protected final void severeViolation(final String description, final PackageId... packages) {
        this.reportViolation(new SimpleViolation(Violation.Severity.SEVERE, description, packages));
    }

    @Override
    public void startedScan() {
        collector.clearViolations();
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return collector.getReportedViolations();
    }
}
