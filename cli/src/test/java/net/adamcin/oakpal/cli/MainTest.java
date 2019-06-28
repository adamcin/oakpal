package net.adamcin.oakpal.cli;

import org.junit.Test;

public class MainTest {
    @Test
    public void testMainNoRuntimeExit() {
        Main.main(new String[] {"--blah"});
    }
}
