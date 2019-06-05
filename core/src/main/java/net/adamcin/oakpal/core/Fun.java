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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Function transformation methods.
 */
public final class Fun {
    private Fun() {
        // no construct
    }

    public static <T, I, R> Function<T, R>
    compose(@NotNull final Function<T, I> before, @NotNull final Function<I, R> after) {
        return after.compose(before);
    }

    public static <T, U, I, R> BiFunction<T, U, R>
    compose2(@NotNull final BiFunction<T, U, I> before, @NotNull final Function<I, R> after) {
        return before.andThen(after);
    }

    public static <T, P> Predicate<T>
    composeTest(@NotNull final Function<T, P> inputFunction,
                @NotNull final Predicate<P> testResult) {
        return input -> testResult.test(inputFunction.apply(input));
    }

    public static <T, U, P, Q> BiPredicate<T, U>
    composeTest2(@NotNull final Function<T, P> inputTFunction,
                 @NotNull final Function<U, Q> inputUFunction,
                 @NotNull final BiPredicate<P, Q> testResult) {
        return (inputT, inputU) -> testResult.test(inputTFunction.apply(inputT), inputUFunction.apply(inputU));
    }

    public static <T, U, P> BiPredicate<T, U>
    composeTest2(@NotNull final BiFunction<T, U, P> inputFunction,
                 @NotNull final Predicate<P> testResult) {
        return (inputT, inputU) -> testResult.test(inputFunction.apply(inputT, inputU));
    }

    public static <T, R> Consumer<T>
    toVoid1(@NotNull final Function<T, R> inputFunction) {
        return inputFunction::apply;
    }

    public static <T, U, R> BiConsumer<T, U>
    toVoid2(@NotNull final BiFunction<T, U, R> inputFunction) {
        return inputFunction::apply;
    }

    public static <T, R> Function<T, R>
    infer1(@NotNull final Function<T, R> methodRef) {
        return methodRef;
    }

    public static <T, U, R> BiFunction<T, U, R>
    infer2(@NotNull final BiFunction<T, U, R> methodRef) {
        return methodRef;
    }

    public static <T> Supplier<T>
    infer0(@NotNull final Supplier<T> methodRef) {
        return methodRef;
    }

    public static <T> Predicate<T>
    inferTest1(@NotNull final Predicate<T> methodRef) {
        return methodRef;
    }

    public static <T, U> BiPredicate<T, U>
    inferTest2(@NotNull final BiPredicate<T, U> methodRef) {
        return methodRef;
    }

    public static <K, V> Map.Entry<K, V>
    toEntry(@Nullable final K key, @Nullable final V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, V, R> Function<Map.Entry<K, V>, R>
    mapEntry(@NotNull final BiFunction<K, V, R> biMapFunction) {
        return entry -> biMapFunction.apply(entry.getKey(), entry.getValue());
    }

    public static <K, V, W> Function<Map.Entry<K, V>, Map.Entry<K, W>>
    mapValue(@NotNull final BiFunction<K, V, W> valueBiFunction) {
        return entry -> toEntry(entry.getKey(), valueBiFunction.apply(entry.getKey(), entry.getValue()));
    }

    public static <K, V, W> Function<Map.Entry<K, V>, Map.Entry<K, W>>
    mapValue(@NotNull final Function<V, W> valueFunction) {
        return mapValue((key, value) -> valueFunction.apply(value));
    }

    public static <K, V, L> Function<Map.Entry<K, V>, Map.Entry<L, V>>
    mapKey(@NotNull final BiFunction<K, V, L> keyBiFunction) {
        return entry -> toEntry(keyBiFunction.apply(entry.getKey(), entry.getValue()), entry.getValue());
    }

    public static <K, V, L> Function<Map.Entry<K, V>, Map.Entry<L, V>>
    mapKey(@NotNull final Function<K, L> keyFunction) {
        return mapKey((key, value) -> keyFunction.apply(key));
    }

    public static <K, V> Consumer<Map.Entry<K, V>>
    onEntry(@NotNull final BiConsumer<K, V> biConsumer) {
        return entry -> biConsumer.accept(entry.getKey(), entry.getValue());
    }

    public static <K, V> Consumer<Map.Entry<K, V>>
    onKey(@NotNull final Consumer<K> consumer) {
        return entry -> consumer.accept(entry.getKey());
    }

    public static <K, V> Consumer<Map.Entry<K, V>>
    onValue(@NotNull final Consumer<V> consumer) {
        return entry -> consumer.accept(entry.getValue());
    }

    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testEntry(@NotNull final BiPredicate<K, V> biPredicate) {
        return entry -> biPredicate.test(entry.getKey(), entry.getValue());
    }

    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testValue(@NotNull final Predicate<V> valuePredicate) {
        return testEntry((key, value) -> valuePredicate.test(value));
    }

    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testKey(@NotNull final Predicate<K> keyPredicate) {
        return testEntry((key, value) -> keyPredicate.test(key));
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
        private FunRuntimeException(@NotNull final Throwable cause) {
            super(cause);
        }
    }

    /**
     * Composes four lambdas into a single function for use with flatMap() defined by {@link java.util.stream.Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     *
     * @param monadUnit the "unit" (or "single") function defined by the appropriate monad. I.E. Stream::of,
     *                  Optional::of, or Optional::ofNullable.
     * @param monadZero the "zero" (or "empty") function defined by the appropriate monad, as in Stream::empty,
     *                  or Optional::empty
     * @param onElement some function that produces type {@code R} when given an object of type {@code T}, or fails
     *                  with an Exception.
     * @param onError   an optional consumer function to perform some logic when the parser function throws.
     *                  Receives both the failing input element and the caught Exception.
     * @param <M>       The captured monad type, which must match the return types of the {@code monadUnit} and
     *                  {@code monadZero} functions, but which is not involved in the {@code onElement} or
     *                  {@code onError} functions.
     * @param <T>       The input type mapped by the monad, i.e. the String type in {@code Stream<String>}.
     * @param <R>       The output type mapped by the monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a flatMappable function
     */
    public static <M, T, R> Function<T, M> composeTry(final Function<R, M> monadUnit,
                                                      final Supplier<M> monadZero,
                                                      final ThrowingFunction<T, R> onElement,
                                                      final BiConsumer<T, Exception> onError) {
        final BiConsumer<T, Exception> consumeError = onError != null
                ? onError
                : (e, t) -> {
        };

        return (element) -> {
            try {
                return monadUnit.apply(onElement.tryApply(element));
            } catch (final Exception error) {
                consumeError.accept(element, error);
                return monadZero.get();
            }
        };
    }


    public static <R> Supplier<R>
    uncheck0(@NotNull final ThrowingSupplier<R> mayThrowOnGet) {
        return () -> {
            try {
                return mayThrowOnGet.tryGet();
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, R> Function<T, R>
    uncheck1(@NotNull final ThrowingFunction<T, R> mayThrowOnApply) {
        return input -> {
            try {
                return mayThrowOnApply.tryApply(input);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R>
    uncheck2(@NotNull final ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnApply.tryApply(inputT, inputU);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T> Predicate<T>
    uncheckTest1(@NotNull final ThrowingPredicate<T> mayThrowOnTest) {
        return input -> {
            try {
                return mayThrowOnTest.tryTest(input);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, U> BiPredicate<T, U>
    uncheckTest2(@NotNull final ThrowingBiPredicate<T, U> mayThrowOnTest) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnTest.tryTest(inputT, inputU);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T>
    uncheckVoid1(@NotNull final ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            try {
                mayThrowOnAccept.tryAccept(input);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T, U> BiConsumer<T, U>
    uncheckVoid2(@NotNull final ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (inputT, inputU) -> {
            try {
                mayThrowOnAccept.tryAccept(inputT, inputU);
            } catch (Exception e) {
                throw new FunRuntimeException(e);
            }
        };
    }

    public static <T> Predicate<T>
    testOrDefault1(@NotNull final ThrowingPredicate<T> mayThrowOnTest, boolean defaultValue) {
        return input -> {
            try {
                return mayThrowOnTest.tryTest(input);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <R> Supplier<R>
    tryOrDefault0(@NotNull final ThrowingSupplier<R> mayThrowOnGet, @Nullable R defaultValue) {
        return () -> {
            try {
                return mayThrowOnGet.tryGet();
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <T, R> Function<T, R>
    tryOrDefault1(@NotNull final ThrowingFunction<T, R> mayThrowOnApply, @Nullable R defaultValue) {
        return input -> {
            try {
                return mayThrowOnApply.tryApply(input);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R>
    tryOrDefault2(@NotNull final ThrowingBiFunction<T, U, R> mayThrowOnApply, @Nullable R defaultValue) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnApply.tryApply(inputT, inputU);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <R> Supplier<Optional<R>>
    tryOrOptional0(@NotNull final ThrowingSupplier<R> mayThrowOnGet) {
        return () -> {
            try {
                return Optional.of(mayThrowOnGet.tryGet());
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T, R> Function<T, Optional<R>>
    tryOrOptional1(@NotNull final ThrowingFunction<T, R> mayThrowOnApply) {
        return input -> {
            try {
                return Optional.of(mayThrowOnApply.tryApply(input));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, Optional<R>>
    tryOrOptional2(@NotNull final ThrowingBiFunction<T, U, R> mayThrowOnApply) {
        return (inputT, inputU) -> {
            try {
                return Optional.of(mayThrowOnApply.tryApply(inputT, inputU));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T> Consumer<T>
    tryOrVoid1(@NotNull final ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            try {
                mayThrowOnAccept.tryAccept(input);
            } catch (Exception e) {
                // do nothing
            }
        };
    }


    public static <T, U> BiConsumer<T, U>
    tryOrVoid2(@NotNull final ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (inputT, inputU) -> {
            try {
                mayThrowOnAccept.tryAccept(inputT, inputU);
            } catch (Exception e) {
                // do nothing
            }
        };
    }

    public static <T> Predicate<T>
    tryOrFalse1(@NotNull final ThrowingPredicate<T> mayThrowOnTest) {
        return input -> {
            try {
                return mayThrowOnTest.tryTest(input);
            } catch (Exception e) {
                return false;
            }
        };
    }

    public static <T, U> BiPredicate<T, U>
    tryOrFalse2(@NotNull final ThrowingBiPredicate<T, U> mayThrowOnTest) {
        return (inputT, inputU) -> {
            try {
                return mayThrowOnTest.tryTest(inputT, inputU);
            } catch (Exception e) {
                return false;
            }
        };
    }
}

