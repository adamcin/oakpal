/*
 * Copyright 2020 Mark Adamcin
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
import java.util.stream.Stream;

/**
 * @deprecated use {@link net.adamcin.oakpal.api.Fun}
 */
@Deprecated
public final class Fun {
    private Fun() {
        // no construct
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#streamIt(Object)}
     */
    @Deprecated
    public static <T> Stream<T>
    streamIt(final @Nullable T element) {
        return net.adamcin.oakpal.api.Fun.streamIt(element);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#streamOpt(Optional)}
     */
    @Deprecated
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Stream<T>
    streamOpt(final @NotNull Optional<T> element) {
        return net.adamcin.oakpal.api.Fun.streamOpt(element);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tee(Consumer)}
     */
    @Deprecated
    public static <T> @NotNull Function<T, T>
    tee(final @NotNull Consumer<? super T> consumer) {
        return net.adamcin.oakpal.api.Fun.tee(consumer);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#constantly1(Supplier)}
     */
    @Deprecated
    public static <T, R> @NotNull Function<T, R>
    constantly1(final @NotNull Supplier<? extends R> supplier) {
        return net.adamcin.oakpal.api.Fun.constantly1(supplier);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#constantly2(Supplier)}
     */
    @Deprecated
    public static <K, V, R> @NotNull BiFunction<K, V, R>
    constantly2(final @NotNull Supplier<? extends R> supplier) {
        return net.adamcin.oakpal.api.Fun.constantly2(supplier);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#compose1(Function, Function)}
     */
    @Deprecated
    public static <T, I, R> Function<T, R>
    compose(final @NotNull Function<T, ? extends I> before, final @NotNull Function<? super I, ? extends R> after) {
        return net.adamcin.oakpal.api.Fun.compose1(before, after);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#compose0(Supplier, Function)}
     */
    @Deprecated
    public static <R, S> Supplier<S>
    compose0(final @NotNull Supplier<? extends R> before, final @NotNull Function<? super R, ? extends S> after) {
        return net.adamcin.oakpal.api.Fun.compose0(before, after);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#compose2(BiFunction, Function)}
     */
    @Deprecated
    public static <K, V, I, R> BiFunction<K, V, R>
    compose2(final @NotNull BiFunction<K, V, ? extends I> before, final @NotNull Function<? super I, ? extends R> after) {
        return net.adamcin.oakpal.api.Fun.compose2(before, after);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTest1(Function, Predicate)}
     */
    @Deprecated
    public static <T, P> Predicate<T>
    composeTest(final @NotNull Function<? super T, ? extends P> inputFunction,
                final @NotNull Predicate<? super P> testResult) {
        return net.adamcin.oakpal.api.Fun.composeTest1(inputFunction, testResult);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTest2(Function, Function, BiPredicate)}
     */
    @Deprecated
    public static <K, V, P, Q> BiPredicate<K, V>
    composeTest2(final @NotNull Function<? super K, ? extends P> inputTFunction,
                 final @NotNull Function<? super V, ? extends Q> inputUFunction,
                 final @NotNull BiPredicate<? super P, ? super Q> testResult) {
        return net.adamcin.oakpal.api.Fun.composeTest2(inputTFunction, inputUFunction, testResult);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTest2(BiFunction, Predicate)}
     */
    @Deprecated
    public static <K, V, P> BiPredicate<K, V>
    composeTest2(final @NotNull BiFunction<? super K, ? super V, ? extends P> inputFunction,
                 final @NotNull Predicate<? super P> testResult) {
        return net.adamcin.oakpal.api.Fun.composeTest2(inputFunction, testResult);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#toVoid1(Function)}
     */
    @Deprecated
    public static <T> Consumer<T>
    toVoid1(final @NotNull Function<? super T, ?> inputFunction) {
        return net.adamcin.oakpal.api.Fun.toVoid1(inputFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#toVoid2(BiFunction)}
     */
    @Deprecated
    public static <K, V> BiConsumer<K, V>
    toVoid2(final @NotNull BiFunction<? super K, ? super V, ?> inputFunction) {
        return net.adamcin.oakpal.api.Fun.toVoid2(inputFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#infer1(Function)}
     */
    @Deprecated
    public static <T, R> Function<T, R>
    infer1(final @NotNull Function<? super T, ? extends R> methodRef) {
        return net.adamcin.oakpal.api.Fun.infer1(methodRef);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#infer2(BiFunction)}
     */
    @Deprecated
    public static <K, V, R> BiFunction<K, V, R>
    infer2(final @NotNull BiFunction<? super K, ? super V, ? extends R> methodRef) {
        return net.adamcin.oakpal.api.Fun.infer2(methodRef);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#infer0(Supplier)}
     */
    @Deprecated
    public static <T> Supplier<T>
    infer0(final @NotNull Supplier<? extends T> methodRef) {
        return net.adamcin.oakpal.api.Fun.infer0(methodRef);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#inferTest1(Predicate)}
     */
    @Deprecated
    public static <T> Predicate<T>
    inferTest1(final @NotNull Predicate<? super T> methodRef) {
        return net.adamcin.oakpal.api.Fun.inferTest1(methodRef);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#inferTest2(BiPredicate)}
     */
    @Deprecated
    public static <K, V> BiPredicate<K, V>
    inferTest2(final @NotNull BiPredicate<? super K, ? super V> methodRef) {
        return net.adamcin.oakpal.api.Fun.inferTest2(methodRef);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#entryTee(BiConsumer)}
     */
    @Deprecated
    public static <K, V> Function<Map.Entry<K, V>, Map.Entry<K, V>>
    entryTee(final @NotNull BiConsumer<? super K, ? super V> consumer) {
        return net.adamcin.oakpal.api.Fun.entryTee(consumer);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#zipKeysWithValueFunc(Function)}
     */
    @Deprecated
    public static <K, V> Function<K, Map.Entry<K, V>>
    zipKeysWithValueFunc(final @NotNull Function<? super K, ? extends V> valueFunc) {
        return net.adamcin.oakpal.api.Fun.zipKeysWithValueFunc(valueFunc);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#zipValuesWithKeyFunc(Function)}
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>>
    zipValuesWithKeyFunc(final @NotNull Function<? super V, ? extends K> keyFunction) {
        return net.adamcin.oakpal.api.Fun.zipValuesWithKeyFunc(keyFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#toEntry(Object, Object)}
     */
    @Deprecated
    public static <K, V> Map.Entry<K, V>
    toEntry(final @Nullable K key, final @Nullable V value) {
        return net.adamcin.oakpal.api.Fun.toEntry(key, value);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#keepFirstMerger()}
     */
    @Deprecated
    public static <V> BinaryOperator<V> keepFirstMerger() {
        return net.adamcin.oakpal.api.Fun.keepFirstMerger();
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#keepLastMerger()}
     */
    @Deprecated
    public static <V> BinaryOperator<V> keepLastMerger() {
        return net.adamcin.oakpal.api.Fun.keepLastMerger();
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#throwingMerger()}
     */
    @Deprecated
    public static <V> BinaryOperator<V> throwingMerger() {
        return net.adamcin.oakpal.api.Fun.throwingMerger();
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#entriesToMap()}
     */
    @Deprecated
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMap() {
        return net.adamcin.oakpal.api.Fun.entriesToMap();
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#entriesToMapOfType(Supplier)}
     */
    @Deprecated
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMapOfType(final @NotNull Supplier<Map<K, V>> mapSupplier) {
        return net.adamcin.oakpal.api.Fun.entriesToMapOfType(mapSupplier);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#entriesToMap(BinaryOperator)}
     */
    @Deprecated
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMap(final @NotNull BinaryOperator<V> mergeFunction) {
        return net.adamcin.oakpal.api.Fun.entriesToMap(mergeFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#entriesToMapOfType(Supplier, BinaryOperator)}
     */
    @Deprecated
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>>
    entriesToMapOfType(final @NotNull Supplier<Map<K, V>> mapSupplier, final @NotNull BinaryOperator<V> mergeFunction) {
        return net.adamcin.oakpal.api.Fun.entriesToMapOfType(mapSupplier, mergeFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#mapEntry(BiFunction)}
     */
    @Deprecated
    public static <K, V, R> Function<Map.Entry<K, V>, R>
    mapEntry(final @NotNull BiFunction<? super K, ? super V, ? extends R> biMapFunction) {
        return net.adamcin.oakpal.api.Fun.mapEntry(biMapFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#mapValue(BiFunction)}
     */
    @Deprecated
    public static <K, V, W> Function<Map.Entry<K, V>, Map.Entry<K, W>>
    mapValue(final @NotNull BiFunction<? super K, ? super V, ? extends W> valueBiFunction) {
        return net.adamcin.oakpal.api.Fun.mapValue(valueBiFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#mapValue(Function)}
     */
    @Deprecated
    public static <K, V, W> Function<Map.Entry<K, V>, Map.Entry<K, W>>
    mapValue(final @NotNull Function<? super V, ? extends W> valueFunction) {
        return net.adamcin.oakpal.api.Fun.mapValue(valueFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#mapKey(BiFunction)}
     */
    @Deprecated
    public static <K, V, L> Function<Map.Entry<K, V>, Map.Entry<L, V>>
    mapKey(final @NotNull BiFunction<? super K, ? super V, ? extends L> keyBiFunction) {
        return net.adamcin.oakpal.api.Fun.mapKey(keyBiFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#mapKey(Function)}
     */
    @Deprecated
    public static <K, V, L> Function<Map.Entry<K, V>, Map.Entry<L, V>>
    mapKey(final @NotNull Function<? super K, ? extends L> keyFunction) {
        return net.adamcin.oakpal.api.Fun.mapKey(keyFunction);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#onEntry(BiConsumer)}
     */
    @Deprecated
    public static <K, V> Consumer<Map.Entry<K, V>>
    onEntry(final @NotNull BiConsumer<? super K, ? super V> biConsumer) {
        return net.adamcin.oakpal.api.Fun.onEntry(biConsumer);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#onKey(Consumer)}
     */
    @Deprecated
    public static <K, V> Consumer<Map.Entry<K, V>>
    onKey(final @NotNull Consumer<? super K> consumer) {
        return net.adamcin.oakpal.api.Fun.onKey(consumer);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#onValue(Consumer)}
     */
    @Deprecated
    public static <K, V> Consumer<Map.Entry<K, V>>
    onValue(final @NotNull Consumer<? super V> consumer) {
        return net.adamcin.oakpal.api.Fun.onValue(consumer);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#testEntry(BiPredicate)}
     */
    @Deprecated
    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testEntry(final @NotNull BiPredicate<? super K, ? super V> biPredicate) {
        return net.adamcin.oakpal.api.Fun.testEntry(biPredicate);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#testValue(Predicate)}
     */
    @Deprecated
    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testValue(final @NotNull Predicate<? super V> valuePredicate) {
        return net.adamcin.oakpal.api.Fun.testValue(valuePredicate);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#testKey(Predicate)}
     */
    @Deprecated
    public static <K, V> Predicate<? super Map.Entry<K, V>>
    testKey(final @NotNull Predicate<? super K> keyPredicate) {
        return net.adamcin.oakpal.api.Fun.testKey(keyPredicate);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#inSet(Collection)}
     */
    @Deprecated
    public static <T, S extends Collection<? super T>> Predicate<T>
    inSet(final @NotNull S haystack) {
        return net.adamcin.oakpal.api.Fun.inSet(haystack);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#isKeyIn(Map)}
     */
    @Deprecated
    public static <K, M extends Map<? super K, ?>> Predicate<K>
    isKeyIn(final @NotNull M haystack) {
        return net.adamcin.oakpal.api.Fun.isKeyIn(haystack);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#isValueIn(Map)}
     */
    @Deprecated
    public static <V, M extends Map<?, ? super V>> Predicate<V>
    isValueIn(final @NotNull M haystack) {
        return net.adamcin.oakpal.api.Fun.isValueIn(haystack);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTry1(Function, Supplier, net.adamcin.oakpal.api.Fun.ThrowingFunction, BiConsumer)}
     */
    @Deprecated
    public static <M, T, R> Function<T, M>
    composeTry(final @NotNull Function<? super R, ? extends M> monadUnit,
               final @NotNull Supplier<? extends M> monadZero,
               final @NotNull net.adamcin.oakpal.api.Fun.ThrowingFunction<? super T, ? extends R> mayThrowOnApply,
               final @Nullable BiConsumer<? super T, ? super Exception> onError) {
        return net.adamcin.oakpal.api.Fun.composeTry1(monadUnit, monadZero, mayThrowOnApply, onError);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTry1(Function, Function, net.adamcin.oakpal.api.Fun.ThrowingFunction)}
     */
    @Deprecated
    public static <M, T, R> Function<T, M>
    composeTry(final @NotNull Function<? super R, ? extends M> monoidSuccess,
               final @NotNull Function<? super Exception, ? extends M> monoidError,
               final @NotNull net.adamcin.oakpal.api.Fun.ThrowingFunction<? super T, ? extends R> mayThrowOnApply) {
        return net.adamcin.oakpal.api.Fun.composeTry1(monoidSuccess, monoidError, mayThrowOnApply);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTry0(Function, Supplier, net.adamcin.oakpal.api.Fun.ThrowingSupplier, Consumer)}
     */
    @Deprecated
    public static <M, R> Supplier<M>
    composeTry0(final @NotNull Function<? super R, ? extends M> monadUnit,
                final @NotNull Supplier<? extends M> monadZero,
                final @NotNull net.adamcin.oakpal.api.Fun.ThrowingSupplier<? extends R> mayThrowOnGet,
                final @Nullable Consumer<? super Exception> onError) {
        return net.adamcin.oakpal.api.Fun.composeTry0(monadUnit, monadZero, mayThrowOnGet, onError);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTry0(Function, Function, net.adamcin.oakpal.api.Fun.ThrowingSupplier)}
     */
    @Deprecated
    public static <M, R> Supplier<M>
    composeTry0(final @NotNull Function<? super R, ? extends M> monoidSuccess,
                final @NotNull Function<? super Exception, ? extends M> monoidError,
                final @NotNull net.adamcin.oakpal.api.Fun.ThrowingSupplier<? extends R> mayThrowOnGet) {
        return net.adamcin.oakpal.api.Fun.composeTry0(monoidSuccess, monoidError, mayThrowOnGet);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTry2(Function, Supplier, net.adamcin.oakpal.api.Fun.ThrowingBiFunction, BiConsumer)}
     */
    @Deprecated
    public static <M, K, V, R> BiFunction<K, V, M>
    composeTry2(final @NotNull Function<? super R, ? extends M> monadUnit,
                final @NotNull Supplier<? extends M> monadZero,
                final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiFunction<? super K, ? super V, ? extends R> mayThrowOnApply,
                final @Nullable BiConsumer<? super Map.Entry<? super K, ? super V>, ? super Exception> onError) {
        return net.adamcin.oakpal.api.Fun.composeTry2(monadUnit, monadZero, mayThrowOnApply, onError);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#composeTry2(Function, Function, net.adamcin.oakpal.api.Fun.ThrowingBiFunction)}
     */
    @Deprecated
    public static <M, K, V, R> BiFunction<K, V, M>
    composeTry2(final @NotNull Function<? super R, ? extends M> monoidSuccess,
                final @NotNull Function<? super Exception, ? extends M> monoidError,
                final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiFunction<? super K, ? super V, ? extends R> mayThrowOnApply) {
        return net.adamcin.oakpal.api.Fun.composeTry2(monoidSuccess, monoidError, mayThrowOnApply);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheck0(net.adamcin.oakpal.api.Fun.ThrowingSupplier)}
     */
    @Deprecated
    public static <R> Supplier<R>
    uncheck0(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingSupplier<? extends R> mayThrowOnGet) {
        return net.adamcin.oakpal.api.Fun.uncheck0(mayThrowOnGet);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheck1(net.adamcin.oakpal.api.Fun.ThrowingFunction)}
     */
    @Deprecated
    public static <T, R> Function<T, R>
    uncheck1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingFunction<? super T, ? extends R> mayThrowOnApply) {
        return net.adamcin.oakpal.api.Fun.uncheck1(mayThrowOnApply);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheck2(net.adamcin.oakpal.api.Fun.ThrowingBiFunction)}
     */
    @Deprecated
    public static <K, V, R> BiFunction<K, V, R>
    uncheck2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiFunction<? super K, ? super V, ? extends R> mayThrowOnApply) {
        return net.adamcin.oakpal.api.Fun.uncheck2(mayThrowOnApply);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheckTest1(net.adamcin.oakpal.api.Fun.ThrowingPredicate)}
     */
    @Deprecated
    public static <T> Predicate<T>
    uncheckTest1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingPredicate<? super T> mayThrowOnTest) {
        return net.adamcin.oakpal.api.Fun.uncheckTest1(mayThrowOnTest);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheckTest2(net.adamcin.oakpal.api.Fun.ThrowingBiPredicate)}
     */
    @Deprecated
    public static <K, V> BiPredicate<K, V>
    uncheckTest2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiPredicate<? super K, ? super V> mayThrowOnTest) {
        return net.adamcin.oakpal.api.Fun.uncheckTest2(mayThrowOnTest);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheckVoid1(net.adamcin.oakpal.api.Fun.ThrowingConsumer)}
     */
    @Deprecated
    public static <T> Consumer<T>
    uncheckVoid1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingConsumer<? super T> mayThrowOnAccept) {
        return net.adamcin.oakpal.api.Fun.uncheckVoid1(mayThrowOnAccept);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#uncheckVoid2(net.adamcin.oakpal.api.Fun.ThrowingBiConsumer)}
     */
    @Deprecated
    public static <K, V> BiConsumer<K, V>
    uncheckVoid2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiConsumer<? super K, ? super V> mayThrowOnAccept) {
        return net.adamcin.oakpal.api.Fun.uncheckVoid2(mayThrowOnAccept);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#testOrDefault1(net.adamcin.oakpal.api.Fun.ThrowingPredicate, boolean)}
     */
    @Deprecated
    public static <T> Predicate<T>
    testOrDefault1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingPredicate<? super T> mayThrowOnTest, boolean defaultValue) {
        return net.adamcin.oakpal.api.Fun.testOrDefault1(mayThrowOnTest, defaultValue);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#testOrDefault2(net.adamcin.oakpal.api.Fun.ThrowingBiPredicate, boolean)}
     */
    @Deprecated
    public static <K, V> BiPredicate<K, V>
    testOrDefault2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiPredicate<? super K, ? super V> mayThrowOnTest, boolean defaultValue) {
        return net.adamcin.oakpal.api.Fun.testOrDefault2(mayThrowOnTest, defaultValue);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrDefault0(net.adamcin.oakpal.api.Fun.ThrowingSupplier, Object)}
     */
    @Deprecated
    public static <R> Supplier<R>
    tryOrDefault0(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingSupplier<R> mayThrowOnGet, @Nullable R defaultValue) {
        return net.adamcin.oakpal.api.Fun.tryOrDefault0(mayThrowOnGet, defaultValue);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrDefault1(net.adamcin.oakpal.api.Fun.ThrowingFunction, Object)}
     */
    @Deprecated
    public static <T, R> Function<T, R>
    tryOrDefault1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingFunction<? super T, R> mayThrowOnApply, @Nullable R defaultValue) {
        return net.adamcin.oakpal.api.Fun.tryOrDefault1(mayThrowOnApply, defaultValue);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrDefault2(net.adamcin.oakpal.api.Fun.ThrowingBiFunction, Object)}
     */
    @Deprecated
    public static <K, V, R> BiFunction<K, V, R>
    tryOrDefault2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiFunction<? super K, ? super V, R> mayThrowOnApply, @Nullable R defaultValue) {
        return net.adamcin.oakpal.api.Fun.tryOrDefault2(mayThrowOnApply, defaultValue);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrOptional0(net.adamcin.oakpal.api.Fun.ThrowingSupplier)}
     */
    @Deprecated
    public static <R> Supplier<Optional<R>>
    tryOrOptional0(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingSupplier<R> mayThrowOnGet) {
        return net.adamcin.oakpal.api.Fun.tryOrOptional0(mayThrowOnGet);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrOptional1(net.adamcin.oakpal.api.Fun.ThrowingFunction)}
     */
    @Deprecated
    public static <T, R> Function<T, Optional<R>>
    tryOrOptional1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingFunction<? super T, R> mayThrowOnApply) {
        return net.adamcin.oakpal.api.Fun.tryOrOptional1(mayThrowOnApply);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrOptional2(net.adamcin.oakpal.api.Fun.ThrowingBiFunction)}
     */
    @Deprecated
    public static <K, V, R> BiFunction<K, V, Optional<R>>
    tryOrOptional2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiFunction<? super K, ? super V, R> mayThrowOnApply) {
        return net.adamcin.oakpal.api.Fun.tryOrOptional2(mayThrowOnApply);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrVoid1(net.adamcin.oakpal.api.Fun.ThrowingConsumer)}
     */
    @Deprecated
    public static <T> Consumer<T>
    tryOrVoid1(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingConsumer<? super T> mayThrowOnAccept) {
        return net.adamcin.oakpal.api.Fun.tryOrVoid1(mayThrowOnAccept);
    }

    /**
     * @deprecated use {@link net.adamcin.oakpal.api.Fun#tryOrVoid2(net.adamcin.oakpal.api.Fun.ThrowingBiConsumer)}
     */
    @Deprecated
    public static <K, V> BiConsumer<K, V>
    tryOrVoid2(final @NotNull net.adamcin.oakpal.api.Fun.ThrowingBiConsumer<? super K, ? super V> mayThrowOnAccept) {
        return net.adamcin.oakpal.api.Fun.tryOrVoid2(mayThrowOnAccept);
    }
}
