package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.core.OpearFile.NAME_CLASS_PATH;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class OpearFileTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpearFileTest.class);

    @Test
    public void testFindDefaultPlanLocation() {
        Result<OpearFile> opearResult = OpearFile.fromDirectory(new File("src/test/resources/plans/bar"));
        assertFalse("opearResult should not be a failure", opearResult.isFailure());
        Result<URL> urlResult = opearResult.map(Opear::getDefaultPlan);
        assertFalse("urlResult should not be a failure", urlResult.isFailure());

        Result<OakpalPlan> planResult = urlResult.flatMap(OakpalPlan::fromJson);
        assertFalse("plan should load successfully", planResult.isFailure());

        List<String> checklists = planResult.map(OakpalPlan::getChecklists).getOrDefault(Collections.emptyList());
        assertEquals("checklists should contain test/bar checklist",
                Collections.singletonList("test/bar"), checklists);

        Result<OpearFile> notAPlanResult = OpearFile.fromDirectory(new File("src/test/resources/plans/none"));
        assertFalse("notAPlanResult should not be a failure", notAPlanResult.isFailure());
        Result<URL> notAPlanUrlResult = notAPlanResult.map(Opear::getDefaultPlan);
        assertFalse("notAPlanUrlResult should not be a failure", notAPlanUrlResult.isFailure());

        Result<OakpalPlan> notAPlanPlanResult = notAPlanUrlResult.flatMap(OakpalPlan::fromJson);
        assertFalse("notAPlanPlan should load successfully", notAPlanPlanResult.isFailure());

        List<String> notAPlanChecklists = notAPlanPlanResult.map(OakpalPlan::getChecklists).getOrDefault(Collections.emptyList());
        assertEquals("notAPlanChecklists should contain no checklists",
                Collections.singletonList("net.adamcin.oakpal.core/basic"), notAPlanChecklists);

    }

    @Test
    public void testFindPlanLocation() throws Exception {
        final File fooDir = new File("src/test/resources/plans/foo");
        Result<OpearFile> opearResult = OpearFile.fromDirectory(fooDir);

        Result<URL> fooUrlResult = opearResult.flatMap(opear -> opear.getSpecificPlan("other-plan.json"));
        assertEquals("foo plan url should be correct",
                new URL(fooDir.toURI().toURL(), "other-plan.json"),
                fooUrlResult.getOrDefault(OakpalPlan.BASIC_PLAN_URL));

        Result<URL> foo2UrlResult = opearResult.flatMap(opear -> opear.getSpecificPlan("no-plan.json"));
        assertTrue("foo2 plan url should be failure", foo2UrlResult.isFailure());
    }


    @Test
    public void testEmptyPlanJar() throws Exception {
        final File targetDir = new File("target/test-temp");
        targetDir.mkdirs();
        final File mfJar = new File(targetDir, "emptyplan.jar");
        final File mfCache = new File(targetDir, "emptyplan.cache");
        if (mfJar.exists()) {
            mfJar.delete();
        }
        final File mfDir = new File("src/test/resources/plans/xEmpty");
        final File mfFile = new File(mfDir, JarFile.MANIFEST_NAME);
        try (InputStream mfStream = new FileInputStream(mfFile)) {
            Manifest manifest = new Manifest(mfStream);

            try (JarOutputStream mfJarOut = new JarOutputStream(new FileOutputStream(mfJar), manifest)) {
                // nothing to add
                mfJarOut.putNextEntry(new JarEntry("other-plan.json"));
                mfJarOut.write("{}".getBytes(StandardCharsets.UTF_8));
            }
        }

        Result<OpearFile> opearResult = result1((File file) -> new JarFile(file, true))
                .apply(mfJar)
                .flatMap(jar -> OpearFile.fromJar(jar, mfCache));
        opearResult.throwCause(Exception.class);
        assertTrue("opear result should be success ", opearResult.isSuccess());
        Result<OakpalPlan> plan = opearResult.map(OpearFile::getDefaultPlan).flatMap(OakpalPlan::fromJson);
        plan.throwCause(Exception.class);

        assertTrue("opear plan should be empty",
                plan.map(OakpalPlan::getChecklists).getOrDefault(Arrays.asList("not a checklist")).isEmpty());
    }

    @Test
    public void testGetPlanClassPath() {
        Result<OpearFile> opearResult = OpearFile.fromDirectory(
                new File("src/test/resources/OpearFileTest/folders_on_classpath"));

        assertFalse("opearResult should not be a failure", opearResult.isFailure());
        OpearFile opearFile = opearResult.getOrDefault(null);
        assertNotNull("opearFile is not null", opearFile);

        assertArrayEquals("classpath should be", new String[]{"classes", "test-classes"},
                opearFile.metadata.getPlanClassPath());
    }

    @Test
    public void testGetHashCacheKey() throws Exception {
        Result<String> cacheKeyDeletedResult = OpearFile.getHashCacheKey("/no/such/path");
        assertTrue("cacheKey is failure", cacheKeyDeletedResult.isFailure());
        assertTrue("cacheKey failure is FileNotFoundException",
                cacheKeyDeletedResult.findCause(FileNotFoundException.class).isPresent());

        buildDeepTestJar();
        Result<String> cacheKeyResult = OpearFile.getHashCacheKey(deepTestTarget.getPath());
        assertTrue("cacheKey is success", cacheKeyResult.isSuccess());
        String cacheKey = cacheKeyResult.getOrDefault("");
        assertEquals("cacheKey should be 43 characters long: " + cacheKey, 43, cacheKey.length());
        final String pattern = "^[0-9A-Za-z_-]*$";
        assertTrue(String.format("cacheKey %s matches regex %s", cacheKey, pattern), cacheKey.matches(pattern));
    }

    @Test
    public void testFromJar_mkdirsFail() throws Exception {
        buildDeepTestJar();
        final File cacheDir = new File("target/test-output/OpearFileTest/testFromJar_mkdirsFail/cache");
        if (cacheDir.exists()) {
            FileUtils.deleteDirectory(cacheDir);
        }
        FileUtils.touch(new File(cacheDir, OpearFile.getHashCacheKey(deepTestTarget.getPath()).getOrDefault("failed_to_fail")));
        assertTrue("fail with jar when nondirectory present at cache id",
                OpearFile.fromJar(new JarFile(deepTestTarget), cacheDir).isFailure());
    }

    @Test
    public void testFromJar_useCacheDirectory() throws Exception {
        buildDeepTestJar();
        final File cacheDir = new File("target/test-output/OpearFileTest/testFromJar_useCacheDirectory/cache");
        if (cacheDir.exists()) {
            FileUtils.deleteDirectory(cacheDir);
        }

        assertTrue("succeed with jar when cache is fresh",
                OpearFile.fromJar(new JarFile(deepTestTarget), cacheDir).isSuccess());
        assertTrue("succeed with cache dir when cache is present",
                OpearFile.fromJar(new JarFile(deepTestTarget), cacheDir).isSuccess());
    }

    @Test
    public void testCacheJar_fail() throws Exception {
        buildDeepTestJar();
        final File cacheDir = new File("target/test-output/OpearFileTest/testCacheJar_fail/cache");
        if (cacheDir.exists()) {
            FileUtils.deleteDirectory(cacheDir);
        }
        new File(cacheDir, "deep-plan.json").mkdirs();
        assertTrue("fail to cache entry",
                OpearFile.cacheJar(new JarFile(deepTestTarget), cacheDir).isFailure());

        FileUtils.deleteDirectory(cacheDir);
        FileUtils.touch(new File(cacheDir, "META-INF"));
        assertTrue("fail to cache directory entry",
                OpearFile.cacheJar(new JarFile(deepTestTarget), cacheDir).isFailure());
    }

    @Test
    public void testReadNonExistingManifest() {
        assertTrue("non-existing file can't be read",
                OpearFile.readExpectedManifest(
                        new File("src/test/resources/OpearFileTest/non-existing-file.mf"))
                        .isFailure());
    }

    @Test
    public void testValidateOpearManifest() throws Exception {
        assertTrue("invalid manifest when null",
                OpearFile.validateOpearManifest(null).isFailure());
        assertTrue("invalid manifest when no bsn specified",
                OpearFile.validateOpearManifest(
                        new Manifest(new ByteArrayInputStream(new byte[0])))
                        .isFailure());
    }

    @Test
    public void testValidateUriHeaderValues() {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(NAME_CLASS_PATH, "../somedir");
        assertTrue(".. should fail",
                OpearFile.validateUriHeaderValues(manifest, NAME_CLASS_PATH).isFailure());
        manifest.getMainAttributes().put(NAME_CLASS_PATH, "/somedir");
        assertTrue("/ should succeed",
                OpearFile.validateUriHeaderValues(manifest, NAME_CLASS_PATH).isSuccess());
        manifest.getMainAttributes().put(NAME_CLASS_PATH, "/somedir/../..");
        assertTrue("/../.. should fail",
                OpearFile.validateUriHeaderValues(manifest, NAME_CLASS_PATH).isFailure());
    }

    @Test
    public void testGetPlanClassLoader_empty() throws Exception {
        final File cacheDir = new File("target/test-output/OpearFileTest/testGetPlanClassLoader_empty/cache");
        if (cacheDir.exists()) {
            FileUtils.deleteDirectory(cacheDir);
        }

        OpearFile opearFile = new OpearFile(cacheDir,
                new OpearFile.OpearMetadata(new String[0], new String[0], true));

        final ClassLoader parent = new URLClassLoader(new URL[0], null);
        assertSame("same classloader with empty classpath", parent, opearFile.getPlanClassLoader(parent));
    }

    @Test
    public void testGetPlanClassLoader() throws Exception {
        final File cacheDir = new File("target/test-output/OpearFileTest/testGetPlanClassLoader/cache");
        if (cacheDir.exists()) {
            FileUtils.deleteDirectory(cacheDir);
        }
        buildDeepTestJar();
        final Result<OpearFile> opearResult = OpearFile.fromJar(new JarFile(deepTestTarget), cacheDir);
        assertTrue("is successful", opearResult.isSuccess());

        OpearFile opearFile = opearResult.getOrDefault(null);
        assertNotNull("not null", opearFile);
        final String checklistName = "OAKPAL-INF/checklists/embedded-checklist.json";
        final ClassLoader controlCl = new URLClassLoader(new URL[]{embedModuleTarget.toURI().toURL()}, null);
        assertNotNull("control checklist URL not null", controlCl.getResource(checklistName));
        final ClassLoader classLoader = opearFile.getPlanClassLoader(new URLClassLoader(new URL[0], null));
        final URL embeddedChecklistUrl = classLoader.getResource(checklistName);
        assertNotNull("checklist URL not null: " + printClassLoader(classLoader), embeddedChecklistUrl);
    }

    private String printClassLoader(final ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return Arrays.toString(((URLClassLoader) classLoader).getURLs());
        } else {
            return classLoader.toString();
        }
    }

    final File baseDir = new File("src/test/resources/OpearFileTest");
    final File testTarget = new File("target/test-output/OpearFileTest");
    final File deepTestSrc = new File(baseDir, "deep_test_src");
    final File deepTestTarget = new File(testTarget, "deep_test.jar");
    final File embedModuleSrc = new File(baseDir, "embedded_module_src");
    final File embedModuleTarget = new File(testTarget, "embedded_module.jar");


    private void buildEmbeddedModuleJar() throws Exception {
        TestPackageUtil.buildJarFromDir(embedModuleSrc, embedModuleTarget, Collections.emptyMap());
    }

    private void buildDeepTestJar() throws Exception {
        buildEmbeddedModuleJar();
        TestPackageUtil.buildJarFromDir(deepTestSrc, deepTestTarget, Collections.singletonMap(embedModuleTarget.getName(), embedModuleTarget));
    }
}
