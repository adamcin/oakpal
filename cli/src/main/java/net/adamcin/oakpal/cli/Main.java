package net.adamcin.oakpal.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

final class Main {
    private final Map<String, String> env;
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;

    Main(final Map<String, String> env,
         final InputStream in,
         final PrintStream out,
         final PrintStream err) {
        this.env = env;
        this.in = in;
        this.out = out;
        this.err = err;
    }

    int doMain(final String[] args) {
        return 0;
    }

    /**
     * Let's do a mental map:
     * <p>
     * 1. input?
     * <p>
     * 2. output?
     * <p>
     * 3. status code?
     *
     * @param args argv yo
     */
    public static void main(final String[] args) {
        Main main = new Main(System.getenv(), System.in, System.out, System.err);
        Runtime.getRuntime().exit(main.doMain(args));
    }
}
