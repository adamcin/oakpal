package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.api.Nothing;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.DefaultErrorListener;
import net.adamcin.oakpal.core.FileBlobMemoryNodeStore;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.OakpalPlan;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.inferTest1;
import static net.adamcin.oakpal.api.Fun.result0;
import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.api.Fun.uncheck0;

final class Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(Command.class);
    private static final String SHORT_NO_OPT_PREFIX = "+";
    private static final String LONG_NO_OPT_PREFIX = "--no-";
    private static final String VERSION_PROPERTIES_NAME = "version.properties";
    private static final String COMMAND_HELP_TXT = "help.txt";
    static final Integer EXIT_GENERAL_ERROR = 1;
    static final Integer EXIT_ABORTED_SCAN = 9;
    static final Integer EXIT_SEVERE_VIOLATION = 10;
    static final Integer EXIT_MAJOR_VIOLATION = 11;
    static final Integer EXIT_MINOR_VIOLATION = 12;

    IO<Integer> perform(final @NotNull Console console, final @NotNull String[] args) {
        final Result<Options> optsResult = parseArgs(console, args);
        if (optsResult.isFailure()) {
            return console.printLineErr(optsResult.getError().get().getMessage())
                    .add(printHelp(console::printLineErr))
                    .add(IO.unit(EXIT_GENERAL_ERROR));
        } else {
            final Options opts = optsResult.getOrDefault(Options.DEFAULT_OPTIONS);

            if (opts.isJustHelp()) {
                return printHelp(console::printLine).add(IO.unit(0));
            } else if (opts.isJustVersion()) {
                return printVersion(console::printLine).add(IO.unit(0));
            } else {
                return doScan(console, opts);
            }
        }
    }

    Supplier<NodeStore> getNodeStoreSupplier(final @NotNull Options opts) {
        if (opts.isStoreBlobs()) {
            return () -> new FileBlobMemoryNodeStore(
                    opts.getCacheDir().toPath().resolve("blobs").toFile().getAbsolutePath());
        } else {
            return MemoryNodeStore::new;
        }
    }

    IO<Integer> doScan(final @NotNull Console console, final @NotNull Options opts) {
        final ClassLoader cl = opts.getScanClassLoader();
        final URL planUrl = opts.getPlanUrl();

        /* ------------ */
        /* perform scan */
        /* ------------ */
        final Result<List<CheckReport>> scanResult = OakpalPlan.fromJson(planUrl)
                .flatMap(result1(plan ->
                        opts.applyOverrides(plan).toOakMachineBuilder(new DefaultErrorListener(), cl)
                                .withNodeStoreSupplier(getNodeStoreSupplier(opts))))
                .map(OakMachine.Builder::build).flatMap(oak -> runOakScan(opts, oak));

        if (scanResult.isFailure()) {
            return console.printLineErr(scanResult.teeLogError().getError().get().getMessage())
                    .add(IO.unit(EXIT_ABORTED_SCAN));
        } else {
            final List<CheckReport> reports = scanResult.getOrDefault(Collections.emptyList());
            final Optional<Integer> highestSeverity = getHighestReportSeverity(opts, reports);
            return printReports(reports, opts.getPrinter()).add(IO.unit(highestSeverity.orElse(0)));
        }
    }

    Result<List<CheckReport>> runOakScan(final @NotNull Options opts, final @NotNull OakMachine oak) {
        return result0(() -> oak.scanPackages(opts.getScanFiles())).get();
    }

    Optional<Integer> getHighestReportSeverity(final @NotNull Options opts,
                                               final @NotNull List<CheckReport> reports) {
        return reports.stream()
                .flatMap(compose1(CheckReport::getViolations, Collection::stream))
                .map(Violation::getSeverity)
                .reduce(Severity::maxSeverity)
                .filter(opts.getFailOnSeverity().meetsMinimumSeverity())
                .map(severity -> {
                    switch (severity) {
                        case SEVERE:
                            return EXIT_SEVERE_VIOLATION;
                        case MAJOR:
                            return EXIT_MAJOR_VIOLATION;
                        case MINOR:
                        default:
                            return EXIT_MINOR_VIOLATION;
                    }
                });
    }

    IO<Nothing> printReports(final @NotNull List<CheckReport> reports,
                             final @NotNull Function<StructuredMessage, IO<Nothing>> linePrinter) {
        return linePrinter.apply(new AllReportsMessage(reports));
    }

    IO<Nothing> printHelp(final @NotNull Function<Object, IO<Nothing>> linePrinter) {
        return uncheck0(() -> {
            try (InputStream input = Command.class.getResourceAsStream(COMMAND_HELP_TXT);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().map(linePrinter).reduce(IO.empty, IO::add);
            }
        }).get();
    }

    IO<Nothing> printVersion(final @NotNull Function<Object, IO<Nothing>> linePrinter) {
        return uncheck0(() -> {
            final Properties properties = new Properties();
            try (InputStream input = Command.class.getResourceAsStream(VERSION_PROPERTIES_NAME)) {
                properties.load(input);
                return linePrinter.apply("OakPAL CLI " + properties.getProperty("version"));
            }
        }).get();
    }

    Optional<String> flipOpt(final @NotNull String wholeOpt) {
        if (wholeOpt.startsWith(LONG_NO_OPT_PREFIX)) {
            return Optional.of("--" + wholeOpt.substring(LONG_NO_OPT_PREFIX.length()));
        } else if (wholeOpt.startsWith(SHORT_NO_OPT_PREFIX)) {
            return Optional.of("-" + wholeOpt.substring(SHORT_NO_OPT_PREFIX.length()));
        } else {
            return Optional.empty();
        }
    }

    @NotNull Result<Options> parseArgs(final @NotNull Console console, final @NotNull String[] args) {
        Options.Builder builder = new Options.Builder();
        for (int i = 0; i < args.length; i++) {
            final String wholeOpt = args[i];
            final Optional<String> flipped = flipOpt(wholeOpt);
            final boolean isNoOpt = flipped.isPresent();
            final String opt = flipped.orElse(wholeOpt);

            switch (opt) {
                case "-h":
                case "--help":
                    builder.setJustHelp(!isNoOpt);
                    break;
                case "-v":
                case "--version":
                    builder.setJustVersion(!isNoOpt);
                    break;
                case "-b":
                case "--store-blobs":
                    builder.setStoreBlobs(!isNoOpt);
                    break;
                case "-f":
                case "--file":
                    builder.setOpearFile(isNoOpt ? null : console.getCwd().toPath().resolve(args[++i]).toFile());
                    break;
                case "-c":
                case "--cache":
                    builder.setCacheDir(isNoOpt ? null : console.getCwd().toPath().resolve(args[++i]).toFile());
                    break;
                case "-o":
                case "--outfile":
                    builder.setOutFile(isNoOpt ? null : console.getCwd().toPath().resolve(args[++i]).toFile());
                    break;
                case "-j":
                case "--json":
                    builder.setOutputJson(!isNoOpt);
                    break;
                case "--hooks":
                    builder.setNoHooks(isNoOpt);
                    break;
                case "-p":
                case "--plan":
                    builder.setNoPlan(isNoOpt);
                    builder.setPlanName(isNoOpt ? null : args[++i]);
                    break;
                case "-pf":
                case "--plan-file":
                    builder.setPlanFile(isNoOpt ? null : console.getCwd().toPath().resolve(args[++i]).toFile());
                    break;
                case "--plan-file-base":
                    builder.setPlanFileBaseDir(isNoOpt ? null : console.getCwd().toPath().resolve(args[++i]).toFile());
                    break;
                case "-pi":
                case "--pre-install-file":
                    if (isNoOpt) {
                        builder.setPreInstallFiles(Collections.emptyList());
                    } else {
                        builder.addPreInstallFile(console.getCwd().toPath().resolve(args[++i]).toFile());
                    }
                    break;
                case "-ri":
                case "--repoinit-file":
                    if (isNoOpt) {
                        builder.setRepoInitFiles(Collections.emptyList());
                    } else {
                        builder.addRepoInitFile(console.getCwd().toPath().resolve(args[++i]).toFile());
                    }
                    break;
                case "-r":
                case "--run-modes":
                    builder.setNoRunModes(isNoOpt);
                    if (isNoOpt) {
                        builder.setRunModes(Collections.emptyList());
                    } else {
                        builder.setRunModes(Stream.of(args[++i].split(","))
                                .map(String::trim)
                                .filter(inferTest1(String::isEmpty).negate())
                                .collect(Collectors.toList()));
                    }
                    break;
                case "-xp":
                case "--extend-classpath":
                    if (isNoOpt) {
                        builder.setExtendedClassPathFiles(Collections.emptyList());
                    } else {
                        builder.addExtendedClassPathFile(console.getCwd().toPath().resolve(args[++i]).toFile());
                    }
                    break;
                case "-s":
                case "--severity-fail":
                    if (isNoOpt) {
                        builder.setFailOnSeverity(null);
                        break;
                    } else {
                        final String severityArg = args[++i];
                        final Result<Severity> severityResult = result1(Severity::byName)
                                .apply(severityArg);
                        if (severityResult.isFailure()) {
                            return Result.failure(severityResult.getError().get());
                        }
                        severityResult.forEach(builder::setFailOnSeverity);
                        break;
                    }
                default:
                    final File scanFile = console.getCwd().toPath().resolve(wholeOpt).toFile();
                    if (!scanFile.isFile()) {
                        return Result.failure(String.format("%s is not a file.", wholeOpt));
                    }
                    builder.addScanFile(scanFile);
                    break;
            }
        }
        return builder.build(console);
    }
}
