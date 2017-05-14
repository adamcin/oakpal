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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Simple POJO implementing a {@link ViolationReport}, used for deserialization.
 */
public class SimpleViolationReport implements ViolationReport {

    private final URL reporterUrl;
    private final Collection<Violation> violations;

    public SimpleViolationReport(URL reporterUrl, Collection<Violation> violations) {
        this.reporterUrl = reporterUrl;
        this.violations = Collections.unmodifiableCollection(violations);
    }

    @Override
    public URL getReporterUrl() {
        return reporterUrl;
    }

    @Override
    public Collection<Violation> getViolations() {
        return violations;
    }

    public static SimpleViolationReport generateReport(ViolationReporter reporter) {
        return new SimpleViolationReport(reporter.getReporterUrl(),
                reporter.reportViolations().stream()
                        .map(SimpleViolation::fromReported)
                        .collect(Collectors.toList()));
    }
}
