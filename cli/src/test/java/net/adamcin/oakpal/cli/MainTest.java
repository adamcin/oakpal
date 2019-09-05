package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MainTest {

    final File testOutputBaseDir = new File("target/test-output/MainTest");
    final File cacheBaseDir = new File(testOutputBaseDir, "oakpal-cache");

    @Before
    public void setUp() throws Exception {
        testOutputBaseDir.mkdirs();
        Main.setExitFunction(status -> {});
        Main.setSwapOutFunction(err -> System.out);
    }

    @Test
    public void testDefaultSwapOutFunction() {
        final PrintStream realOut = System.out;
        final PrintStream realErr = System.err;

        assertSame("same out is returned after replacing with err", realOut,
                Main.DEFAULT_SWAP_OUT.apply(System.err));
        assertSame("same err is returned after replacing with original out", realErr,
                Main.DEFAULT_SWAP_OUT.apply(realOut));

        assertNotSame("leave out and err as they were", System.out, System.err);
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

