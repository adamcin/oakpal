package net.adamcin.oakpal.core;


import java.io.Serializable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
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

    public abstract Result<V> logError();

    public final boolean isSuccess() {
        return !isFailure();
    }

    public final boolean isFailure() {
        return getError().isPresent();
    }

    public abstract Optional<RuntimeException> getError();

    public final Optional<Throwable> findCause(final @NotNull Predicate<Throwable> predicate) {
        return Optional.empty();
    }

    /*
    private static Optional<Throwable> findCause(final @NotNull Throwable parent, final @NotNull Predicate<Throwable> predicate) {
        return Optional.of(parent).filter(predicate);(() -> Optional.ofNullable(parent.getCause()).filter(predicate));
    }
    */

    public abstract void forEach(final @NotNull Consumer<V> consumer);

    private static final class Failure<V> extends Result<V> {
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
        public void forEach(final @NotNull Consumer<V> consumer) {
            logSupression();
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

        @Override
        public Result<V> logError() {
            LOGGER.debug("failure [stacktrace visible in TRACE logging]: {}", this);
            logTrace();
            return this;
        }

        private void logTrace() {
            LOGGER.trace("thrown:", this.exception);
        }

        private void logSupression() {
            LOGGER.debug("failure (suppressed) [stacktrace visible in TRACE logging]: {}", this);
            logTrace();
        }
    }

    private static final class Success<V> extends Result<V> {
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
        public void forEach(final @NotNull Consumer<V> consumer) {
            consumer.accept(value);
        }

        @Override
        public Stream<V> stream() {
            return value != null ? Stream.of(value) : Stream.empty();
        }

        @Override
        public Result<V> logError() {
            return this;
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

    /**
     * Builds a result for a wrapped collector.
     *
     * @param <V> wrapped incremental value type
     * @param <A> wrapped accumulator type
     */
    static final class Builder<V, A> implements Consumer<Result<V>> {
        final Result<A> resultAcc;
        final AtomicReference<Result<A>> latch;
        final BiConsumer<A, V> accumulator;

        Builder(final @NotNull Result<A> initial,
                final @NotNull BiConsumer<A, V> accumulator) {
            this.resultAcc = initial;
            this.latch = new AtomicReference<>(resultAcc);
            this.accumulator = accumulator;
        }

        @Override
        public void accept(final Result<V> valueResult) {
            latch.accumulateAndGet(resultAcc, (fromLatch, fromArg) ->
                    fromLatch.flatMap(state ->
                            valueResult.map(value -> {
                                accumulator.accept(state, value);
                                return state;
                            })));
        }

        Result<A> build() {
            return latch.get();
        }
    }

    /**
     * Create a collector that accumulates a stream of Results into a single Result containing either:
     * 1. the collected values of the streamed results according using supplied collector
     * 2. the first encountered failure
     *
     * @param collector the underlying collector
     * @param <V> the incremental value
     * @param <R> the intended container type
     * @param <A> the collector's accumulator type
     * @return if all successful, a Result of a Collection; otherwise, the first encountered failure
     */
    public static <V, R, A> Collector<Result<V>, Builder<V, A>, Result<R>>
    collectOrFailOnFirst(final @NotNull Collector<V, A, R> collector) {

        final Supplier<Builder<V, A>>
                supplier = () ->
                new Builder<>(Result.success(collector.supplier().get()), collector.accumulator());

        final BiConsumer<Result.Builder<V, A>, Result<V>>
                accumulator = Builder::accept;

        final BinaryOperator<Builder<V, A>>
                combiner =
                (builder0, builder1) -> new Builder<>(
                        builder0.build().flatMap(left ->
                                builder1.build().map(right ->
                                        collector.combiner().apply(left, right))),
                        collector.accumulator());

        final Function<Builder<V, A>, Result<R>>
                finisher = acc -> acc.build().map(collector.finisher());

        return Collector.of(supplier, accumulator, combiner, finisher,
                collector.characteristics().stream()
                        .filter(charac -> charac != Collector.Characteristics.IDENTITY_FINISH)
                        .toArray(Collector.Characteristics[]::new));
    }

    public static <V> Collector<Result<V>, Stream.Builder<Result<V>>, Stream<Result<V>>>
    logAndRestream() {
        return new RestreamLogCollector<>("");
    }

    public static <V> Collector<Result<V>, Stream.Builder<Result<V>>, Stream<Result<V>>>
    logAndRestream(final @NotNull String message) {
        return new RestreamLogCollector<>(": " + message);
    }

    private static final class RestreamLogCollector<T>
            implements Collector<Result<T>, Stream.Builder<Result<T>>, Stream<Result<T>>> {
        private final Supplier<Stream.Builder<Result<T>>> supplier;
        private final BiConsumer<Stream.Builder<Result<T>>, Result<T>> accum;

        private RestreamLogCollector(final @NotNull String collectorMessage) {
            if (LOGGER.isDebugEnabled()) {
                final Throwable creation = new Throwable();
                this.supplier = () -> {
                    LOGGER.debug("result collector (see TRACE for creation stack)" + collectorMessage);
                    LOGGER.trace("created here", creation);
                    return Stream.builder();
                };
                this.accum = (builder, element) -> builder.accept(element.logError());
            } else {
                this.supplier = Stream::builder;
                this.accum = Stream.Builder::accept;
            }
        }

        @Override
        public Supplier<Stream.Builder<Result<T>>> supplier() {
            return supplier;
        }

        @Override
        public BiConsumer<Stream.Builder<Result<T>>, Result<T>> accumulator() {
            return accum;
        }

        @Override
        public BinaryOperator<Stream.Builder<Result<T>>> combiner() {
            return (left, right) -> {
                right.build().forEachOrdered(left);
                return left;
            };
        }

        @Override
        public Function<Stream.Builder<Result<T>>, Stream<Result<T>>> finisher() {
            return Stream.Builder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.CONCURRENT);
        }
    }
}
