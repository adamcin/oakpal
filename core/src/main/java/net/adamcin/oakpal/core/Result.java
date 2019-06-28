package net.adamcin.oakpal.core;


import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A type representing either a successful result value, or failure, with an error.
 *
 * @param <V> The result type.
 */
public abstract class Result<V> implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Result.class);
    private Result() {
        /* construct from factory */
    }

    /**
     * Map it.
     *
     * @param f   the mapping function
     * @param <W> the result type.
     * @return a mapped result
     */
    public abstract <W> @NotNull Result<W> map(final @NotNull Function<V, W> f);

    /**
     * Flat-map it.
     *
     * @param f   the mapping function
     * @param <W> the result type
     * @return the flat mapped result
     */
    public abstract <W> @NotNull Result<W> flatMap(final @NotNull Function<V, Result<W>> f);

    public abstract V getOrElse(final V defaultValue);

    public abstract V getOrElse(final @NotNull Supplier<V> defaultValue);

    public abstract Result<V> orElse(final @NotNull Supplier<Result<V>> defaultValue);

    public abstract Stream<V> stream();

    public final boolean isSuccess() {
        return !isFailure();
    }

    public final boolean isFailure() {
        return getError().isPresent();
    }

    public abstract Optional<RuntimeException> getError();

    private static class Failure<V> extends Result<V> {
        private final RuntimeException exception;

        private Failure(final String message) {
            super();
            this.exception = new IllegalStateException(message);
        }

        private Failure(final @NotNull RuntimeException e) {
            this.exception = e;
        }

        private Failure(final @NotNull Exception e) {
            this.exception = new IllegalStateException(e.getMessage(), e);
        }

        private Failure(final @NotNull String message, final @NotNull Exception e) {
            this.exception = new IllegalStateException(message, e);
        }

        @Override
        public @NotNull <W> Result<W> map(final @NotNull Function<V, W> f) {
            return new Failure<>(this.exception);
        }

        @Override
        public @NotNull <W> Result<W> flatMap(final @NotNull Function<V, Result<W>> f) {
            return new Failure<>(this.exception);
        }

        @Override
        public V getOrElse(final V defaultValue) {
            logSupression();
            return defaultValue;
        }

        @Override
        public V getOrElse(final @NotNull Supplier<V> defaultValue) {
            logSupression();
            return defaultValue.get();
        }

        @Override
        public Result<V> orElse(final @NotNull Supplier<Result<V>> defaultValue) {
            logSupression();
            return defaultValue.get();
        }

        @Override
        public Stream<V> stream() {
            return Stream.empty();
        }

        @Override
        public Optional<RuntimeException> getError() {
            return Optional.of(this.exception);
        }


        @Override
        public String toString() {
            return String.format("Failure(%s)", exception.getMessage());
        }

        private void logSupression() {
            LOGGER.debug("suppressed failure [stacktrace visible in TRACE logging]: {}", this);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("thrown:", this.exception);
            }
        }
    }

    private static class Success<V> extends Result<V> {
        private final V value;

        private Success(final V value) {
            super();
            this.value = value;
        }

        @Override
        public @NotNull <W> Result<W> map(final @NotNull Function<V, W> f) {
            return new Success<>(f.apply(value));
        }

        @Override
        public @NotNull <W> Result<W> flatMap(final @NotNull Function<V, Result<W>> f) {
            return f.apply(value);
        }

        @Override
        public V getOrElse(final V defaultValue) {
            return value;
        }

        @Override
        public V getOrElse(final @NotNull Supplier<V> defaultValue) {
            return value;
        }

        @Override
        public Result<V> orElse(final @NotNull Supplier<Result<V>> defaultValue) {
            return this;
        }

        @Override
        public Stream<V> stream() {
            return value != null ? Stream.of(value) : Stream.empty();
        }

        @Override
        public Optional<RuntimeException> getError() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return String.format("Success(%s)", String.valueOf(value));
        }
    }

    public static <V> Result<V> failure(final String message) {
        return new Failure<>(message);
    }

    public static <V> Result<V> failure(final @NotNull Exception e) {
        return new Failure<>(e);
    }

    public static <V> Result<V> failure(final @NotNull String message, final @NotNull Exception e) {
        return new Failure<>(message, e);
    }

    public static <V> Result<V> failure(final @NotNull RuntimeException e) {
        return new Failure<>(e);
    }

    public static <V> Result<V> success(final V value) {
        return new Success<>(value);
    }
}
