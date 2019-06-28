package net.adamcin.oakpal.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Violation;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OptionsTest {

    private File tempDir;

    @Before
    public void setUp() throws Exception {
        Path basePath = Paths.get("target/test-output");
        basePath.toFile().mkdirs();
        tempDir = Files.createTempDirectory(basePath, "OptionsTest").toFile();
        tempDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void testSettersAndSimpleBuild() {
        final Console console = mock(Console.class);
        Options.Builder builder = new Options.Builder();

        builder.setJustHelp(false);
        builder.setJustVersion(false);
        builder.setFailOnSeverity(Violation.Severity.MAJOR);
        builder.setPlanName(null);
        builder.setOutputJson(false);
        builder.setOutFile(null);

        final Result<Options> options = builder.build(console);
        assertFalse("options build is successful", options.getError().isPresent());
    }

    @Test
    public void testOutFile() throws Exception {
        final Console console = mock(Console.class);
        when(console.getCwd()).thenReturn(tempDir);
        Options.Builder builder = new Options.Builder().setOutputJson(true);
        final File outFile = new File(tempDir, "testOutFile.json");
        builder.setOutFile(outFile);
        final DisposablePrinter printer = new Main.DisposablePrinterImpl(new PrintWriter(outFile));

        when(console.openPrinter(outFile)).thenReturn(printer);
        final Result<Options> options = builder.build(console);
        options.stream().forEachOrdered(opts -> {
            opts.getPrinter().apply(() -> JavaxJson.key("message", "test").get()).get();
        });
        printer.dispose();

        try (Reader fileReader = new InputStreamReader(new FileInputStream(outFile), StandardCharsets.UTF_8);
             JsonReader reader = Json.createReader(fileReader)) {

            JsonObject json = reader.readObject();
            assertTrue("json should have 'message' key", json.containsKey("message"));
        }
    }
}
