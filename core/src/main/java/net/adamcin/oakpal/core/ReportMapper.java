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

import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfObjects;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.core.JavaxJson.optArray;
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
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonCollectors;
import javax.json.stream.JsonGenerator;

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

    /**
     * Functional interface that indicates that the consuming method will open AND close the stream for reading.
     */
    @FunctionalInterface
    public interface ReaderSupplier {
        Reader open() throws IOException;
    }

    /**
     * Functional interface that indicates that the consuming method will open AND close the stream for writing.
     */
    @FunctionalInterface
    public interface WriterSupplier {
        Writer open() throws IOException;
    }

    public static List<CheckReport> readReports(final ReaderSupplier readerSupplier) throws IOException {
        try (Reader reader = readerSupplier.open();
             JsonReader jsonReader = Json.createReader(reader)) {

            JsonObject json = jsonReader.readObject();

            List<CheckReport> reports = optArray(json, KEY_REPORTS)
                    .map(jsonReports -> mapArrayOfObjects(jsonReports, ReportMapper::reportFromJson))
                    .orElseGet(Collections::emptyList);

            return Collections.unmodifiableList(reports);
        }
    }

    public static List<CheckReport> readReportsFromFile(final File jsonFile)
            throws IOException {
        return readReports(() -> new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8));
    }

    public static void writeReports(final Collection<CheckReport> reports, final WriterSupplier writerSupplier) throws IOException {
        JsonWriterFactory writerFactory = Json
                .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        try (Writer writer = writerSupplier.open(); JsonWriter jsonWriter = writerFactory.createWriter(writer)) {
            jsonWriter.writeObject(key(KEY_REPORTS, reportsToJson(reports)).get());
        }
    }

    public static void writeReportsToFile(final Collection<CheckReport> reports, final File outputFile) throws IOException {
        writeReports(reports, () -> new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
    }

    @Deprecated
    public static List<CheckReport> readReportsFromStream(final ReaderSupplier readerSupplier) throws IOException {
        return readReports(readerSupplier);
    }

    static CheckReport reportFromJson(final JsonObject jsonReport) {
        return SimpleReport.fromJson(jsonReport);
    }

    static Violation violationFromJson(final JsonObject jsonViolation) {
        return SimpleViolation.fromJson(jsonViolation);
    }

    /**
     * Transforms a collection of CheckReports to a JsonArray
     *
     * @param reports the reports to serialize
     * @return a JsonArray of CheckReport json objects
     * @deprecated 1.2.2 CheckReport and Violation are now JSON serializable
     */
    @Deprecated
    public static JsonArray reportsToJson(final Collection<CheckReport> reports) {
        return reports.stream()
                .map(CheckReport::toJson)
                .collect(JsonCollectors.toJsonArray());
    }

    @Deprecated
    static JsonObject reportToJson(final CheckReport report) {
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

    static JsonObject violationToJson(final Violation violation) {
        return violation.toJson();
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
