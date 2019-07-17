package net.adamcin.oakpal.core;

import static net.adamcin.oakpal.core.Fun.result1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
