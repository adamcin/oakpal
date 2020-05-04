package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.Nothing;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
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
        builder.setPreInstallFiles(Collections.emptyList());
        builder.setRepoInitFiles(Collections.emptyList());
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
                    result1(URL::toURI).apply(overriddenPlan.getPreInstallUrls().get(0))
                            .map(Fun.compose1(File::new, File::getAbsolutePath)).getOrDefault(""));
        });
    }

    @Test
    public void testWithRepoInitFiles() throws Exception {
        final File testOutDir = new File(tempDir, "testWithRepoInitFiles");
        FileUtils.deleteDirectory(testOutDir);
        final Console console = getMockConsole();
        when(console.getCwd()).thenReturn(tempDir);
        final Result<Options> defaultOptionsResult = new Options.Builder().build(console);
        assertFalse("options build is successful", defaultOptionsResult.getError().isPresent());
        defaultOptionsResult.forEach(options -> {
            assertTrue("repoInitFiles is empty by default", options.getRepoInitFiles().isEmpty());
            assertFalse("false hasOverrides", options.hasOverrides());
            final OakpalPlan originalPlan = new OakpalPlan.Builder(null, null)
                    .withRepoInitUrls(Collections.emptyList())
                    .build();
            final OakpalPlan overriddenPlan = options.applyOverrides(originalPlan);
            assertSame("same plan with no overrides", originalPlan, overriddenPlan);
            assertTrue("repo init urls is empty", overriddenPlan.getRepoInitUrls().isEmpty());
        });

        final File repoInitFile1 = new File("src/test/resources/opears/adhocPlan/repoinit1.txt");
        final File repoInitFile2 = new File("src/test/resources/opears/adhocPlan/repoinit2.txt");

        final Result<Options> optionsResult = new Options.Builder()
                .addRepoInitFile(repoInitFile1).addRepoInitFile(repoInitFile2).build(console);
        optionsResult.forEach(options -> {
            assertFalse("repoInitFiles is not empty", options.getRepoInitFiles().isEmpty());
            assertTrue("true hasOverrides", options.hasOverrides());
            final OakpalPlan originalPlan = new OakpalPlan.Builder(null, null)
                    .withRepoInitUrls(Collections.emptyList())
                    .build();
            final OakpalPlan overriddenPlan = options.applyOverrides(originalPlan);
            assertNotSame("not same plan", originalPlan, overriddenPlan);
            assertFalse("repo init urls is not empty", overriddenPlan.getRepoInitUrls().isEmpty());
            assertEquals("expect repo init files",
                    Arrays.asList(repoInitFile1.getAbsolutePath(), repoInitFile2.getAbsolutePath()),
                    overriddenPlan.getRepoInitUrls().stream()
                            .map(uncheck1(URL::toURI))
                            .map(compose1(File::new, File::getAbsolutePath))
                            .collect(Collectors.toList()));
        });

        final File cacheDir = new File(testOutDir, "oakpal-cache");
        final List<String> repoInits = Arrays.asList("create user repoInitUser1", "create user repoInitUser2");
        final OakpalPlan repoInitPlan = new OakpalPlan.Builder(null, null).withRepoInits(repoInits).build();
        final byte[] repoInitBytes = Options.getRepoInitBytes(repoInits);
        final String repoInitFileName = Options.fileNameForRepoInitBytes(repoInitBytes).getOrDefault(null);
        assertNotNull("repoInitFileName should not be null", repoInitFileName);

        final File expectRepoInitCache = new File(cacheDir.toPath().resolve("repoinits").toFile(), repoInitFileName);
        assertFalse("repoInit cache file should not exist", expectRepoInitCache.exists());

        final Result<Options> noCacheResult = new Options.Builder().setCacheDir(cacheDir).build(console);
        assertFalse("noCacheResult build is successful", noCacheResult.getError().isPresent());
        noCacheResult.forEach(options -> {
            final OakpalPlan overriddenPlan = options.applyOverrides(repoInitPlan);
            assertSame("same plan with no overrides", repoInitPlan, overriddenPlan);
            assertFalse("repoInit cache file should not exist after no cache", expectRepoInitCache.exists());
            assertEquals("expect equal repoInits", repoInits, overriddenPlan.getRepoInits());
        });

        final Result<Options> firstCacheResult = new Options.Builder()
                .setCacheDir(cacheDir).addRepoInitFile(repoInitFile1).build(console);
        assertFalse("firstCacheResult build is successful", firstCacheResult.getError().isPresent());
        firstCacheResult.forEach(options -> {
            final OakpalPlan overriddenPlan = options.applyOverrides(repoInitPlan);
            assertNotSame("not same plan with no overrides", repoInitPlan, overriddenPlan);
            assertTrue("repoInit cache file should exist after first cache: " +
                    expectRepoInitCache.getAbsolutePath(), expectRepoInitCache.exists());
            assertEquals("expect empty repoInits", Collections.emptyList(), overriddenPlan.getRepoInits());
            assertEquals("expect 2 repoInitUrls", 2, overriddenPlan.getRepoInitUrls().size());
            try (InputStream is = new FileInputStream(expectRepoInitCache)) {
                assertArrayEquals("expect file bytes", repoInitBytes, IOUtils.toByteArray(is));
            } catch (IOException e) {
                fail("should not fail: " + e.getMessage());
            }

        });

        final List<String> repoInitsMinus1 = Collections.singletonList(repoInits.get(0));
        final byte[] repoInitBytesMinus1 = Options.getRepoInitBytes(repoInitsMinus1);
        assertNotEquals("expect different filename", repoInitFileName,
                Options.fileNameForRepoInitBytes(repoInitBytesMinus1));
        try (FileOutputStream os = new FileOutputStream(expectRepoInitCache)) {
            os.write(repoInitBytesMinus1);
        }
        firstCacheResult.forEach(options -> {
            final OakpalPlan overriddenPlan = options.applyOverrides(repoInitPlan);
            assertNotSame("not same plan with no overrides", repoInitPlan, overriddenPlan);
            assertTrue("repoInit cache file should exist after first cache: " +
                    expectRepoInitCache.getAbsolutePath(), expectRepoInitCache.exists());
            assertEquals("expect empty repoInits", Collections.emptyList(), overriddenPlan.getRepoInits());
            assertEquals("expect 2 repoInitUrls", 2, overriddenPlan.getRepoInitUrls().size());
            try (InputStream is = new FileInputStream(expectRepoInitCache)) {
                assertArrayEquals("expect file bytes minus 1", repoInitBytesMinus1, IOUtils.toByteArray(is));
            } catch (IOException e) {
                fail("should not fail minus 1: " + e.getMessage());
            }
        });
    }

    @Test
    public void testCacheRepoInits_emptyRepoInits() throws Exception {
        final File testOutDir = new File(tempDir, "testCacheRepoInits_emptyRepoInits");
        FileUtils.deleteDirectory(testOutDir);
        final File cacheDir = new File(testOutDir, "oakpal-cache");
        Result<URL> urlResult = Options.cacheRepoInits(cacheDir, Collections.emptyList());
        assertTrue("empty repoInits get failure to cache", urlResult.isFailure());
    }

    @Test
    public void testWriteToStream_throwingSupplier() throws Exception {
        OutputStream mockOs = mock(OutputStream.class);
        doThrow(IllegalStateException.class).when(mockOs).write(any(byte[].class));
        Result<Nothing> result = Options.writeToStream(new byte[0], () -> mockOs);
        assertTrue("expect failure", result.isFailure());
        assertTrue("expect same exception", result.findCause(IllegalStateException.class).isPresent());
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
