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

package net.adamcin.oakpal.toolslib;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FunUtil {

    private FunUtil() {
        // no construct
    }

    @FunctionalInterface
    public interface ThrowingPredicate<T> {
        boolean test(T input) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T input) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T input) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {
        void accept(T tInput, U uInput) throws Exception;
    }

    public static <T> Predicate<T> testUnchecked(final ThrowingPredicate<T> mayThrowOnTest) {
        return input -> {
            try {
                return mayThrowOnTest.test(input);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Predicate<T> testOrDefault(final ThrowingPredicate<T> mayThrowOnTest, boolean defaultValue) {
        return input -> {
            try {
                return mayThrowOnTest.test(input);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <T, R> Function<T, R> applyUnchecked(final ThrowingFunction<T, R> mayThrowOnApply) {
        return input -> {
            try {
                return mayThrowOnApply.apply(input);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, R> Function<T, R> tryOrDefault(final ThrowingFunction<T, R> mayThrowOnApply, R defaultValue) {
        return input -> {
            try {
                return mayThrowOnApply.apply(input);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <T, R> Function<T, Optional<R>> tryOrOptional(final ThrowingFunction<T, R> mayThrowOnApply) {
        return input -> {
            try {
                return Optional.of(mayThrowOnApply.apply(input));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    public static <T> Consumer<T> tryConsume(final ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            try {
                mayThrowOnAccept.accept(input);
            } catch (Exception e) {
                // do nothing
            }
        };
    }

    public static <T> Consumer<T> consumeUnchecked(final ThrowingConsumer<T> mayThrowOnAccept) {
        return input -> {
            try {
                mayThrowOnAccept.accept(input);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, U> BiConsumer<T, U> tryConsume(final ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (tInput, uInput) -> {
            try {
                mayThrowOnAccept.accept(tInput, uInput);
            } catch (Exception e) {
                // do nothing
            }
        };
    }

    public static <T, U> BiConsumer<T, U> consumeUnchecked(final ThrowingBiConsumer<T, U> mayThrowOnAccept) {
        return (tInput, uInput) -> {
            try {
                mayThrowOnAccept.accept(tInput, uInput);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }


    public static <T, P> Predicate<T> fundicate(final Function<T, P> inputFunction, final Predicate<P> testResult) {
        return input -> testResult.test(inputFunction.apply(input));
    }
}
