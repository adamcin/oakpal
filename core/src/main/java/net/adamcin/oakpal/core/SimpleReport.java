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

import org.jetbrains.annotations.NotNull;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfObjects;
import static net.adamcin.oakpal.core.JavaxJson.optArray;
import static net.adamcin.oakpal.core.ReportMapper.KEY_CHECK_NAME;
import static net.adamcin.oakpal.core.ReportMapper.KEY_VIOLATIONS;

/**
 * Simple POJO implementing a {@link CheckReport}, used for deserialization.
 */
public final class SimpleReport implements CheckReport {

    private final String checkName;
    private final List<Violation> violations;

    public SimpleReport(final String checkName, final Collection<Violation> violations) {
        this.checkName = checkName;
        this.violations = Collections.unmodifiableList(
                new ArrayList<>(violations != null ? violations : Collections.emptyList()));
    }

    @Override
    public String getCheckName() {
        return this.checkName;
    }

    @Override
    public @NotNull Collection<Violation> getViolations() {
        return violations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleReport that = (SimpleReport) o;
        return Objects.equals(checkName, that.checkName) &&
                violations.equals(that.violations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkName, violations);
    }

    @Override
    public String toString() {
        return "SimpleReport{" +
                "checkName='" + checkName + '\'' +
                ", violations=" + violations +
                '}';
    }

    public static SimpleReport generateReport(final @NotNull ProgressCheck reporter) {
        return new SimpleReport(Optional.ofNullable(reporter.getCheckName())
                .orElse(reporter.getClass().getSimpleName()),
                reporter.getReportedViolations().stream()
                        .map(SimpleViolation::fromReported)
                        .collect(Collectors.toList()));
    }

    public static SimpleReport generateReport(final @NotNull ErrorListener reporter) {
        return new SimpleReport(reporter.getClass().getSimpleName(),
                reporter.getReportedViolations().stream()
                        .map(SimpleViolation::fromReported)
                        .collect(Collectors.toList()));
    }

    public static SimpleReport fromJson(final @NotNull JsonObject jsonReport) {
        String vCheckName = jsonReport.getString(KEY_CHECK_NAME, "");
        List<Violation> violations = optArray(jsonReport, KEY_VIOLATIONS)
                .map(array -> mapArrayOfObjects(array, ReportMapper::violationFromJson))
                .orElseGet(Collections::emptyList);

        return new SimpleReport(vCheckName, violations);
    }
}
