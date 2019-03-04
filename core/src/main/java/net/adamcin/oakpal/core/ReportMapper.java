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

import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfObjects;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.core.JavaxJson.optArray;
import static net.adamcin.oakpal.core.JavaxJson.val;
import static net.adamcin.oakpal.core.Util.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonCollectors;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serialize violations to/from json.
 */
public final class ReportMapper {
    public static final String KEY_REPORTS = "reports";
    public static final String KEY_CHECK_NAME = "checkName";
    public static final String KEY_VIOLATIONS = "violations";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_SEVERITY = "severity";
    public static final String KEY_PACKAGES = "packages";

    private ReportMapper() {
        throw new RuntimeException("No instantiation");
    }

    public static List<CheckReport> readReportsFromFile(final File jsonFile)
            throws IOException, JSONException {
        return readReportsFromStream(() -> new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    public interface ReaderSupplier {
        Reader open() throws IOException;
    }

    @FunctionalInterface
    public interface WriterSupplier {
        Writer open() throws IOException;
    }

    public static List<CheckReport> readReportsFromStream(final ReaderSupplier readerSupplier) throws IOException, JSONException {
        try (Reader reader = readerSupplier.open();
             JsonReader jsonReader = Json.createReader(reader)) {

            JsonObject json = jsonReader.readObject();

            List<CheckReport> reports = optArray(json, KEY_REPORTS)
                    .map(jsonReports -> mapArrayOfObjects(jsonReports, ReportMapper::reportFromJson))
                    .orElseGet(Collections::emptyList);

            return Collections.unmodifiableList(reports);
        }
    }

    private static CheckReport reportFromJson(final JsonObject jsonReport) {
        String vCheckName = jsonReport.getString(KEY_CHECK_NAME, "");
        List<Violation> violations = optArray(jsonReport, KEY_VIOLATIONS)
                .map(array -> mapArrayOfObjects(array, ReportMapper::violationFromJson))
                .orElseGet(Collections::emptyList);

        return new SimpleReport(vCheckName, violations);
    }

    private static Violation violationFromJson(final JsonObject jsonViolation) {
        String vSeverity = jsonViolation.getString(KEY_SEVERITY, Violation.Severity.MINOR.name());
        Violation.Severity severity = Violation.Severity.valueOf(vSeverity);
        String description = jsonViolation.getString(KEY_DESCRIPTION, "");
        List<PackageId> packages = optArray(jsonViolation, KEY_PACKAGES)
                .map(array -> mapArrayOfStrings(array, PackageId::fromString, true))
                .orElseGet(Collections::emptyList);

        return new SimpleViolation(severity, description, packages);
    }

    public static void writeReportsToFile(final Collection<CheckReport> reports, final File outputFile) throws IOException {
        writeReports(reports, () -> new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
    }

    public static void writeReports(final Collection<CheckReport> reports, final WriterSupplier writerSupplier) throws IOException {
        try (Writer writer = writerSupplier.open(); JsonWriter jsonWriter = Json.createWriter(writer)) {
            jsonWriter.writeObject(key(KEY_REPORTS, reportsToJson(reports)).get());
        }
    }

    public static JsonArray reportsToJson(final Collection<CheckReport> reports) throws JSONException {
        return val(reports.stream()
                .map(ReportMapper::reportToJson)
                .collect(JsonCollectors.toJsonArray())).get().asJsonArray();
    }

    private static JsonObject reportToJson(final CheckReport report) {
        JavaxJson.Obj jsonReport = obj();

        if (!isEmpty(report.getCheckName())) {
            jsonReport.key(KEY_CHECK_NAME, report.getCheckName());
        }
        if (report.getViolations() != null) {
            jsonReport.key(KEY_VIOLATIONS, report.getViolations().stream()
                    .map(ReportMapper::violationToJson)
                    .collect(JsonCollectors.toJsonArray()));
        }

        return jsonReport.get();
    }

    private static JsonObject violationToJson(final Violation violation) {
        JavaxJson.Obj json = obj();
        ofNullable(violation.getSeverity()).ifPresent(json.key(KEY_SEVERITY)::val);
        ofNullable(violation.getDescription()).ifPresent(json.key(KEY_DESCRIPTION)::val);
        ofNullable(violation.getPackages()).map(col -> col.stream().map(Objects::toString))
                .ifPresent(json.key(KEY_PACKAGES)::val);
        return json.get();
    }


    @Deprecated
    public static JSONArray reportsToJSON(Collection<CheckReport> reports) throws JSONException {
        return new JSONArray(reports.stream()
                .map(ReportMapper::reportToJSON)
                .collect(Collectors.toList()));
    }

    @Deprecated
    private static JSONObject reportToJSON(CheckReport report) throws JSONException {
        JSONObject jsonReport = new JSONObject();
        if (report.getCheckName() != null) {
            jsonReport.put(KEY_CHECK_NAME, report.getCheckName());
        }

        if (report.getViolations() != null) {
            JSONArray jsonViolations = new JSONArray(report.getViolations().stream()
                    .map(ReportMapper::violationToJSON)
                    .collect(Collectors.toList()));
            jsonReport.put(KEY_VIOLATIONS, jsonViolations);
        }

        return jsonReport;
    }

    @Deprecated
    private static JSONObject violationToJSON(Violation violation) throws JSONException {
        JSONObject jsonViolation = new JSONObject();
        if (violation.getSeverity() != null) {
            jsonViolation.put(KEY_SEVERITY, violation.getSeverity());
        }

        if (violation.getDescription() != null) {
            jsonViolation.put(KEY_DESCRIPTION, violation.getDescription());
        }

        if (violation.getPackages() != null) {
            JSONArray vPackages = new JSONArray(violation.getPackages().stream()
                    .map(p -> p.toString())
                    .collect(Collectors.toList()));
            jsonViolation.put(KEY_PACKAGES, vPackages);
        }

        return jsonViolation;
    }

}
