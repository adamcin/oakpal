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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.JsonObjectConvertible;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.Violation;
import org.jetbrains.annotations.NotNull;

import javax.json.JsonObject;
import javax.json.stream.JsonCollectors;
import java.util.Collection;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.JavaxJson.obj;

/**
 * Type for collected {@link Violation}s from a particlular {@link ProgressCheck}.
 */
public interface CheckReport extends JsonObjectConvertible {
    /**
     * The serialized display name of the package check that created the report.
     *
     * @return the serialized display name of the reporter.
     */
    String getCheckName();

    /**
     * The reported violations.
     *
     * @return the reported violations
     */
    @NotNull Collection<Violation> getViolations();

    /**
     * The list of violations filtered to ignore severities less than {@code atLeastAsSevere}.
     *
     * @param atLeastAsSevere lower bound for severity
     * @return the reported violations filtered by severity
     */
    default Collection<Violation> getViolations(Severity atLeastAsSevere) {
        if (atLeastAsSevere == null) {
            return getViolations();
        }
        return getViolations().stream().filter(v -> v != null
                && !v.getSeverity().isLessSevereThan(atLeastAsSevere))
                .collect(Collectors.toList());
    }

    /**
     * Serializes the report to a JsonObject.
     *
     * @return the json representation of this report
     */
    @Override
    default JsonObject toJson() {
        JavaxJson.Obj jsonReport = obj();

        jsonReport.key(CoreConstants.checkReportKeys().checkName()).opt(this.getCheckName());
        jsonReport.key(CoreConstants.checkReportKeys().violations()).opt(this.getViolations().stream()
                .map(Violation::toJson)
                .collect(JsonCollectors.toJsonArray()));

        return jsonReport.get();
    }
}
