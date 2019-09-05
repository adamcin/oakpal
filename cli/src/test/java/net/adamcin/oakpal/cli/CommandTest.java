package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.uncheck0;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.swing.text.html.Option;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.FileBlobMemoryNodeStore;
import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.ReportCollector;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.SimpleReport;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTest.class);

    final File testOutputBaseDir = new File("target/test-output/CommandTest");

    @Before
    public void setUp() throws Exception {
        testOutputBaseDir.mkdirs();
    }

    @Test
    public void testPrintHelp() {
        final String output = captureOutput(Command::printHelp);
        LOGGER.info("help: \n{}", output);
        assertFalse("help should not be empty", output.isEmpty());
    }

    @Test
    public void testPrintVersion() {
        final String output = captureOutput(Command::printVersion);
        LOGGER.info("version: \n{}", output);
        assertFalse("version should not be empty", output.isEmpty());
    }

    @Test
    public void testGetNodeStoreSupplier() {
        final Command command = new Command();
        final Console console = getMockConsole();
        assertTrue("is FileBlobMemoryNodeStore",
                command.getNodeStoreSupplier(
                        new Options.Builder()
                                .build(console)
                                .getOrDefault(null)).get() instanceof FileBlobMemoryNodeStore);
        assertTrue("is MemoryNodeStore",
                command.getNodeStoreSupplier(
                        new Options.Builder()
                                .setNoCacheBlobs(true)
                                .build(console)
                                .getOrDefault(null)).get() instanceof MemoryNodeStore);
    }

    @Test
    public void testWriteReports() {
        final List<CheckReport> reports = new ArrayList<>();
        reports.add(new SimpleReport("some check", Collections.emptyList()));
        reports.add(new SimpleReport("check with violations", Arrays.asList(
                new SimpleViolation(Violation.Severity.MINOR, "minor violation"),
                new SimpleViolation(Violation.Severity.SEVERE, "severe violation with one packageId",
                        PackageId.fromString("my_packages/acme/1.0")),
                new SimpleViolation(Violation.Severity.MAJOR, "major violation with several packageIds",
                        PackageId.fromString("my_packages/alpha/1.0"),
                        PackageId.fromString("my_packages/beta/1.0"),
                        PackageId.fromString("my_packages/gamma/1.0"))
        )));

        final String output = captureStructured(false, (command, printer) ->
                command.printReports(reports, printer));
        LOGGER.info("reports: \n{}", output);
        assertFalse("reports should not be empty", output.isEmpty());

        final String jsonOutput = captureStructured(true, (command, printer) ->
                command.printReports(reports, printer));
        LOGGER.info("json reports: \n{}", jsonOutput);
        assertFalse("json reports should not be empty", jsonOutput.isEmpty());

        List<CheckReport> readReports = uncheck0(() ->
                ReportMapper.readReports(() -> new BufferedReader(new StringReader(jsonOutput)))).get();
        for (int i = 0; i < reports.size(); i++) {
            assertEquals("read report should match input report at index " + i,
                    reports.get(i), readReports.get(i));
        }
    }

    private Console getMockConsole() {
        final Console console = mock(Console.class);
        doCallRealMethod().when(console).getCwd();
        doCallRealMethod().when(console).getEnv();
        doCallRealMethod().when(console).getSystemProperties();
        return console;
    }

    @Test
    public void testGetHighestReportSeverity() {
        ReportCollector collector1 = new ReportCollector();
        collector1.reportViolation(new SimpleViolation(Violation.Severity.MINOR, ""));
        collector1.reportViolation(new SimpleViolation(Violation.Severity.MAJOR, ""));
        CheckReport hasMajor = new SimpleReport("hasMajor", collector1.getReportedViolations());
        ReportCollector collector2 = new ReportCollector();
        collector2.reportViolation(new SimpleViolation(Violation.Severity.MINOR, ""));
        collector2.reportViolation(new SimpleViolation(Violation.Severity.SEVERE, ""));
        CheckReport hasSevere = new SimpleReport("hasSevere", collector2.getReportedViolations());
        ReportCollector collector3 = new ReportCollector();
        collector3.reportViolation(new SimpleViolation(Violation.Severity.MINOR, ""));
        CheckReport hasMinor = new SimpleReport("hasMinor", collector3.getReportedViolations());
        final Console console = getMockConsole();
        Options optsFailDefault = new Options.Builder()
                .build(console).getOrDefault(null);
        Options optsFailMinor = new Options.Builder()
                .setFailOnSeverity(Violation.Severity.MINOR)
                .build(console).getOrDefault(null);
        Options optsFailMajor = new Options.Builder()
                .setFailOnSeverity(Violation.Severity.MAJOR)
                .build(console).getOrDefault(null);
        Options optsFailSevere = new Options.Builder()
                .setFailOnSeverity(Violation.Severity.SEVERE)
                .build(console).getOrDefault(null);
        final Command command = new Command();
        assertFalse("no exit minor default fail",
                command.getHighestReportSeverity(optsFailDefault, Collections.singletonList(hasMinor)).isPresent());
        assertEquals("major exit default fail", Command.EXIT_MAJOR_VIOLATION,
                command.getHighestReportSeverity(optsFailDefault, Collections.singletonList(hasMajor)).get());
        assertEquals("severe exit default fail", Command.EXIT_SEVERE_VIOLATION,
                command.getHighestReportSeverity(optsFailDefault, Collections.singletonList(hasSevere)).get());

        assertEquals("minor exit minor fail", Command.EXIT_MINOR_VIOLATION,
                command.getHighestReportSeverity(optsFailMinor, Collections.singletonList(hasMinor)).get());
        assertEquals("major exit minor fail", Command.EXIT_MAJOR_VIOLATION,
                command.getHighestReportSeverity(optsFailMinor, Collections.singletonList(hasMajor)).get());
        assertEquals("severe exit minor fail", Command.EXIT_SEVERE_VIOLATION,
                command.getHighestReportSeverity(optsFailMinor, Collections.singletonList(hasSevere)).get());

        assertFalse("no exit minor major fail",
                command.getHighestReportSeverity(optsFailMajor, Collections.singletonList(hasMinor)).isPresent());
        assertEquals("major exit major fail", Command.EXIT_MAJOR_VIOLATION,
                command.getHighestReportSeverity(optsFailMajor, Collections.singletonList(hasMajor)).get());
        assertEquals("severe exit major fail", Command.EXIT_SEVERE_VIOLATION,
                command.getHighestReportSeverity(optsFailMajor, Collections.singletonList(hasSevere)).get());

        assertFalse("no exit minor severe fail",
                command.getHighestReportSeverity(optsFailSevere, Collections.singletonList(hasMinor)).isPresent());
        assertFalse("no exit major severe fail",
                command.getHighestReportSeverity(optsFailSevere, Collections.singletonList(hasMajor)).isPresent());
        assertEquals("severe exit major fail", Command.EXIT_SEVERE_VIOLATION,
                command.getHighestReportSeverity(optsFailSevere, Collections.singletonList(hasSevere)).get());

        assertFalse("no exit minor default fail",
                command.getHighestReportSeverity(optsFailDefault, Arrays.asList(hasMinor, hasMinor)).isPresent());
        assertEquals("minor exit", Command.EXIT_MAJOR_VIOLATION,
                command.getHighestReportSeverity(optsFailDefault, Arrays.asList(hasMinor, hasMajor)).get());
        assertEquals("major exit", Command.EXIT_MAJOR_VIOLATION,
                command.getHighestReportSeverity(optsFailDefault, Arrays.asList(hasMajor, hasMinor)).get());
        assertEquals("major exit", Command.EXIT_MAJOR_VIOLATION,
                command.getHighestReportSeverity(optsFailDefault, Arrays.asList(hasMajor, hasMajor)).get());
        assertEquals("severe exit", Command.EXIT_SEVERE_VIOLATION,
                command.getHighestReportSeverity(optsFailDefault, Arrays.asList(hasSevere, hasMajor)).get());
    }

    private static class OptionsValidator {
        final Console console;

        OptionsValidator(final @NotNull Console console) {
            this.console = console;
        }

        private void expectFailure(final @NotNull String[] args) {
            final Command command = new Command();
            assertFalse("args should fail: " + Arrays.toString(args),
                    command.parseArgs(console, args).isSuccess());
        }

        private void expectSuccess(final @NotNull String[] args,
                                   final @NotNull Consumer<Options> optionsConsumer) {
            final Command command = new Command();
            final Result<Options> result = command.parseArgs(console, args);
            assertTrue("args should succeed", result.isSuccess());
            result.forEach(optionsConsumer);
        }
    }

    private String[] args(final String... arg) {
        return arg;
    }

    @Test
    public void testParseArgs_simpleOnes() {
        final Console console = getMockConsole();
        final OptionsValidator validator = new OptionsValidator(console);

        validator.expectSuccess(args(), options -> {
            assertFalse("not just help", options.isJustHelp());
            assertFalse("not just version", options.isJustVersion());
        });

        validator.expectSuccess(args("--help"),
                options -> assertTrue("is just help", options.isJustHelp()));
        validator.expectSuccess(args("--no-help"),
                options -> assertFalse("is not just help", options.isJustHelp()));
        validator.expectSuccess(args("--help", "--no-help"),
                options -> assertFalse("is not just help", options.isJustHelp()));
        validator.expectSuccess(args("-h"),
                options -> assertTrue("is just help", options.isJustHelp()));

        validator.expectSuccess(args("--version"),
                options -> assertTrue("is just version", options.isJustVersion()));
        validator.expectSuccess(args("--no-version"),
                options -> assertFalse("is not just version", options.isJustVersion()));
        validator.expectSuccess(args("--version", "--no-version"),
                options -> assertFalse("is not just version", options.isJustVersion()));
        validator.expectSuccess(args("-v"),
                options -> assertTrue("is just version", options.isJustVersion()));

        validator.expectSuccess(args(),
                options -> assertFalse("is not noCacheBlobs", options.isNoCacheBlobs()));
        validator.expectSuccess(args("--blobs"),
                options -> assertFalse("is not noCacheBlobs", options.isNoCacheBlobs()));
        validator.expectSuccess(args("--no-blobs"),
                options -> assertTrue("is noCacheBlobs", options.isNoCacheBlobs()));
        validator.expectSuccess(args("--no-blobs", "--blobs"),
                options -> assertFalse("is not noCacheBlobs", options.isNoCacheBlobs()));
        validator.expectSuccess(args("--no-blobs", "-b"),
                options -> assertFalse("is not noCacheBlobs", options.isNoCacheBlobs()));

        validator.expectFailure(args("-s", "extreme"));
        validator.expectSuccess(args(),
                options -> assertEquals("expect major by default",
                        Violation.Severity.MAJOR, options.getFailOnSeverity()));
        validator.expectSuccess(args("-s", "major"),
                options -> assertEquals("expect MAJOR",
                        Violation.Severity.MAJOR, options.getFailOnSeverity()));
        validator.expectSuccess(args("-s", "minor"),
                options -> assertEquals("expect MINOR",
                        Violation.Severity.MINOR, options.getFailOnSeverity()));
        validator.expectSuccess(args("-s", "severe"),
                options -> assertEquals("expect severe",
                        Violation.Severity.SEVERE, options.getFailOnSeverity()));

        validator.expectSuccess(args("--no-plan"),
                options -> assertNull("expect no plan", options.getPlanName()));
    }

    @Test
    public void testParseArgs_realOpear() throws Exception {
        final File testOutDir = new File(testOutputBaseDir, "testParseArgs_realOpear");
        FileUtils.deleteDirectory(testOutDir);
        final File simpleEchoSrc = new File("src/test/resources/opears/simpleEcho");
        final File simpleEchoJar = new File(testOutDir, "simpleEcho.jar");
        TestPackageUtil.buildJarFromDir(simpleEchoSrc, simpleEchoJar, Collections.emptyMap());

        final Console console = getMockConsole();
        final OptionsValidator validator = new OptionsValidator(console);

        validator.expectSuccess(args("--plan", "other-plan.json", "-f", simpleEchoJar.getAbsolutePath()),
                options -> assertEquals("expect plan name", "other-plan.json", options.getPlanName()));

        final File notAJar = new File(testOutDir, "notA.jar");
        FileUtils.touch(notAJar);
        validator.expectFailure(args("-f", notAJar.getAbsolutePath()));
    }

    @Test
    public void testParseArgs_outputs() throws Exception {
        final Console console = getMockConsole();
        final OptionsValidator validator = new OptionsValidator(console);

        final JsonObject valueObject = key("key", "value").get();
        final StructuredMessage testMessage = new StructuredMessage() {
            @Override
            public JsonObject toJson() {
                return valueObject;
            }

            @Override
            public String toString() {
                return "opaque";
            }
        };

        final Stack<Object> stdOutStack = new Stack<>();

        doAnswer(call -> {
            stdOutStack.push(call.getArgument(0));
            return IO.empty;
        }).when(console).printLine(any());

        final Stack<Object> fileStack = new Stack<>();

        final DisposablePrinter outFilePrinter = new DisposablePrinter() {
            @Override
            public void dispose() {
            }

            @Override
            public IO<Nothing> apply(Object o) {
                return () -> {
                    fileStack.push(o);
                    return Nothing.instance;
                };
            }
        };

        final File invalidOutFile = new File(testOutputBaseDir, "invalidOut.txt");
        FileUtils.touch(invalidOutFile);
        final File validOutFile = new File(testOutputBaseDir, "validOut.txt");
        FileUtils.touch(validOutFile);

        doAnswer(call -> {
            if (Files.isSameFile(validOutFile.toPath(),
                    call.<File>getArgument(0).toPath())) {
                return Result.success(outFilePrinter);
            } else {
                return Result.failure("not a file");
            }
        }).when(console).openPrinter(any(File.class));

        final Function<Stack<Object>, Consumer<Options>> expectJson =
                stack ->
                        options -> {
                            options.getPrinter().apply(testMessage).get();
                            assertFalse("output should not be empty", stack.isEmpty());
                            final Object line = stack.pop();
                            assertSame("is json: " + line, valueObject, line);
                        };

        final Function<Stack<Object>, Consumer<Options>> expectNotJson =
                stack ->
                        options -> {
                            options.getPrinter().apply(testMessage).get();
                            assertFalse("output should not be empty", stack.isEmpty());
                            final Object line = stack.pop();
                            assertSame("is not json: " + line, testMessage, line);
                        };

        validator.expectSuccess(args("--json"), expectJson.apply(stdOutStack));
        validator.expectSuccess(args("--no-json"), expectNotJson.apply(stdOutStack));
        validator.expectSuccess(args("--json", "--no-json"), expectNotJson.apply(stdOutStack));
        validator.expectSuccess(args("-j"), expectJson.apply(stdOutStack));

        validator.expectFailure(args("-o", invalidOutFile.getPath()));
        validator.expectFailure(args("-o", invalidOutFile.getPath(), "--json"));
        validator.expectSuccess(args("-o", validOutFile.getPath(), "--json"),
                expectJson.apply(fileStack));
        validator.expectSuccess(args("-o", validOutFile.getPath()),
                expectNotJson.apply(fileStack));
    }


    String captureOutput(final @NotNull BiFunction<Command, Function<Object, IO<Nothing>>, IO<Nothing>> commandStrategy) {
        final Command command = new Command();
        final StringWriter sw = new StringWriter();
        try (PrintWriter writer = new PrintWriter(sw)) {
            assertNotNull("commandStrategy", commandStrategy);
            assertNotNull("writer", writer);
            commandStrategy.apply(command, message -> IO.empty.flatMap(nothing -> {
                writer.println(message);
                return IO.empty;
            })).get();
        } catch (NullPointerException e) {
            LOGGER.error("wut", e);
            throw e;
        }
        return sw.toString();
    }

    String captureStructured(final boolean outputJson,
                             final @NotNull BiFunction<Command, Function<StructuredMessage, IO<Nothing>>, IO<Nothing>> commandStrategy) {
        final Command command = new Command();
        final StringWriter sw = new StringWriter();
        try (PrintWriter writer = new PrintWriter(sw)) {
            commandStrategy.apply(command, message -> IO.empty.flatMap(nothing -> {
                writer.println(outputJson ? message.toJson() : message);
                return IO.empty;
            })).get();
        }
        return sw.toString();
    }
}
