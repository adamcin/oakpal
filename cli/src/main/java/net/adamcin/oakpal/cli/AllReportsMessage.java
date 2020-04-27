/*
 * Copyright 2019 Mark Adamcin
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

package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ReportMapper;
import org.jetbrains.annotations.NotNull;

import javax.json.JsonObject;
import java.util.List;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.Fun.compose1;

class AllReportsMessage implements StructuredMessage {

    private final List<CheckReport> reports;

    AllReportsMessage(final @NotNull List<CheckReport> reports) {
        this.reports = reports;
    }

    @Override
    public String toString() {
        return reports.stream()
                .map(compose1(ReportMessage::new, ReportMessage::toString))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public JsonObject toJson() {
        return ReportMapper.reportsToJsonObject(reports);
    }
}
