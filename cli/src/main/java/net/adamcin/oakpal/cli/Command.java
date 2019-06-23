package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.inferTest1;
import static net.adamcin.oakpal.core.Fun.result1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.Chart;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.DefaultErrorListener;
import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Violation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(Command.class);
    private static final String NO_OPT_PREFIX = "--no-";
    private static final Integer EXIT_GENERAL_ERROR = 1;
    private static final Integer EXIT_ABORTED_SCAN = 9;
    private static final Integer EXIT_SEVERE_VIOLATION = 10;
    private static final Integer EXIT_MAJOR_VIOLATION = 11;
    private static final Integer EXIT_MINOR_VIOLATION = 12;
    private static final String VERSION_PROPERTIES_NAME =
            Command.class.getPackage().getName() + "/version.properties";
    private static final String COMMAND_HELP_TXT =
            Command.class.getPackage().getName() + "/help.txt";

    IO<Integer> perform(final @NotNull Console console, final @NotNull String[] args) {
        final Result<Options> optsResult = parseArgs(console, args);
        if (optsResult.getError().isPresent()) {
            return console.printLineErr(optsResult.getError().get().getMessage())
                    .add(printHelp(console::printLineErr))
                    .add(IO.unit(EXIT_GENERAL_ERROR));
        } else {
            final Options opts = optsResult.getOrElse(Options.DEFAULT_OPTIONS);

            if (opts.isJustHelp()) {
                return printHelp(console::printLine).add(IO.unit(0));
            } else if (opts.isJustVersion()) {
                return printVersion(console::printLine).add(IO.unit(0));
            }

            final ClassLoader cl = Loader.getPathClassLoader(console,
                    Command.class.getClassLoader());

            final URL chartUrl = Loader.findChartLocation(console, opts.getChartName());
            final Result<List<CheckReport>> scanResult = Chart.fromJson(chartUrl)
                    .flatMap(result1(chart ->
                            chart.toOakMachineBuilder(new DefaultErrorListener(), cl)))
                    .map(OakMachine.Builder::build).flatMap(oak -> runOakScan(opts, oak));

            if (scanResult.getError().isPresent()) {
                return console.printLineErr(scanResult.getError().get().getMessage())
                        .add(IO.unit(EXIT_ABORTED_SCAN));
            } else {
                final List<CheckReport> reports = scanResult.getOrElse(Collections.emptyList());
                final Optional<Integer> highestSeverity = reports.stream()
                        .flatMap(compose(CheckReport::getViolations, Collection::stream))
                        .map(Violation::getSeverity)
                        .reduce(Violation.Severity::maxSeverity)
                        .filter(inferTest1(opts.getFailOnSeverity().meetsMinimumSeverity()))
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
                return printReports(reports, opts.getPrinter()).add(IO.unit(highestSeverity.orElse(0)));
            }
        }
    }

    IO<Nothing> printReports(final @NotNull List<CheckReport> reports,
                             final @NotNull Function<StructuredMessage, IO<Nothing>> linePrinter) {
        return reports.stream().map(compose(ReportMessage::new, linePrinter)).reduce(IO.empty, IO::add);
    }

    IO<Nothing> printHelp(final @NotNull Function<Object, IO<Nothing>> linePrinter) {
        try (InputStream input = Command.class.getClassLoader().getResourceAsStream(COMMAND_HELP_TXT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().map(linePrinter).reduce(IO.empty, IO::add);
        } catch (IOException e) {
            throw new RuntimeException("failed to read version.properties", e);
        }
    }

    IO<Nothing> printVersion(final @NotNull Function<Object, IO<Nothing>> linePrinter) {
        final Properties properties = new Properties();
        try (InputStream input = Command.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES_NAME)) {
            properties.load(input);
            return linePrinter.apply("OakPAL CLI " + properties.getProperty("version"));
        } catch (IOException e) {
            throw new RuntimeException("failed to read version.properties", e);
        }
    }

    Result<List<CheckReport>> runOakScan(final @NotNull Options opts, final @NotNull OakMachine oak) {
        try {
            return Result.success(oak.scanPackages(opts.getScanFiles()));
        } catch (AbortedScanException e) {
            return Result.failure(e);
        }
    }

    @NotNull Result<Options> parseArgs(final @NotNull Console console, final @NotNull String[] args) {
        Options.Builder builder = new Options.Builder();
        for (int i = 0; i < args.length; i++) {
            final String wholeOpt = args[i];
            final boolean isNoOpt = wholeOpt.startsWith(NO_OPT_PREFIX);
            final String opt = isNoOpt ? wholeOpt.substring(NO_OPT_PREFIX.length()) : wholeOpt;

            switch (opt) {
                case "-h":
                case "--help":
                    builder.setJustHelp(!isNoOpt);
                    break;
                case "-v":
                case "--version":
                    builder.setJustVersion(!isNoOpt);
                    break;
                case "-o":
                case "--outfile":
                    builder.setOutFile(isNoOpt ? null : new File(console.getCwd(), args[++i]));
                    break;
                case "-j":
                case "--json":
                    builder.setOutputJson(!isNoOpt);
                    break;
                case "-n":
                case "--chart":
                    builder.setChartName(isNoOpt ? null : args[++i]);
                    break;
                case "-f":
                case "--fail-severity":
                    final String severityArg = args[++i];
                    final Result<Violation.Severity> severityResult = result1(Violation.Severity::byName)
                            .apply(severityArg);
                    if (severityResult.getError().isPresent()) {
                        return Result.failure(severityResult.getError().get());
                    }
                    severityResult.stream().forEach(builder::setFailOnSeverity);
                    break;
                default:
                    final File scanFile = new File(console.getCwd(), wholeOpt);
                    if (!scanFile.isFile()) {
                        return Result.failure(String.format("%s is not a file.", scanFile.getAbsolutePath()));
                    }
                    builder.addScanFile(scanFile);
                    break;
            }
        }
        return builder.build(console);
    }
}
