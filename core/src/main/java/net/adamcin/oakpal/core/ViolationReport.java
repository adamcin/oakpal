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
import java.util.stream.Collectors;

/**
 * Type for collected {@link Violation}s from a particlular {@link ViolationReporter}.
 */
public interface ViolationReport {

    /**
     * The URL of the reporter.
     * @return the URL of the reporter
     */
    URL getReporterUrl();

    /**
     * The reported violations.
     * @return the reported violations
     */
    Collection<Violation> getViolations();

    /**
     * The list of violations filtered to ignore severities less than {@code atLeastAsSevere}.
     * @param atLeastAsSevere lower bound for severity
     * @return the reported violations filtered by severity
     */
    default Collection<Violation> getViolations(Violation.Severity atLeastAsSevere) {
        if (atLeastAsSevere == null) {
            return getViolations();
        }
        return getViolations().stream().filter(v -> v != null
                && !v.getSeverity().isLessSevereThan(atLeastAsSevere))
                .collect(Collectors.toList());
    }
}
