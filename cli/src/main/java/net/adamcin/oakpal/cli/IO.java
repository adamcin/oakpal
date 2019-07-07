package net.adamcin.oakpal.cli;

import java.util.function.Function;
import java.util.function.Supplier;

import net.adamcin.oakpal.core.Nothing;

/**
 * Just a simple IO monad.
 *
 * @param <A> the type of value read from "input" after execution, to be fed back into the functional execution chain.
 */
public interface IO<A> extends Supplier<A> {

    default <T> IO<T> add(final IO<T> io) {
        return () -> {
            IO.this.get();
            return io.get();
        };
    }

    IO<Nothing> empty = () -> Nothing.instance;

    static <A> IO<A> unit(A a) {
        return () -> a;
    }

    default <B> IO<B> map(final Function<A, B> f) {
        return () -> f.apply(this.get());
    }

    default <B> IO<B> flatMap(final Function<A, IO<B>> f) {
        return () -> f.apply(this.get()).get();
    }
}
