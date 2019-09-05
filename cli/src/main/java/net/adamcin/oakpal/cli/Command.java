package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.inferTest1;
import static net.adamcin.oakpal.core.Fun.result0;
import static net.adamcin.oakpal.core.Fun.result1;
import static net.adamcin.oakpal.core.Fun.uncheck0;

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

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.DefaultErrorListener;
import net.adamcin.oakpal.core.FileBlobMemoryNodeStore;
import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(Command.class);
    private static final String NO_OPT_PREFIX = "--no-";
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
        if (opts.isNoCacheBlobs()) {
            return MemoryNodeStore::new;
        } else {
            return () -> new FileBlobMemoryNodeStore(
                    opts.getCacheDir().toPath().resolve("blobs").toFile().getAbsolutePath());
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
                        plan.toOakMachineBuilder(new DefaultErrorListener(), cl)
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
                .flatMap(compose(CheckReport::getViolations, Collection::stream))
                .map(Violation::getSeverity)
                .reduce(Violation.Severity::maxSeverity)
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

    @NotNull Result<Options> parseArgs(final @NotNull Console console, final @NotNull String[] args) {
        Options.Builder builder = new Options.Builder();
        for (int i = 0; i < args.length; i++) {
            final String wholeOpt = args[i];
            final boolean isNoOpt = wholeOpt.startsWith(NO_OPT_PREFIX);
            final String opt = isNoOpt ? "--" + wholeOpt.substring(NO_OPT_PREFIX.length()) : wholeOpt;

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
                case "--blobs":
                    builder.setNoCacheBlobs(isNoOpt);
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
                case "-p":
                case "--plan":
                    builder.setNoPlan(isNoOpt);
                    builder.setPlanName(isNoOpt ? null : args[++i]);
                    break;
                case "-s":
                case "--severity-fail":
                    final String severityArg = args[++i];
                    final Result<Violation.Severity> severityResult = result1(Violation.Severity::byName)
                            .apply(severityArg);
                    if (severityResult.isFailure()) {
                        return Result.failure(severityResult.getError().get());
                    }
                    severityResult.forEach(builder::setFailOnSeverity);
                    break;
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
