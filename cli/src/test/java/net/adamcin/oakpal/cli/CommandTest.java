package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.result0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.Nothing;
import net.adamcin.oakpal.core.SimpleReport;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTest.class);

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

        List<JsonObject> readObjects = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(jsonOutput))) {
            reader.lines()
                    .map(line -> Json.createReader(new StringReader(line)).readObject())
                    .forEachOrdered(readObjects::add);
        } catch (IOException e ) {
            // shouldn't happen
        }
        assertEquals("should read same number of objects as there are reports",
                reports.size(), readObjects.size());

        List<SimpleReport> readReports = readObjects.stream().map(SimpleReport::fromJson).collect(Collectors.toList());
        for (int i = 0; i < reports.size(); i++) {
            assertEquals("read report should match input report at index " + i,
                    reports.get(i), readReports.get(i));
        }
    }

    String captureOutput(final @NotNull BiFunction<Command, Function<Object, IO<Nothing>>, IO<Nothing>> commandStrategy) {
        final Command command = new Command();
        final StringWriter sw = new StringWriter();
        try (PrintWriter writer = new PrintWriter(sw)) {
            commandStrategy.apply(command, message -> IO.empty.flatMap(nothing -> {
                writer.println(message);
                return IO.empty;
            })).get();
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
