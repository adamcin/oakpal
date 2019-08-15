package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class MainTest {

    final File testOutputBaseDir = new File("target/test-output/MainTest");
    final File cacheBaseDir = new File(testOutputBaseDir, "oakpal-cache");

    @Before
    public void setUp() throws Exception {
        testOutputBaseDir.mkdirs();
    }

    @Test
    public void testMainNoRuntimeExit() {
        Main.main(new String[] {"--blah"});
    }

    @Test
    public void testEmptyPlanExecution() {
        Main.main(new String[]{"-c", cacheBaseDir.getPath(), "-f", "src/test/resources/opears/emptyplan"});
    }

    @Test
    public void testDefultPlanExecution() {
        Main.main(new String[0]);
    }

    @Test
    public void testNoPlanExecution() {
        Main.main(new String[]{"--no-plan"});
    }

    @Test
    public void testSimpleEchoDir() {
        Main.main(new String[]{"-c", cacheBaseDir.getPath(), "-f", "src/test/resources/opears/simpleEcho"});
    }

    @Test
    public void testOutputFile() {
        final File testOutputDir = new File(testOutputBaseDir, "testOutputFile");
        testOutputDir.mkdirs();
        final File testOutputFile = new File(testOutputDir, "out.txt");
        if (testOutputFile.exists()) {
            testOutputFile.delete();
        }
        Main.main(new String[]{"-c", cacheBaseDir.getPath(), "-f", "src/test/resources/opears/simpleEcho", "-o", testOutputFile.getPath()});
        assertTrue("testOutputFile exists", testOutputFile.exists());
        final File testOutputJson = new File(testOutputDir, "out.json");
        if (testOutputJson.exists()) {
            testOutputJson.delete();
        }
        Main.main(new String[]{"-c", cacheBaseDir.getPath(),
                "-f", "src/test/resources/opears/simpleEcho",
                "-j", "-o", testOutputJson.getPath()});
        assertTrue("testOutputJson exists", testOutputJson.exists());
    }

    @Test
    public void testMainJustHelp() {
        Main.main(new String[] {"--help"});
    }

    @Test
    public void testMainJustVersion() {
        Main.main(new String[] {"--version"});
    }

    @Test
    public void testMain_scanUnfilteredPackage() throws Exception {
        File pack = TestPackageUtil.prepareTestPackage("unfiltered_package.zip");
        Main.main(new String[]{"-c", cacheBaseDir.getPath(), pack.getPath()});
    }
}

