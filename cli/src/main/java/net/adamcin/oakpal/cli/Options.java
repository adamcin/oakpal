package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.result1;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarFile;

import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.OpearFile;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Violation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Options {
    static final Options DEFAULT_OPTIONS = new Options();
    private final boolean justHelp;
    private final boolean justVersion;
    private final URL planUrl;
    private final ClassLoader scanClassLoader;
    private final File opearFile;
    private final String planName;
    private final List<File> scanFiles;
    private final Function<StructuredMessage, IO<Nothing>> printer;
    private final Violation.Severity failOnSeverity;

    Options() {
        this(true, true,
                OakpalPlan.BASIC_PLAN_URL, Options.class.getClassLoader(),
                null, null,
                Collections.emptyList(),
                message -> IO.empty,
                Violation.Severity.MAJOR);
    }

    Options(final boolean justHelp,
            final boolean justVersion,
            final @NotNull URL planUrl,
            final @NotNull ClassLoader scanClassLoader,
            final @Nullable File opearFile,
            final @Nullable String planName,
            final @NotNull List<File> scanFiles,
            final @NotNull Function<StructuredMessage, IO<Nothing>> printer,
            final @NotNull Violation.Severity failOnSeverity) {
        this.justHelp = justHelp;
        this.justVersion = justVersion;
        this.planUrl = planUrl;
        this.scanClassLoader = scanClassLoader;
        this.opearFile = opearFile;
        this.planName = planName;
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

    public URL getPlanUrl() {
        return planUrl;
    }

    public ClassLoader getScanClassLoader() {
        return scanClassLoader;
    }

    public @Nullable File getOpearFile() {
        return opearFile;
    }

    public @Nullable String getPlanName() {
        return planName;
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
        private boolean noPlan;
        private String planName;
        private File outFile;
        private File opearFile;
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

        public Builder setNoPlan(final boolean noPlan) {
            this.noPlan = noPlan;
            return this;
        }

        public Builder setPlanName(final @Nullable String planName) {
            this.planName = planName;
            return this;
        }

        public Builder setOutFile(final @Nullable File outFile) {
            this.outFile = outFile;
            return this;
        }

        public Builder setOpearFile(final @Nullable File opearFile) {
            this.opearFile = opearFile;
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
            final File opearResolved = Optional.ofNullable(opearFile).orElseGet(() ->
                    new File(console.getCwd(),
                            console.getEnv().getOrDefault(Console.ENV_OAKPAL_OPEAR, ".")));

            final File cacheBaseDir = new File(console.getCwd(), ".opear-cache");
            cacheBaseDir.mkdirs();

            final Result<OpearFile> opearResult = Result.success(opearResolved).flatMap(file -> {
                if (file.isFile()) {
                    try (JarFile jarFile = new JarFile(opearResolved, true)) {
                        return OpearFile.fromJar(jarFile, cacheBaseDir);
                    } catch (IOException e) {
                        return Result.failure(String.format("%s is not a jar format file", opearResolved.getPath()), e);
                    }
                } else {
                    return OpearFile.fromDirectory(file);
                }
            });

            return opearResult.flatMap(opear ->
                    Optional.ofNullable(planName)
                            .map(opear::getSpecificPlan)
                            .orElse(Result.success(noPlan ? OakpalPlan.EMPTY_PLAN_URL : opear.getDefaultPlan()))
                            .flatMap(planUrl ->
                                    messageWriter(console, outputJson, outFile).map(writer ->
                                            new Options(justHelp, justVersion, planUrl,
                                                    opear.getPlanClassLoader(getClass().getClassLoader()), opearFile,
                                                    planName, scanFiles, writer, failOnSeverity))));
        }

    }

    /**
     * Writes a structured message to the appropriate stream in the appropriate format.
     *
     * @param outputJson whether to output json or not
     * @return a nothing IO monad.
     */
    static @NotNull Result<Function<StructuredMessage, IO<Nothing>>> messageWriter(final @NotNull Console console,
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