package net.adamcin.oakpal.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A sentinel type for functional parameters representing Nothing, like Void.
 */
public final class Nothing {
    /**
     * This is the singleton sentinel value.
     */
    public static final Nothing instance = new Nothing();

    private Nothing() {
        /* prevent instantiation */
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> Function<T, Nothing>
    voidToNothing1(final @NotNull Consumer<? super T> consumer) {
        return input -> {
            consumer.accept(input);
            return Nothing.instance;
        };
    }

    @SuppressWarnings("WeakerAccess")
    public static <T, U> BiFunction<T, U, Nothing>
    voidToNothing2(final @NotNull BiConsumer<? super T, ? super U> consumer) {
        return (inputT, inputU) -> {
            consumer.accept(inputT, inputU);
            return Nothing.instance;
        };
    }

    @SuppressWarnings("WeakerAccess")
    public Nothing combine(final @Nullable Nothing nothing) {
        return this;
    }
}
