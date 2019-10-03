package net.adamcin.oakpal.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.JsonObject;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;

class ReportMessage implements StructuredMessage {
    private final CheckReport report;

    ReportMessage(final @NotNull CheckReport report) {
        this.report = report;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        try (PrintWriter writer = new PrintWriter(sw)) {
            writer.println(String.format("report: %s", String.valueOf(report.getCheckName())));
            for (Violation v : report.getViolations()) {
                final Set<String> packageIds = v.getPackages().stream().map(PackageId::getDownloadName)
                        .collect(Collectors.toSet());
                final String violLog = !packageIds.isEmpty()
                        ? String.format(" +- <%s> %s %s", v.getSeverity(), v.getDescription(), packageIds)
                        : String.format(" +- <%s> %s", v.getSeverity(), v.getDescription());
                writer.println(violLog);
            }
        }
        return sw.toString().trim();
    }

    @Override
    public JsonObject toJson() {
        return report.toJson();
    }
}
