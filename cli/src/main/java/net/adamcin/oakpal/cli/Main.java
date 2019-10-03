package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.api.Fun.result1;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

import net.adamcin.oakpal.api.Nothing;
import net.adamcin.oakpal.api.Result;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Main} hosts the {@link #main(String[])} method, and as an object, captures ENV, stdin, stdout, and stderr as
 * abstracted variables for use by commands.
 */
final class Main implements Console {
    private final File cwd;
    private final Map<String, String> env;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final Map<File, DisposablePrinter> printers = new HashMap<>();

    Main(final @NotNull File cwd,
         final @NotNull Map<String, String> env,
         final @NotNull PrintStream stdout,
         final @NotNull PrintStream stderr) {
        this.cwd = cwd;
        this.env = env;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    @Override
    public @NotNull File getCwd() {
        return this.cwd;
    }

    @Override
    public @NotNull Map<String, String> getEnv() {
        return Collections.unmodifiableMap(this.env);
    }

    @Override
    public IO<Nothing> printLine(final @NotNull Object object) {
        return () -> {
            stdout.println(object.toString());
            return Nothing.instance;
        };
    }

    @Override
    public IO<Nothing> printLineErr(final Object object) {
        return () -> {
            stderr.println(object.toString());
            return Nothing.instance;
        };
    }

    static class DisposablePrinterImpl implements DisposablePrinter {
        final PrintWriter writer;

        DisposablePrinterImpl(final @NotNull PrintWriter writer) {
            this.writer = writer;
        }

        @Override
        public void dispose() {
            writer.close();
        }

        @Override
        public IO<Nothing> apply(final Object object) {
            return () -> {
                writer.println(object.toString());
                writer.flush();
                return Nothing.instance;
            };
        }
    }

    @Override
    public Result<DisposablePrinter> openPrinter(final @NotNull File outFile) {
        final Result<DisposablePrinter> printerResult =
                result1((File file) -> new PrintWriter(file, StandardCharsets.UTF_8.name())).apply(outFile)
                        .map(DisposablePrinterImpl::new);
        printerResult.forEach(printer -> printers.put(outFile, printer));
        return printerResult;
    }

    @Override
    public void dispose() {
        printers.values().stream().forEach(DisposablePrinter::dispose);
        printers.clear();
    }

    int doMain(final @NotNull String[] args) {
        final Command command = new Command();
        final int exitCode = command.perform(this, args).get();
        this.dispose();
        return exitCode;
    }

    static final Function<PrintStream, PrintStream> DEFAULT_SWAP_OUT = err -> {
        final PrintStream out = System.out;
        System.setOut(err);
        return out;
    };

    private static @NotNull IntConsumer exitFunction = Runtime.getRuntime()::exit;
    private static @NotNull Function<PrintStream, PrintStream> swapOutFunction = DEFAULT_SWAP_OUT;

    public static void setExitFunction(final @NotNull IntConsumer exitFunction) {
        Main.exitFunction = exitFunction;
    }

    public static void setSwapOutFunction(final @NotNull Function<PrintStream, PrintStream> swapOutFunction) {
        Main.swapOutFunction = swapOutFunction;
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
        final PrintStream realOut = swapOutFunction.apply(System.err);
        Main main = new Main(new File(".").getAbsoluteFile(),
                Collections.unmodifiableMap(System.getenv()),
                realOut, System.err);
        final int exit = main.doMain(args);
        swapOutFunction.apply(realOut);
        exitFunction.accept(exit);
    }
}
