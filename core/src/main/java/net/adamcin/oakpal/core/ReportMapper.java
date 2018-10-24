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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONTokener;

/**
 * Serialize violations to/from json.
 */
public final class ReportMapper {
    public static final String KEY_REPORTS = "reports";
    public static final String KEY_CHECK_NAME = "reporterUrl";
    public static final String KEY_VIOLATIONS = "violations";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_SEVERITY = "severity";
    public static final String KEY_PACKAGES = "packages";

    private ReportMapper() {
        throw new RuntimeException("No instantiation");
    }

    public static List<CheckReport> readReportsFromFile(File jsonFile)
            throws IOException, JSONException {

        InputStream is = null;
        try {
            is = new FileInputStream(jsonFile);
            return readReportsFromStream(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static List<CheckReport> readReportsFromStream(InputStream inputStream) throws IOException, JSONException {
        Reader reader = new InputStreamReader(inputStream, "UTF-8");

        return readReportsFromReader(reader);
    }

    public static List<CheckReport> readReportsFromReader(Reader reader) throws IOException, JSONException {
        JSONTokener tokener = new JSONTokener(reader);
        JSONObject json = new JSONObject(tokener);

        List<CheckReport> reports = new ArrayList<>();
        JSONArray jsonReports = json.optJSONArray(KEY_REPORTS);

        if (jsonReports != null) {
            reports = StreamSupport.stream(jsonReports.spliterator(), false)
                    .filter(v -> v instanceof JSONObject)
                    .map(v -> reportFromJSON((JSONObject) v))
                    .collect(Collectors.toList());
        }

        return Collections.unmodifiableList(reports);
    }

    private static CheckReport reportFromJSON(JSONObject jsonReport) {
        String vCheckName = jsonReport.optString(KEY_CHECK_NAME);
        List<Violation> violations = new ArrayList<>();
        JSONArray vViolations = jsonReport.optJSONArray(KEY_VIOLATIONS);
        if (vViolations != null) {
            violations = StreamSupport.stream(vViolations.spliterator(), false)
                    .filter(v -> v instanceof JSONObject)
                    .map(v -> violationFromJSON((JSONObject) v))
                    .collect(Collectors.toList());
        }

        return new SimpleReport(vCheckName, violations);
    }

    private static Violation violationFromJSON(JSONObject jsonViolation) {
        String vSeverity = jsonViolation.optString(KEY_SEVERITY, Violation.Severity.MINOR.name());
        Violation.Severity severity = Violation.Severity.valueOf(vSeverity);
        String description = jsonViolation.optString(KEY_DESCRIPTION, "");
        List<PackageId> packages = new ArrayList<>();
        JSONArray vPackages = jsonViolation.optJSONArray(KEY_PACKAGES);
        if (vPackages != null) {
            packages = StreamSupport.stream(vPackages.spliterator(), false)
                    .filter(v -> v instanceof JSONString)
                    .map(v -> Optional.ofNullable(PackageId.fromString(v.toString())))
                    .filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());
        }

        return new SimpleViolation(severity, description, packages);
    }

    public static void writeReportsToFile(Collection<CheckReport> reports, File outputFile) throws IOException, JSONException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
            writeReportsToStream(reports, os);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public static void writeReportsToStream(Collection<CheckReport> reports, OutputStream outputStream) throws IOException, JSONException {
        Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
        writeReportsToWriter(reports, writer);
    }

    public static void writeReportsToWriter(Collection<CheckReport> reports, Writer writer) throws IOException, JSONException {
        JSONArray jsonReports = reportsToJSON(reports);
        JSONObject rootObj = new JSONObject();
        rootObj.put(KEY_REPORTS, jsonReports);
        rootObj.write(writer, 2, 0);
        writer.flush();
    }

    public static JSONArray reportsToJSON(Collection<CheckReport> reports) throws JSONException {
        return new JSONArray(reports.stream()
                .map(ReportMapper::reportToJSON)
                .collect(Collectors.toList()));
    }

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
