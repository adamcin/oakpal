/*
 * Copyright 2019 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.core;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Function transformation methods.
 */
public final class Fun {
    private Fun() {
        // no construct
    }

    public static <T> Stream<T> streamIt(final @Nullable T element) {
        return streamOpt(Optional.ofNullable(element));
    }

    public static <T> Stream<T> streamOpt(final @NotNull Optional<T> element) {
        return element.map(Stream::of).orElse(Stream.empty());
    }

    public static <T> @NotNull Function<T, T> tee(final @NotNull Consumer<T> consumer) {
        return input -> {
            consumer.accept(input);
            return input;
        };
    }

    public static <T> @NotNull Function<T, Nothing> doEach1(final @NotNull Consumer<T> consumer) {
        return input -> {
            consumer.accept(input);
            return Nothing.instance;
        };
    }

    public static <T, U> @NotNull BiFunction<T, U, Nothing> doEach2(final @NotNull BiConsumer<T, U> consumer) {
        return (input0, input1) -> {
            consumer.accept(input0, input1);
            return Nothing.instance;
        };
    }

    public static <T, R> @NotNull Function<T, R> constant1(final @NotNull Supplier<R> supplier) {
        return input -> supplier.get();
    }

    public static <T, U, R> @NotNull BiFunction<T, U, R> constant2(final @NotNull Supplier<R> supplier) {
        return (input0, input1) -> supplier.get();
    }

    public static <T, I, R> Function<T, R>
    compose(final @NotNull Function<T, I> before, final @NotNull Function<I, R> after) {
        return before.andThen(after);
    }

    public static <R, S> Supplier<S>
    compose0(final @NotNull Supplier<R> before, final @NotNull Function<R, S> after) {
        final Function<Nothing, S> composed = compose(constant1(before), after);
        return () -> composed.apply(Nothing.instance);
    }

    public static <T, U, I, R> BiFunction<T, U, R>
    compose2(final @NotNull BiFunction<T, U, I> before, final @NotNull Function<I, R> after) {
        return before.andThen(after);
    }

    public static <T, P> Predicate<T>
    composeTest(final @NotNull Function<T, P> inputFunction,
                final @NotNull Predicate<P> testResult) {
        return input -> testResult.test(inputFunction.apply(input));
    }

    public static <T, U, P, Q> BiPredicate<T, U>
    composeTest2(final @NotNull Function<T, P> inputTFunction,
                 final @NotNull Function<U, Q> inputUFunction,
                 final @NotNull BiPredicate<P, Q> testResult) {
        return (inputT, inputU) -> testResult.test(inputTFunction.apply(inputT), inputUFunction.apply(inputU));
    }

    public static <T, U, P> BiPredicate<T, U>
    composeTest2(final @NotNull BiFunction<T, U, P> inputFunction,
                 final @NotNull Predicate<P> testResult) {
        return (inputT, inputU) -> testResult.test(inputFunction.apply(inputT, inputU));
    }

    public static <T, R> Consumer<T>
    toVoid1(final @NotNull Function<T, R> inputFunction) {
        return inputFunction::apply;
    }

    public static <T, U, R> BiConsumer<T, U>
    toVoid2(final @NotNull BiFunction<T, U, R> inputFunction) {
        return inputFunction::apply;
    }

    public static <T, R> Function<T, R>
    infer1(final @NotNull Function<T, R> methodRef) {
        return methodRef;
    }

    public static <T, U, R> BiFunction<T, U, R>
    infer2(final @NotNull BiFunction<T, U, R> methodRef) {
        return methodRef;
    }

    public static <T> Supplier<T>
    infer0(final @NotNull Supplier<T> methodRef) {
        return methodRef;
    }

    public static <T> Predicate<T>
    inferTest1(final @NotNull Predicate<T> methodRef) {
        return methodRef;
    }

    public static <T, U> BiPredicate<T, U>
    inferTest2(final @NotNull BiPredicate<T, U> methodRef) {
        return methodRef;
    }

    public static <K, V> Function<Map.Entry<K, V>, Map.Entry<K, V>>
    entryTee(final @NotNull BiConsumer<K, V> consumer) {
        return entry -> {
            consumer.accept(entry.getKey(), entry.getValue());
            return entry;
        };
    }

    public static <K, V> Function<K, Map.Entry<K, V>>
    zipKeysWithValueFunc(final @NotNull Function<K, V> valueFunc) {
        return key -> toEntry(key, valueFunc.apply(key));
    }

    public static <K, V> Function<V, Map.Entry<K, V>>
    zipValuesWithKeyFunc(final @NotNull Function<V, K> keyFunction) {
        return value -> toEntry(keyFunction.apply(value), value);
    }

    public static <K, V> Map.Entry<K, V>
    toEntry(final @Nullable K key, final @Nullable V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMap(final @NotNull BinaryOperator<V> mergeFunction) {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction);
    }

    public static <K, V, R> Function<Map.Entry<K, V>, R>
    mapEntry(final @NotNull BiFunction<K, V, R> biMapFunction) {
        return entry -> biMapFunction.apply(entry.getKey(), entry.getValue());
    }

    public static <K, V, W> Function<Map.Entry<K, V>, Map.Entry<K, W>>
    mapValue(final @NotNull BiFunction<K, V, W> valueBiFunction) {
        return entry -> toEntry(entry.getKey(), valueBiFunction.apply(entry.getKey(), entry.getValue()));
    }

    public static <K, V, W> Function<Map.Entry<K, V>, Map.Entry<K, W>>
    mapValue(final @NotNull Function<V, W> valueFunction) {
        return mapValue((key, value) -> valueFunction.apply(value));
    }

    public static <K, V, L> Function<Map.Entry<K, V>, Map.Entry<L, V>>
    mapKey(final @NotNull BiFunction<K, V, L> keyBiFunction) {
        return entry -> toEntry(keyBiFunction.apply(entry.getKey(), entry.getValue()), entry.getValue());
    }

    public static <K, V, L> Function<Map.Entry<K, V>, Map.Entry<L, V>>
    mapKey(final @NotNull Function<K, L> keyFunction) {
        return mapKey((key, value) -> keyFunction.apply(key));
    }

    public static <K, V> Consumer<Map.Entry<K, V>>
    onEntry(final @NotNull BiConsumer<K, V> biConsumer) {
        return entry -> biConsumer.accept(entry.getKey(), entry.getValue());
    }

    public static <K, V> Consumer<Map.Entry<K, V>>
    onKey(final @NotNull Consumer<K> consumer) {
        return entry -> consumer.accept(entry.getKey());
    }

    public static <K, V> Consumer<Map.Entry<K, V>>
    onValue(final @NotNull Consumer<V> consumer) {
        return entry -> consumer.accept(entry.getValue());
    }

    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testEntry(final @NotNull BiPredicate<K, V> biPredicate) {
        return entry -> biPredicate.test(entry.getKey(), entry.getValue());
    }

    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testValue(final @NotNull Predicate<V> valuePredicate) {
        return testEntry((key, value) -> valuePredicate.test(value));
    }

    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testKey(final @NotNull Predicate<K> keyPredicate) {
        return testEntry((key, value) -> keyPredicate.test(key));
    }

    public static <T> Predicate<T> inSet(final @NotNull Set<? super T> haystack) {
        return haystack::contains;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<R> {
        R tryGet() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingPredicate<T> {
        boolean tryTest(T input) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingBiPredicate<T, U> {
        boolean tryTest(T inputT, U inputU) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R tryApply(T input) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingBiFunction<T, U, R> {
        R tryApply(T inputT, U inputU) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void tryAccept(T input) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {
        void tryAccept(T inputT, U inputU) throws Exception;
    }

    /**
     * Wrapping runtime error type for unchecked throwing functions.
     */
    public static final class FunRuntimeException extends RuntimeException {
        private FunRuntimeException(final @NotNull Throwable cause) {
            super(cause);
        }
    }

    /**
     * Composes four lambdas into a single function for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     *
     * @param monoidUnit the "unit" (or "single") function defined by the appropriate monoid/monad. I.E. Stream::of,
     *                   Optional::of, or Optional::ofNullable.
     * @param monoidZero the "zero" (or "empty") function defined by the appropriate monoid/monad, as in Stream::empty,
     *                   or Optional::empty
     * @param onElement  some function that produces type {@code R} when given an object of type {@code T}, or fails
     *                   with an Exception.
     * @param onError    an optional consumer function to perform some logic when the parser function throws.
     *                   Receives both the failing input element and the caught Exception.
     * @param <M>        The captured monad type, which must match the return types of the {@code monoidUnit} and
     *                   {@code monoidZero} functions, but which is not involved in the {@code onElement} or
     *                   {@code onError} functions.
     * @param <T>        The input type mapped by the monoid/monad, i.e. the String type in {@code Stream<String>}.
     * @param <R>        The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a flatMappable function
     */
    public static <M, T, R> Function<T, M> composeTry(final Function<R, M> monoidUnit,
                                                      final Supplier<M> monoidZero,
                                                      final ThrowingFunction<T, R> onElement,
                                                      final BiConsumer<T, Exception> onError) {
        final BiConsumer<T, Exception> consumeError = onError != null
                ? onError
                : (e, t) -> {
        };

        return (element) -> {
            try {
                return monoidUnit.apply(onElement.tryApply(element));
            } catch (final Exception error) {
                consumeError.accept(element, error);
                return monoidZero.get();
            }
        };
    }


    public static <R> Supplier<R>
    uncheck0(final @NotNull ThrowingSupplier<R> mayThrowOnGet) {
        return () -> {
            try {
                return mayThrowOnGet.tryGet();
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <R> Supplier<Result<R>>
    result0(final @NotNull ThrowingSupplier<R> mayThrowOnGet) {
        final Supplier<R> unchecked = uncheck0(mayThrowOnGet);
        return () -> {
            try {
                return Result.success(unchecked.get());
            } catch (final RuntimeException e) {
                return Result.failure(e);
            }
        };
    }

    public static <T, R> Function<T, R>
    uncheck1(final @NotNull ThrowingFunction<T, R> mayThrowOnApply) {
        return input -> {
            try {
                return mayThrowOnApply.tryApply(input);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, R> Function<T, Result<R>>
    result1(final @NotNull ThrowingFunction<T, R> mayThrowOnGet) {
        final Function<T, R> unchecked = uncheck1(mayThrowOnGet);
        return input -> {
            try {
                return Result.success(unchecked.apply(input));
            } catch (final RuntimeException e) {
                return Result.failure(e);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R>
    uncheck2(final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnApply.tryApply(inputT, inputU);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, Result<R>>
    result2(final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        final BiFunction<T, U, R> unchecked = uncheck2(mayThrowOnApply);
        return (inputT, inputU) -> {
            try {
                return Result.success(unchecked.apply(inputT, inputU));
            } catch (final RuntimeException e) {
                return Result.failure(e);
            }
        };
    }

    public static <T> Predicate<T>
    uncheckTest1(final @NotNull ThrowingPredicate<T> mayThrowOnTest) {
        return input -> {
            try {
                return mayThrowOnTest.tryTest(input);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, U> BiPredicate<T, U>
    uncheckTest2(final @NotNull ThrowingBiPredicate<T, U> mayThrowOnTest) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnTest.tryTest(inputT, inputU);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T>
    uncheckVoid1(final @NotNull ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            try {
                mayThrowOnAccept.tryAccept(input);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, U> BiConsumer<T, U>
    uncheckVoid2(final @NotNull ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (inputT, inputU) -> {
            try {
                mayThrowOnAccept.tryAccept(inputT, inputU);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T> Predicate<T>
    testOrDefault1(final @NotNull ThrowingPredicate<T> mayThrowOnTest, boolean defaultValue) {
        return input -> {
            try {
                return mayThrowOnTest.tryTest(input);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <R> Supplier<R>
    tryOrDefault0(final @NotNull ThrowingSupplier<R> mayThrowOnGet, @Nullable R defaultValue) {
        return compose0(result0(mayThrowOnGet), result -> result.getOrElse(defaultValue));
    }

    public static <T, R> Function<T, R>
    tryOrDefault1(final @NotNull ThrowingFunction<T, R> mayThrowOnApply, @Nullable R defaultValue) {
        return compose(result1(mayThrowOnApply), result -> result.getOrElse(defaultValue));
    }

    public static <T, U, R> BiFunction<T, U, R>
    tryOrDefault2(final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply, @Nullable R defaultValue) {
        return compose2(result2(mayThrowOnApply), result -> result.getOrElse(defaultValue));
    }

    public static <R> Supplier<Optional<R>>
    tryOrOptional0(final @NotNull ThrowingSupplier<R> mayThrowOnGet) {
        return () -> {
            try {
                return Optional.of(mayThrowOnGet.tryGet());
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T, R> Function<T, Optional<R>>
    tryOrOptional1(final @NotNull ThrowingFunction<T, R> mayThrowOnApply) {
        return input -> {
            try {
                return Optional.of(mayThrowOnApply.tryApply(input));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, Optional<R>>
    tryOrOptional2(final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        return (inputT, inputU) -> {
            try {
                return Optional.of(mayThrowOnApply.tryApply(inputT, inputU));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T> Consumer<T>
    tryOrVoid1(final @NotNull ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            try {
                mayThrowOnAccept.tryAccept(input);
            } catch (Exception e) {
                // do nothing
            }
        };
    }


    public static <T, U> BiConsumer<T, U>
    tryOrVoid2(final @NotNull ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (inputT, inputU) -> {
            try {
                mayThrowOnAccept.tryAccept(inputT, inputU);
            } catch (Exception e) {
                // do nothing
            }
        };
    }

    public static <T> Predicate<T>
    tryOrFalse1(final @NotNull ThrowingPredicate<T> mayThrowOnTest) {
        return input -> {
            try {
                return mayThrowOnTest.tryTest(input);
            } catch (Exception e) {
                return false;
            }
        };
    }

    public static <T, U> BiPredicate<T, U>
    tryOrFalse2(final @NotNull ThrowingBiPredicate<T, U> mayThrowOnTest) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnTest.tryTest(inputT, inputU);
            } catch (Exception e) {
                return false;
            }
        };
    }
}

