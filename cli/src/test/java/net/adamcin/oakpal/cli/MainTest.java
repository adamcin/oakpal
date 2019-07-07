package net.adamcin.oakpal.cli;

import org.junit.Test;

public class MainTest {
    @Test
    public void testMainNoRuntimeExit() {
        Main.main(new String[] {"--blah"});
    }

    @Test
    public void testEmptyPlanExecution() {
        Main.main(new String[]{"-f", "src/test/resources/opears/emptyplan"});
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
        Main.main(new String[]{"-f", "src/test/resources/opears/simpleEcho"});
    }
}

