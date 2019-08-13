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
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonCollectors;
import javax.json.stream.JsonGenerator;

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
        /* No instantiation */
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

    /**
     * Opens a reader, reads a json object, closes the reader, and returns a list of reports.
     *
     * @param readerSupplier a function supplying a {@link Reader}
     * @return a list of check reports
     * @throws IOException for failing to read
     */
    public static List<CheckReport> readReports(final @NotNull ReaderSupplier readerSupplier) throws IOException {
        try (Reader reader = readerSupplier.open();
             JsonReader jsonReader = Json.createReader(reader)) {

            JsonObject json = jsonReader.readObject();

            List<CheckReport> reports = optArray(json, KEY_REPORTS)
                    .map(jsonReports -> mapArrayOfObjects(jsonReports, ReportMapper::reportFromJson))
                    .orElseGet(Collections::emptyList);

            return Collections.unmodifiableList(reports);
        }
    }

    /**
     * Read reports from a file and return a list of check reports.
     *
     * @param jsonFile a json file
     * @return a list of check reports
     * @throws IOException if fails to read a file
     */
    public static List<CheckReport> readReportsFromFile(final @NotNull File jsonFile)
            throws IOException {
        return readReports(() -> new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8));
    }

    public static void writeReports(final @NotNull Collection<CheckReport> reports,
                                    final @NotNull WriterSupplier writerSupplier) throws IOException {
        JsonWriterFactory writerFactory = Json
                .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        try (Writer writer = writerSupplier.open(); JsonWriter jsonWriter = writerFactory.createWriter(writer)) {
            jsonWriter.writeObject(key(KEY_REPORTS, reportsToJson(reports)).get());
        }
    }

    public static void writeReportsToFile(final Collection<CheckReport> reports,
                                          final @NotNull File outputFile) throws IOException {
        writeReports(reports, () -> new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
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
     */
    public static JsonArray reportsToJson(final @NotNull Collection<CheckReport> reports) {
        return reports.stream()
                .map(CheckReport::toJson)
                .collect(JsonCollectors.toJsonArray());
    }
}
