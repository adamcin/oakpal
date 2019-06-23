package net.adamcin.oakpal.cli;

import java.util.function.Function;

import net.adamcin.oakpal.core.Nothing;

public interface DisposablePrinter extends Function<Object, IO<Nothing>> {

    void dispose();
}
