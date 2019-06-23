package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.result1;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Violation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Options {
    static final Options DEFAULT_OPTIONS = new Options();
    private final boolean justHelp;
    private final boolean justVersion;
    private final String chartName;
    private final List<File> scanFiles;
    private final Function<StructuredMessage, IO<Nothing>> printer;
    private final Violation.Severity failOnSeverity;

    Options() {
        this(true, true, null,
                Collections.emptyList(),
                message -> IO.empty,
                Violation.Severity.MAJOR);
    }

    Options(final boolean justHelp,
            final boolean justVersion,
            final @Nullable String chartName,
            final @NotNull List<File> scanFiles,
            final @NotNull Function<StructuredMessage, IO<Nothing>> printer,
            final @NotNull Violation.Severity failOnSeverity) {
        this.justHelp = justHelp;
        this.justVersion = justVersion;
        this.chartName = chartName;
        this.scanFiles = scanFiles;
        this.printer = printer;
        this.failOnSeverity = failOnSeverity;
    }

    public boolean isJustHelp() {
        return justHelp;
    }

    public boolean isJustVersion() {
        return justVersion;
    }

    public String getChartName() {
        return chartName;
    }

    public List<File> getScanFiles() {
        return scanFiles;
    }

    public Function<StructuredMessage, IO<Nothing>> getPrinter() {
        return printer;
    }

    public Violation.Severity getFailOnSeverity() {
        return failOnSeverity;
    }

    static final class Builder {
        private boolean justHelp;
        private boolean justVersion;
        private boolean outputJson;
        private String chartName;
        private File outFile;
        private List<File> scanFiles = new ArrayList<>();
        private Violation.Severity failOnSeverity = Violation.Severity.MAJOR;

        public Builder setJustHelp(final boolean justHelp) {
            this.justHelp = justHelp;
            return this;
        }

        public Builder setJustVersion(final boolean justVersion) {
            this.justVersion = justVersion;
            return this;
        }

        public Builder setOutputJson(final boolean outputJson) {
            this.outputJson = outputJson;
            return this;
        }

        public Builder setChartName(final @Nullable String chartName) {
            this.chartName = chartName;
            return this;
        }

        public Builder setOutFile(final @Nullable File outFile) {
            this.outFile = outFile;
            return this;
        }

        public Builder addScanFile(final @NotNull File scanFile) {
            this.scanFiles.add(scanFile);
            return this;
        }

        public Builder setFailOnSeverity(final @NotNull Violation.Severity failOnSeverity) {
            this.failOnSeverity = failOnSeverity;
            return this;
        }

        Result<Options> build(final @NotNull Console console) {
            return messageWriter(console, outputJson, outFile).map(writer ->
                    new Options(justHelp, justVersion, chartName, scanFiles, writer, failOnSeverity));
        }
    }

    /**
     * Writes a structured message to the appropriate stream in the appropriate format.
     *
     * @param outputJson whether to output json or not
     * @return a nothing IO monad.
     */
    final @NotNull
    static Result<Function<StructuredMessage, IO<Nothing>>> messageWriter(final @NotNull Console console,
                                                                          final boolean outputJson,
                                                                          final @Nullable File outFile) {
        final Function<StructuredMessage, Object> objectifier = outputJson
                ? StructuredMessage::toJson : message -> message;
        final Result<Function<Object, IO<Nothing>>> printerResult = Optional.ofNullable(outFile)
                .map(result1(file -> (Function<Object, IO<Nothing>>) console.openPrinter(file)))
                .orElse(Result.success(console::printLine));
        return printerResult.map(objectifier::andThen);
    }

}
