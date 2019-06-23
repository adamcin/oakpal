package net.adamcin.oakpal.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.Nothing;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction of standard CLI environment I/O channels.
 */
public interface Console {
    String ENV_OAKPAL_PATH = "OAKPAL_PATH";

    /**
     * Return the current working directory (at time of execution).
     *
     * @return the current working directory
     */
    @NotNull
    default File getCwd() {
        return new File(".");
    }

    /**
     * Return the environment variables as a read-only map.
     *
     * @return the environment variables
     */
    @NotNull
    default Map<String, String> getEnv() {
        return Collections.emptyMap();
    }

    /**
     * Monad for printing stuff to stdout.
     *
     * @param object something
     * @return a nothing IO monad
     */
    IO<Nothing> printLine(Object object);

    /**
     * Monad for printing stuff to stderr.
     *
     * @param object something
     * @return a nothing IO monad
     */
    IO<Nothing> printLineErr(Object object);

    /**
     * Create a new printer that must be disposed eventually, which specifically writes lines to the specified path.
     *
     * @param outFile path of file to open for writing
     * @return a disposable handle/printer
     * @throws FileNotFoundException when {@link java.io.PrintWriter} would
     */
    DisposablePrinter openPrinter(@NotNull File outFile) throws FileNotFoundException;

    /**
     * Dispose open printers.
     */
    void dispose();

    /**
     * Get the directories specified by the OAKPAL_PATH environment variable.
     *
     * @return the directories specified by the OAKPAL_PATH environment variable
     */
    @NotNull
    default File[] getOakpalPath() {
        final File cwd = getCwd();
        return Stream.of(getEnv().getOrDefault(ENV_OAKPAL_PATH, "")
                .split(Pattern.quote(File.pathSeparator)))
                .filter(Fun.inferTest1(String::isEmpty).negate()).map(path -> new File(cwd, path))
                .toArray(File[]::new);
    }
}
