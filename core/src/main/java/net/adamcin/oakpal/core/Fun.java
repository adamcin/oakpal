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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
 * Function transformation methods targeting usage within Java 8+ Streams. Major support for the following functional
 * primitives:
 * <ol>
 * <li>{@link Function}</li>
 * <li>{@link BiFunction}</li>
 * <li>{@link Consumer}</li>
 * <li>{@link BiConsumer}</li>
 * <li>{@link Predicate}</li>
 * <li>{@link BiPredicate}</li>
 * <li>{@link Supplier}</li>
 * </ol>
 * In addition, the {@link Map.Entry} type is treated as a 2-n tuple, or pair, for mapping {@link Map#entrySet()} streams
 * to bifunctions, bipredicates, and biconsumers.
 * Some methods here serve a very simple purpose of supporting immediate type inference of method references,
 * like {@link #infer1(Function)}, which allows {@code infer1(String::valueOf).andThen()}.
 * To support method overloads in lambda and method ref type inference is in play, many method names have a numeric
 * suffix of 0, 1, or 2. (Sometimes the 1 is omitted even when "0" and "2" overloads are defined). This suffix indicates
 * the highest *arity of function arguments, and often represents the arity of a returned function type as well.
 * A Supplier argument represents an *arity of 0, because it takes 0 arguments in order to return a value. A Function
 * has an *arity of 1, because it accepts one argument to return one value. A BiFunction accepts two arguments, and
 * therefore has an *arity of 2. Consumers and Predicates also have an *arity of 1, and BiConsumers and BiPredicates also
 * have an *arity of 2.
 * Another area of treatment is transformation of checked signatures to unchecked signatures via the following functional
 * interfaces:
 * <ol>
 * <li>{@link ThrowingFunction}</li>
 * <li>{@link ThrowingBiFunction}</li>
 * <li>{@link ThrowingConsumer}</li>
 * <li>{@link ThrowingBiConsumer}</li>
 * <li>{@link ThrowingPredicate}</li>
 * <li>{@link ThrowingBiPredicate}</li>
 * <li>{@link ThrowingSupplier}</li>
 * </ol>
 * The {@code uncheck*} methods will catch any checked exception thrown by the above types and rethrow as a
 * {@link FunRuntimeException}.
 * The {@code result*} methods will catch any exception (checked or not) and transform the return type signature to wrap
 * with {@link Result}.
 * For {@link ThrowingConsumer} and {@link ThrowingBiConsumer}, there is no return type to wrap, so
 * {@link #resultNothing1(ThrowingConsumer)} and {@link #resultNothing2(ThrowingBiConsumer)} will transform the consumers
 * to equivalent functions that return a Result wrapping the {@link Nothing} sentinel type.
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

    public static <T, R> @NotNull Function<T, R> constantly1(final @NotNull Supplier<R> supplier) {
        return input -> supplier.get();
    }

    public static <T, U, R> @NotNull BiFunction<T, U, R> constantly2(final @NotNull Supplier<R> supplier) {
        return (input0, input1) -> supplier.get();
    }

    public static <T, I, R> Function<T, R>
    compose(final @NotNull Function<T, I> before, final @NotNull Function<I, R> after) {
        return before.andThen(after);
    }

    public static <R, S> Supplier<S>
    compose0(final @NotNull Supplier<R> before, final @NotNull Function<R, S> after) {
        final Function<Nothing, S> composed = compose(constantly1(before), after);
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

    public static <T> ThrowingFunction<T, Nothing>
    inferNothing1(final @NotNull ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            mayThrowOnAccept.tryAccept(input);
            return Nothing.instance;
        };
    }

    public static <T, U> ThrowingBiFunction<T, U, Nothing>
    inferNothing2(final @NotNull ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (inputT, inputU) -> {
            mayThrowOnAccept.tryAccept(inputT, inputU);
            return Nothing.instance;
        };
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
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k1));
        }, LinkedHashMap::new);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMap(final @NotNull BinaryOperator<V> mergeFunction) {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction, LinkedHashMap::new);
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

    /**
     * This method is used to support type inference around the {@link Collection#contains(Object)} method, whose argument
     * is not otherwise typed with the element type parameter of the collection. This returns the method reference for the
     * provided collection's {@code contains} method as a Predicate, but with restrictive type parameters so that it does
     * not break inference by introducing {@code Object} as the input type bound.
     *
     * @param haystack the collection possibly containing stream elements
     * @param <T>      the stream element type
     * @param <S>      the collection type wildcard
     * @return {@code haystack::contains} as a properly bounded predicate
     */
    public static <T, S extends Collection<? super T>> Predicate<T>
    inSet(final @NotNull S haystack) {
        return haystack::contains;
    }

    /**
     * This method is used to support type inference around the {@link Map#containsKey(Object)} method, whose argument
     * is not otherwise typed with the key type parameter of the Map. This returns the method reference for the
     * provided map's {@code containsKey} method as a Predicate, but with restrictive type parameters so that it does
     * not break inference by introducing {@code Object} as the input type bound.
     *
     * @param haystack the map possibly containing stream elements as keys
     * @param <K>      the stream element type
     * @param <M>      the map type wildcard
     * @return {@code haystack::containsKey} as a properly bounded predicate
     */
    public static <K, M extends Map<? super K, ?>> Predicate<K>
    isKeyIn(final @NotNull M haystack) {
        return haystack::containsKey;
    }

    /**
     * This method is used to support type inference around the {@link Map#containsValue(Object)} method, whose argument
     * is not otherwise typed with the value type parameter of the Map. This returns the method reference for the
     * provided map's {@code containsValue} method as a Predicate, but with restrictive type parameters so that it does
     * not break inference by introducing {@code Object} as the input type bound.
     *
     * @param haystack the map possibly containing stream elements as values
     * @param <V>      the stream element type
     * @param <M>      the map type wildcard
     * @return {@code haystack::containsValue} as a properly bounded predicate
     */
    public static <V, M extends Map<?, ? super V>> Predicate<V>
    isValueIn(final @NotNull M haystack) {
        return haystack::containsValue;
    }

    /**
     * Inferrable type for {@link Supplier}s that throw.
     *
     * @param <R> output type
     */
    @FunctionalInterface
    public interface ThrowingSupplier<R> {
        R tryGet() throws Exception;
    }

    /**
     * Inferrable type for {@link Predicate}s that throw
     *
     * @param <T> input type
     */
    @FunctionalInterface
    public interface ThrowingPredicate<T> {
        boolean tryTest(T input) throws Exception;
    }

    /**
     * Inferrable type for {@link BiPredicate}s that throw.
     *
     * @param <T> left input type
     * @param <U> right input type
     */
    @FunctionalInterface
    public interface ThrowingBiPredicate<T, U> {
        boolean tryTest(T inputT, U inputU) throws Exception;
    }

    /**
     * Inferrable type for {@link Function}s that throw.
     *
     * @param <T> input type
     * @param <R> output type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R tryApply(T input) throws Exception;
    }

    /**
     * Inferrable type for {@link BiFunction}s that throw.
     *
     * @param <T> left input type
     * @param <U> right input type
     * @param <R> output type
     */
    @FunctionalInterface
    public interface ThrowingBiFunction<T, U, R> {
        R tryApply(T inputT, U inputU) throws Exception;
    }

    /**
     * Inferrable type for {@link Consumer}s that throw.
     *
     * @param <T> input type
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void tryAccept(T input) throws Exception;
    }

    /**
     * Inferrable type for {@link BiConsumer}s that throw.
     *
     * @param <T> left input type
     * @param <U> right input type
     */
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
     * @param monadUnit       the "unit" (or "single") function defined by the appropriate monoid/monad. I.E. Stream::of,
     *                        Optional::of, or Optional::ofNullable.
     * @param monadZero       the "zero" (or "empty") function defined by the appropriate monoid/monad, as in Stream::empty,
     *                        or Optional::empty
     * @param mayThrowOnApply some function that produces type {@code R} when applied to an input of type {@code T}, or fails
     *                        with an Exception.
     * @param onError         an optional consumer function to perform some logic when the parser function throws.
     *                        Receives both the failing input element and the caught Exception.
     * @param <M>             The captured monad type, which must match the return types of the {@code monadUnit} and
     *                        {@code monadZero} functions, but which is not involved in the {@code onElement} or
     *                        {@code onError} functions.
     * @param <T>             The input type mapped by the monoid/monad, i.e. the String type in {@code Stream<String>}.
     * @param <R>             The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a function that never throws an exception.
     */
    public static <M, T, R> Function<T, M>
    composeTry(final @NotNull Function<R, M> monadUnit,
               final @NotNull Supplier<M> monadZero,
               final @NotNull ThrowingFunction<T, R> mayThrowOnApply,
               final @Nullable BiConsumer<T, Exception> onError) {
        final BiConsumer<T, Exception> consumeError = onError != null
                ? onError
                : (e, t) -> {
        };

        return element -> {
            try {
                return monadUnit.apply(mayThrowOnApply.tryApply(element));
            } catch (final Exception error) {
                consumeError.accept(element, error);
                return monadZero.get();
            }
        };
    }

    /**
     * Composes four lambdas into a single function for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     * This variation is geared towards use with {@link Result} or some other union type with an Exception constructor.
     *
     * @param monoidSuccess   the "successful" function defined by the appropriate monoid/monad. I.E. Result::success,
     *                        Optional::of, or Optional::ofNullable.
     * @param monoidError     the "failure" function defined by the appropriate monoid/monad, as in Result::failure.
     * @param mayThrowOnApply some function that produces type {@code R} when given an object of type {@code T}, or fails
     *                        with an Exception.
     * @param <M>             The captured monoid type, which must match the return types of the {@code monoidSuccess} and
     *                        {@code monoidError} functions, but which is not involved in the {@code mayThrowOnApply} function.
     * @param <T>             The input type mapped by the monoid/monad, i.e. the String type in {@code Stream<String>}.
     * @param <R>             The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a function that returns a union type distinguishable between a result type and an error type
     */
    public static <M, T, R> Function<T, M>
    composeTry(final @NotNull Function<R, M> monoidSuccess,
               final @NotNull Function<Exception, M> monoidError,
               final @NotNull ThrowingFunction<T, R> mayThrowOnApply) {
        return element -> {
            try {
                return monoidSuccess.apply(mayThrowOnApply.tryApply(element));
            } catch (final Exception error) {
                return monoidError.apply(error);
            }
        };
    }

    /**
     * Composes four lambdas into a single supplier for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     *
     * @param monadUnit     the "unit" (or "single") function defined by the appropriate monoid/monad. I.E. Stream::of,
     *                      Optional::of, or Optional::ofNullable.
     * @param monadZero     the "zero" (or "empty") function defined by the appropriate monoid/monad, as in Stream::empty,
     *                      or Optional::empty
     * @param mayThrowOnGet some supplier that produces type {@code R}, or fails
     *                      with an Exception.
     * @param onError       an optional consumer function to perform some logic when the parser function throws.
     *                      Receives both the failing input element and the caught Exception.
     * @param <M>           The captured monad type, which must match the return types of the {@code monadUnit} and
     *                      {@code monadZero} functions, but which is not involved in the {@code onElement} or
     *                      {@code onError} functions.
     * @param <R>           The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a supplier that never throws an exception.
     */
    public static <M, R> Supplier<M>
    composeTry0(final @NotNull Function<R, M> monadUnit,
                final @NotNull Supplier<M> monadZero,
                final @NotNull ThrowingSupplier<R> mayThrowOnGet,
                final @Nullable Consumer<Exception> onError) {
        final Consumer<Exception> consumeError = onError != null
                ? onError
                : t -> {
        };

        return () -> {
            try {
                return monadUnit.apply(mayThrowOnGet.tryGet());
            } catch (final Exception error) {
                consumeError.accept(error);
                return monadZero.get();
            }
        };
    }

    /**
     * Composes four lambdas into a single supplier for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     * This variation is geared towards use with {@link Result} or some other union type with an Exception constructor.
     *
     * @param monoidSuccess the "successful" function defined by the appropriate monoid/monad. I.E. Result::success,
     *                      Optional::of, or Optional::ofNullable.
     * @param monoidError   the "failure" function defined by the appropriate monoid/monad, as in Result::failure.
     * @param mayThrowOnGet some function that produces type {@code R} when given an object of type {@code T}, or fails
     *                      with an Exception.
     * @param <M>           The captured monoid type, which must match the return types of the {@code monoidSuccess} and
     *                      {@code monoidError} functions, but which is not involved in the {@code mayThrowOnApply} function.
     * @param <R>           The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a supplier that returns a union type distinguishable between a result type and an error type
     */
    public static <M, R> Supplier<M>
    composeTry0(final @NotNull Function<R, M> monoidSuccess,
                final @NotNull Function<Exception, M> monoidError,
                final @NotNull ThrowingSupplier<R> mayThrowOnGet) {
        return () -> {
            try {
                return monoidSuccess.apply(mayThrowOnGet.tryGet());
            } catch (final Exception error) {
                return monoidError.apply(error);
            }
        };
    }

    /**
     * Composes four lambdas into a single bifunction for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     *
     * @param monadUnit       the "unit" (or "single") function defined by the appropriate monoid/monad. I.E. Stream::of,
     *                        Optional::of, or Optional::ofNullable.
     * @param monadZero       the "zero" (or "empty") function defined by the appropriate monoid/monad, as in Stream::empty,
     *                        or Optional::empty
     * @param mayThrowOnApply some function that produces type {@code R} when applied to inputs of type {@code T} and {@code U},
     *                        or fails with an Exception.
     * @param onError         an optional consumer function to perform some logic when the parser function throws.
     *                        Receives both the failing input element and the caught Exception.
     * @param <M>             The captured monad type, which must match the return types of the {@code monadUnit} and
     *                        {@code monadZero} functions, but which is not involved in the {@code onElement} or
     *                        {@code onError} functions.
     * @param <T>             The left input type mapped by the function, i.e. the String type in {@code Stream<String>}.
     * @param <U>             The right input type mapped by the function, i.e. the String type in {@code Stream<String>}.
     * @param <R>             The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a BiFunction that never throws an exception.
     */
    public static <M, T, U, R> BiFunction<T, U, M>
    composeTry2(final @NotNull Function<R, M> monadUnit,
                final @NotNull Supplier<M> monadZero,
                final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply,
                final @Nullable BiConsumer<Map.Entry<T, U>, Exception> onError) {

        return (elementT, elementU) -> {
            try {
                return monadUnit.apply(mayThrowOnApply.tryApply(elementT, elementU));
            } catch (final Exception error) {
                if (onError != null) {
                    onError.accept(toEntry(elementT, elementU), error);
                }
                return monadZero.get();
            }
        };
    }

    /**
     * Composes four lambdas into a single function for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     * This variation is geared towards use with {@link Result} or some other union type with an Exception constructor.
     *
     * @param monoidSuccess   the "successful" function defined by the appropriate monoid/monad. I.E. Result::success,
     *                        Optional::of, or Optional::ofNullable.
     * @param monoidError     the "failure" function defined by the appropriate monoid/monad, as in Result::failure.
     * @param mayThrowOnApply some function that produces type {@code R} when given inputs of type {@code T} and {@code U},
     *                        or fails with an Exception.
     * @param <M>             The captured monoid type, which must match the return types of the {@code monoidSuccess} and
     *                        {@code monoidError} functions, but which is not involved in the {@code mayThrowOnApply} function.
     * @param <T>             The left input type mapped by the function
     * @param <U>             The right input type mapped by the function
     * @param <R>             The output type mapped by the monoid/monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a function that returns a union type distinguishable between a result type and an error type
     */
    public static <M, T, U, R> BiFunction<T, U, M>
    composeTry2(final @NotNull Function<R, M> monoidSuccess,
                final @NotNull Function<Exception, M> monoidError,
                final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        return (elementT, elementU) -> {
            try {
                return monoidSuccess.apply(mayThrowOnApply.tryApply(elementT, elementU));
            } catch (final Exception error) {
                return monoidError.apply(error);
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
        return composeTry0(Result::success, Result::failure, mayThrowOnGet);
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
    result1(final @NotNull ThrowingFunction<T, R> mayThrowOnApply) {
        return composeTry(Result::success, Result::failure, mayThrowOnApply);
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
        return composeTry2(Result::success, Result::failure, mayThrowOnApply);
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

    public static <T> Function<T, Result<Nothing>>
    resultNothing1(final @NotNull ThrowingConsumer<T> mayThrowOnAccept) {
        return result1(inferNothing1(mayThrowOnAccept));
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

    public static <T, U> BiFunction<T, U, Result<Nothing>>
    resultNothing2(final @NotNull ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return result2(inferNothing2(mayThrowOnAccept));
    }

    public static <T> Predicate<T>
    testOrDefault1(final @NotNull ThrowingPredicate<T> mayThrowOnTest, boolean defaultValue) {
        return compose(result1(mayThrowOnTest::tryTest), result -> result.getOrElse(defaultValue))::apply;
    }

    public static <T, U> BiPredicate<T, U>
    testOrDefault2(final @NotNull ThrowingBiPredicate<T, U> mayThrowOnTest, boolean defaultValue) {
        return compose2(result2(mayThrowOnTest::tryTest), result -> result.getOrElse(defaultValue))::apply;
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
        return compose0(result0(mayThrowOnGet), Result::toOptional);
    }

    public static <T, R> Function<T, Optional<R>>
    tryOrOptional1(final @NotNull ThrowingFunction<T, R> mayThrowOnApply) {
        return compose(result1(mayThrowOnApply), Result::toOptional);
    }

    public static <T, U, R> BiFunction<T, U, Optional<R>>
    tryOrOptional2(final @NotNull ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        return compose2(result2(mayThrowOnApply), Result::toOptional);
    }

    public static <T> Consumer<T>
    tryOrVoid1(final @NotNull ThrowingConsumer<T> mayThrowOnAccept) {
        return compose(resultNothing1(mayThrowOnAccept), Result::teeLogError)::apply;
    }

    public static <T, U> BiConsumer<T, U>
    tryOrVoid2(final @NotNull ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return compose2(resultNothing2(mayThrowOnAccept), Result::teeLogError)::apply;
    }
}

