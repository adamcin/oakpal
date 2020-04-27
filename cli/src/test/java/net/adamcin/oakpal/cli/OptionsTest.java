package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private Console getMockConsole() {
        final Console console = mock(Console.class);
        doCallRealMethod().when(console).getCwd();
        doCallRealMethod().when(console).getEnv();
        doCallRealMethod().when(console).getSystemProperties();
        return console;
    }

    @Test
    public void testSettersAndSimpleBuild() {
        final Console console = getMockConsole();
        Options.Builder builder = new Options.Builder();

        builder.setJustHelp(false);
        builder.setJustVersion(false);
        builder.setStoreBlobs(false);
        builder.setFailOnSeverity(Severity.MAJOR);
        builder.setPlanName(null);
        builder.setOutputJson(false);
        builder.setOutFile(null);
        builder.setNoHooks(false);

        final Result<Options> options = builder.build(console);
        assertFalse("options build is successful", options.getError().isPresent());
    }

    @Test
    public void testOutFile() throws Exception {
        final Console console = getMockConsole();
        when(console.getCwd()).thenReturn(tempDir);
        Options.Builder builder = new Options.Builder().setOutputJson(true);
        final File outFile = new File(tempDir, "testOutFile.json");
        builder.setOutFile(outFile);
        final DisposablePrinter printer = new Main.DisposablePrinterImpl(new PrintWriter(outFile));

        when(console.openPrinter(outFile)).thenReturn(Result.success(printer));
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

    @Test
    public void testOpearFile() {
        final Console console = getMockConsole();
        when(console.getCwd()).thenReturn(tempDir);
        Options.Builder builder = new Options.Builder().setOpearFile(new File("src/test/resources/opears/emptyplan"));

        final Result<Options> optionsResult = builder.build(console);
        assertFalse("options build is successful", optionsResult.getError().isPresent());
        optionsResult.forEach(options ->
                assertTrue("opearFile should be a directory: " + options.getOpearFile().getAbsolutePath(),
                        options.getOpearFile().isDirectory()));
    }

    @Test
    public void testAdhocOpear() {
        final Console console = getMockConsole();
        when(console.getCwd()).thenReturn(tempDir);
        final Result<Options> defaultOptionsResult = new Options.Builder().build(console);
        assertFalse("options build is successful", defaultOptionsResult.getError().isPresent());
        defaultOptionsResult.forEach(options -> {
            assertNull("planFromFile is null by default", options.getPlanFile());
            assertNull("planFromFileBaseDir is null by default", options.getPlanFileBaseDir());
        });

        final Result<Options> optionsResult = new Options.Builder()
                .setPlanFile(new File("src/test/resources/opears/adhocPlan/plan.json")).build(console);
        optionsResult.forEach(options -> {
            assertTrue("planFromFile should be a file: " + options.getPlanFile().getAbsolutePath(),
                    !options.getPlanFile().isDirectory());
            assertNull("planFromFileBaseDir should be null", options.getPlanFileBaseDir());
        });

        final Result<Options> optionsWithBaseResult = new Options.Builder()
                .setPlanFile(new File("src/test/resources/opears/adhocPlan/plan.json"))
                .setPlanFileBaseDir(console.getCwd())
                .build(console);
        optionsWithBaseResult.forEach(options -> {
            assertTrue("planFromFile should be a file: " + options.getPlanFile().getAbsolutePath(),
                    !options.getPlanFile().isDirectory());
            assertTrue("planFromFileBaseDir should be a directory: " + options.getPlanFileBaseDir()
                    .getAbsolutePath(), options.getPlanFileBaseDir().isDirectory());
        });
    }

    @Test
    public void testWithPreInstallFiles() throws Exception {
        final File testOutDir = new File(tempDir, "testWithPreInstallFiles");
        FileUtils.deleteDirectory(testOutDir);
        final Console console = getMockConsole();
        when(console.getCwd()).thenReturn(tempDir);
        final Result<Options> defaultOptionsResult = new Options.Builder().build(console);
        assertFalse("options build is successful", defaultOptionsResult.getError().isPresent());
        defaultOptionsResult.forEach(options -> {
            assertTrue("preInstallFiles is empty by default", options.getPreInstallFiles().isEmpty());
            assertFalse("false hasOverrides", options.hasOverrides());
            final OakpalPlan originalPlan = new OakpalPlan.Builder(null, null)
                    .withPreInstallUrls(Collections.emptyList())
                    .build();
            final OakpalPlan overriddenPlan = options.applyOverrides(originalPlan);
            assertSame("same plan with no overrides", originalPlan, overriddenPlan);
            assertTrue("pre install urls is empty", overriddenPlan.getPreInstallUrls().isEmpty());
        });

        final File contentPackageSrc = new File("src/test/resources/simple-content");
        final File contentPackageJar = new File(testOutDir, "simple-content.zip");
        TestPackageUtil.buildJarFromDir(contentPackageSrc, contentPackageJar, Collections.emptyMap());

        final Result<Options> optionsResult = new Options.Builder()
                .addPreInstallFile(contentPackageJar).build(console);
        optionsResult.forEach(options -> {
            assertFalse("preInstallFiles is not empty", options.getPreInstallFiles().isEmpty());
            assertTrue("true hasOverrides", options.hasOverrides());
            final OakpalPlan originalPlan = new OakpalPlan.Builder(null, null)
                    .withPreInstallUrls(Collections.emptyList())
                    .build();
            final OakpalPlan overriddenPlan = options.applyOverrides(originalPlan);
            assertNotSame("not same plan", originalPlan, overriddenPlan);
            assertFalse("pre install urls is not empty", overriddenPlan.getPreInstallUrls().isEmpty());
            assertEquals("expect pre install file", contentPackageJar.getAbsolutePath(),
                    Fun.result1(URL::toURI).apply(overriddenPlan.getPreInstallUrls().get(0))
                            .map(Fun.compose1(File::new, File::getAbsolutePath)).getOrDefault(""));
        });
    }

    @Test
    public void testNoHooks() {
        final Console console = getMockConsole();
        when(console.getCwd()).thenReturn(tempDir);
        final Options.Builder builder = new Options.Builder()
                .setOpearFile(new File("src/test/resources/opears/hooksPlan"));
        final Result<Options> defaultOptionsResult = builder.build(console);
        assertFalse("options build is successful", defaultOptionsResult.getError().isPresent());
        defaultOptionsResult.forEach(options -> {
            assertFalse("isNoHooks is disabled by default", options.isNoHooks());
            assertFalse("false hasOverrides", options.hasOverrides());
            final OakpalPlan originalPlan = new OakpalPlan.Builder(null, null)
                    .withEnablePreInstallHooks(true)
                    .withInstallHookPolicy(InstallHookPolicy.REPORT)
                    .build();
            final OakpalPlan overriddenPlan = options.applyOverrides(originalPlan);
            assertSame("same plan with no overrides", originalPlan, overriddenPlan);
            assertTrue("pre install hooks enabled", overriddenPlan.isEnablePreInstallHooks());
            assertSame("install hooks policy",
                    InstallHookPolicy.REPORT, overriddenPlan.getInstallHookPolicy());
        });
        final Result<Options> noHooksOptionsResult = builder.setNoHooks(true).build(console);
        assertFalse("options build is successful", noHooksOptionsResult.getError().isPresent());
        noHooksOptionsResult.forEach(options -> {
            assertTrue("isNoHooks is enabled by builder", options.isNoHooks());
            assertTrue("true hasOverrides", options.hasOverrides());
            final OakpalPlan originalPlan = new OakpalPlan.Builder(null, null)
                    .withEnablePreInstallHooks(true)
                    .withInstallHookPolicy(InstallHookPolicy.REPORT)
                    .build();
            final OakpalPlan overriddenPlan = options.applyOverrides(originalPlan);
            assertNotSame("not same plan with overrides", originalPlan, overriddenPlan);
            assertFalse("pre install hooks disabled", overriddenPlan.isEnablePreInstallHooks());
            assertSame("install hooks policy",
                    InstallHookPolicy.SKIP, overriddenPlan.getInstallHookPolicy());
        });

    }
}
