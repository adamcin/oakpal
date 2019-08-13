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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Simple collector of violations for use by {@link ProgressCheck} implementations.
 */
public final class ReportCollector implements ViolationReporter {
    private final List<Violation> violations = new ArrayList<>();

    public void reportViolation(Violation violation) {
        violations.add(violation);
    }

    @SuppressWarnings("WeakerAccess")
    public void clearViolations() {
        this.violations.clear();
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        List<Violation> toReturn = new ArrayList<>(this.violations);
        return Collections.unmodifiableList(toReturn);
    }
}
