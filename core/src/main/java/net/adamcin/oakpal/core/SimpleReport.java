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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Simple POJO implementing a {@link CheckReport}, used for deserialization.
 */
public class SimpleReport implements CheckReport {

    private final String checkName;
    private final Collection<Violation> violations;

    public SimpleReport(final String checkName, Collection<Violation> violations) {
        this.checkName = checkName;
        this.violations = Collections.unmodifiableCollection(violations);
    }

    @Override
    public String getCheckName() {
        return this.checkName;
    }

    @Override
    public Collection<Violation> getViolations() {
        return violations;
    }

    public static SimpleReport generateReport(final ProgressCheck reporter) {
        return new SimpleReport(Optional.ofNullable(reporter.getCheckName())
                .orElse(reporter.getClass().getSimpleName()),
                reporter.getReportedViolations().stream()
                        .map(SimpleViolation::fromReported)
                        .collect(Collectors.toList()));
    }

    public static SimpleReport generateReport(final ErrorListener reporter) {
        return new SimpleReport(reporter.getClass().getSimpleName(),
                reporter.getReportedViolations().stream()
                        .map(SimpleViolation::fromReported)
                        .collect(Collectors.toList()));
    }
}
