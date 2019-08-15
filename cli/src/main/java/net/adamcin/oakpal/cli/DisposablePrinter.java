package net.adamcin.oakpal.cli;

import java.util.function.Function;

import net.adamcin.oakpal.core.Nothing;

/**
 * Extension of simple IO printer function type to add a dispose() method.
 */
public interface DisposablePrinter extends Function<Object, IO<Nothing>> {
    void dispose();
}
