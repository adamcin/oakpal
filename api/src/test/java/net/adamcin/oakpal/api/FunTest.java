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

package net.adamcin.oakpal.api;

import static net.adamcin.oakpal.api.Fun.compose;
import static net.adamcin.oakpal.api.Fun.compose0;
import static net.adamcin.oakpal.api.Fun.compose2;
import static net.adamcin.oakpal.api.Fun.composeTest;
import static net.adamcin.oakpal.api.Fun.composeTest2;
import static net.adamcin.oakpal.api.Fun.composeTry;
import static net.adamcin.oakpal.api.Fun.composeTry0;
import static net.adamcin.oakpal.api.Fun.composeTry2;
import static net.adamcin.oakpal.api.Fun.entriesToMap;
import static net.adamcin.oakpal.api.Fun.entriesToMapOfType;
import static net.adamcin.oakpal.api.Fun.entryTee;
import static net.adamcin.oakpal.api.Fun.inSet;
import static net.adamcin.oakpal.api.Fun.infer0;
import static net.adamcin.oakpal.api.Fun.infer1;
import static net.adamcin.oakpal.api.Fun.infer2;
import static net.adamcin.oakpal.api.Fun.inferTest1;
import static net.adamcin.oakpal.api.Fun.inferTest2;
import static net.adamcin.oakpal.api.Fun.isKeyIn;
import static net.adamcin.oakpal.api.Fun.isValueIn;
import static net.adamcin.oakpal.api.Fun.keepFirstMerger;
import static net.adamcin.oakpal.api.Fun.keepLastMerger;
import static net.adamcin.oakpal.api.Fun.mapEntry;
import static net.adamcin.oakpal.api.Fun.mapKey;
import static net.adamcin.oakpal.api.Fun.mapValue;
import static net.adamcin.oakpal.api.Fun.onEntry;
import static net.adamcin.oakpal.api.Fun.onKey;
import static net.adamcin.oakpal.api.Fun.onValue;
import static net.adamcin.oakpal.api.Fun.result0;
import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.api.Fun.result2;
import static net.adamcin.oakpal.api.Fun.resultNothing1;
import static net.adamcin.oakpal.api.Fun.resultNothing2;
import static net.adamcin.oakpal.api.Fun.testEntry;
import static net.adamcin.oakpal.api.Fun.testKey;
import static net.adamcin.oakpal.api.Fun.testOrDefault1;
import static net.adamcin.oakpal.api.Fun.testOrDefault2;
import static net.adamcin.oakpal.api.Fun.testValue;
import static net.adamcin.oakpal.api.Fun.throwingMerger;
import static net.adamcin.oakpal.api.Fun.throwingVoidToNothing1;
import static net.adamcin.oakpal.api.Fun.throwingVoidToNothing2;
import static net.adamcin.oakpal.api.Fun.toEntry;
import static net.adamcin.oakpal.api.Fun.toVoid1;
import static net.adamcin.oakpal.api.Fun.toVoid2;
import static net.adamcin.oakpal.api.Fun.tryOrDefault0;
import static net.adamcin.oakpal.api.Fun.tryOrDefault1;
import static net.adamcin.oakpal.api.Fun.tryOrDefault2;
import static net.adamcin.oakpal.api.Fun.tryOrOptional0;
import static net.adamcin.oakpal.api.Fun.tryOrOptional1;
import static net.adamcin.oakpal.api.Fun.tryOrOptional2;
import static net.adamcin.oakpal.api.Fun.tryOrVoid1;
import static net.adamcin.oakpal.api.Fun.tryOrVoid2;
import static net.adamcin.oakpal.api.Fun.uncheck0;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.Fun.uncheck2;
import static net.adamcin.oakpal.api.Fun.uncheckTest1;
import static net.adamcin.oakpal.api.Fun.uncheckTest2;
import static net.adamcin.oakpal.api.Fun.uncheckVoid1;
import static net.adamcin.oakpal.api.Fun.uncheckVoid2;
import static net.adamcin.oakpal.api.Fun.zipKeysWithValueFunc;
import static net.adamcin.oakpal.api.Fun.zipValuesWithKeyFunc;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class FunTest {
    final String sentinel = "sentinel";

    @Test
    public void testStreamIt() {
        Stream<String> strings = Fun.streamIt(sentinel);
        final Optional<String> testHead = strings.findFirst();
        assertTrue("value should be present: " + testHead, testHead.isPresent());
        assertSame("value should be same as sentinel", sentinel, testHead.get());
    }

    @Test
    public void testStreamOpt() {
        final Optional<String> sentinel = Optional.of("sentinel");
        Stream<String> strings = Fun.streamOpt(sentinel);
        final Optional<String> testHead = strings.findFirst();
        assertTrue("value should be present: " + testHead, testHead.isPresent());
        assertSame("value should be same as sentinel", sentinel.get(), testHead.get());
    }

    @Test
    public void testTee() throws Exception {
        final CompletableFuture<String> latch = new CompletableFuture<>();
        final Consumer<String> consumer = latch::complete;
        final String value = Fun.tee(consumer).apply(sentinel);
        assertTrue("future should be done: " + latch, latch.isDone());
        assertSame("value should be same as input", sentinel, value);
        assertSame("latched should be same as input", sentinel, latch.get());
    }


    @Test
    public void testConstantly1() {
        final Supplier<String> supplier = () -> sentinel;
        final Function<Boolean, String> function = Fun.constantly1(supplier);
        final String[] results = Stream.of(Boolean.TRUE, Boolean.FALSE).map(function).toArray(String[]::new);
        assertArrayEquals("results should be [sentinel, sentinel]: " + Arrays.toString(results),
                new String[]{sentinel, sentinel}, results);
    }

    @Test
    public void testConstantly2() {
        final Supplier<String> supplier = () -> sentinel;
        final BiFunction<Boolean, Boolean, String> function = Fun.constantly2(supplier);
        final String[] results = Stream.of(
                Fun.toEntry(Boolean.TRUE, Boolean.FALSE),
                Fun.toEntry(Boolean.FALSE, Boolean.TRUE)
        ).map(mapEntry(function)).toArray(String[]::new);
        assertArrayEquals("results should be [sentinel, sentinel]: " + Arrays.toString(results),
                new String[]{sentinel, sentinel}, results);
    }

    @Test
    public void testCompose() {
        assertEquals("compose concat then uppercase", "SENTINELSENTINEL",
                compose(sentinel::concat, String::toUpperCase).apply(sentinel));
    }

    @Test
    public void testCompose0() {
        assertEquals("compose get sentinel with uppercase", "SENTINEL",
                compose0(() -> sentinel, String::toUpperCase).get());
    }

    @Test
    public void testCompose2() {
        assertEquals("compose sentinel+true with lowercase", "sentineltrue",
                compose2((String string, Boolean bool) -> string + bool, String::toLowerCase)
                        .apply(sentinel, true));
    }

    @Test
    public void testComposeTest() {
        final Predicate<String> predicate = composeTest(sentinel::concat,
                string -> string.length() > sentinel.length());
        assertFalse("compose concat empty string has no more length than sentinel", predicate.test(""));
        assertTrue("compose concat empty string has more length than sentinel", predicate.test("more"));
    }

    @Test
    public void testComposeTest2() {
        final Function<String, String> substring4 = string -> string.substring(0, 4);
        final Function<String, String> substring6 = string -> string.substring(0, 6);
        final BiPredicate<String, String> truePredicate = composeTest2(substring6, substring4, String::startsWith);
        final BiPredicate<String, String> falsePredicate = composeTest2(substring4, substring6, String::startsWith);
        assertTrue("compose substrings of different lengths (left longer) then String::startsWith",
                truePredicate.test(sentinel, sentinel));
        assertFalse("compose substrings of different lengths (right longer) then String::startsWith",
                falsePredicate.test(sentinel, sentinel));
    }

    @Test
    public void testComposeTest2BiFunction() {
        final BiFunction<String, Integer, String> chomp = (string, length) -> string.substring(0, length);
        final BiPredicate<String, Integer> sentinelStartsWith = composeTest2(chomp, sentinel::startsWith);
        assertTrue("chomp an input string to a length test if sentinel::startsWith",
                sentinelStartsWith.test("senile", 3));
        assertFalse("chomp an input string to a length test if sentinel::startsWith",
                sentinelStartsWith.test("straighten", 2));
    }

    @Test
    public void testToVoid1() {
        final Function<String, String> func = String::toUpperCase;
        final Consumer<String> cons = toVoid1(func);
        cons.accept(sentinel);
    }

    @Test
    public void testToVoid2() {
        final BiFunction<String, String, String> func = String::concat;
        final BiConsumer<String, String> cons = toVoid2(func);
        cons.accept(sentinel, sentinel);
    }

    @Test
    public void testInfer1() {
        final Function<String, String> func = String::toUpperCase;
        final Function<String, String> inferred = infer1(String::toUpperCase);
        assertEquals("do these equate?", func.apply(sentinel), inferred.apply(sentinel));
    }

    @Test
    public void testInfer2() {
        final BiFunction<String, String, String> func = String::concat;
        final BiFunction<String, String, String> inferred = infer2(String::concat);
        assertEquals("do these equate?",
                func.apply(sentinel, sentinel), inferred.apply(sentinel, sentinel));
    }

    private class TestInfer0Provider {
        String getValue() {
            return sentinel;
        }
    }

    @Test
    public void testInfer0() {
        final TestInfer0Provider provider = new TestInfer0Provider();
        final Supplier<String> supplier = infer0(provider::getValue);
        assertEquals("provider should supply sentinel", sentinel, supplier.get());
    }

    @Test
    public void testInferTest1() {
        final Predicate<String> test = inferTest1(String::isEmpty);
        assertTrue("test for is empty", test.test(""));
        assertFalse("test for is not empty", test.test("abcde"));
    }

    @Test
    public void testInferTest2() {
        final BiPredicate<String, String> test = inferTest2(String::equalsIgnoreCase);
        assertTrue("test for /i equality", test.test("a", "A"));
        assertFalse("test for /i equality", test.test("b", "A"));
    }

    @Test
    public void testThrowingVoidToNothing1() throws Exception {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingConsumer<Integer> consumer = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(value);
        };
        assertSame("should be the same nothing",
                Nothing.instance, throwingVoidToNothing1(consumer).tryApply(2));
        assertEquals("latch value should be 2", 2, latch.get());
    }

    @Test
    public void testThrowingVoidToNothing2() throws Exception {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingBiConsumer<String, Integer> consumer = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(newValue);
        };

        assertSame("should be the same nothing",
                Nothing.instance, throwingVoidToNothing2(consumer).tryApply("", 2));
        assertEquals("latch value should be 2", 2, latch.get());
    }

    @Test
    public void testEntryTee() {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final Map.Entry<String, Boolean> entry = toEntry("foo", false);
        final boolean boolResult = Stream.of(entry)
                .map(entryTee((key, value) -> future.complete(key)))
                .map(Map.Entry::getValue)
                .findFirst().orElse(true);
        assertFalse("test value", boolResult);
        assertEquals("key is correct", "foo", future.getNow(""));
    }

    @Test
    public void testZipKeysWithValueFunc() {
        final Map<String, Integer> lengths = Stream.of("foo", "bar", "acme", "baboon")
                .collect(Collectors.toMap(Function.identity(), String::length));
        final Map<String, Integer> collected = Stream.of("foo")
                .map(zipKeysWithValueFunc(lengths::get))
                .collect(Fun.entriesToMap());
        assertEquals("zipKeys map should match", Collections.singletonMap("foo", 3), collected);
    }

    @Test
    public void testZipValuesWithKeyFunc() {
        final Map<Integer, String> lengths = Stream.of("bar", "acme", "baboon")
                .collect(Collectors.toMap(String::length, Function.identity()));
        final Map<String, Integer> collected = Stream.of(4)
                .map(zipValuesWithKeyFunc(lengths::get))
                .collect(Fun.entriesToMap());
        assertEquals("zipValues map should match", Collections.singletonMap("acme", 4), collected);
    }

    @Test
    public void testToEntry() {
        final Map.Entry<String, String> entry = toEntry("foo", "bar");
        assertEquals("key should be foo", "foo", entry.getKey());
        assertEquals("value should be bar", "bar", entry.getValue());
    }

    @Test
    public void testEntriesToMap() {
        final Map<String, Integer> parallelConstruction = new HashMap<>();
        parallelConstruction.put("one", 1);
        parallelConstruction.put("two", 2);
        parallelConstruction.put("three", 3);
        final Map<String, Integer> collected = Stream.of(toEntry("one", 1), toEntry("two", 2), toEntry("three", 3))
                .collect(entriesToMap());
        assertEquals("entriesToMap should match with puts", parallelConstruction, collected);
        assertTrue("collected map should be a LinkedHashMap", collected instanceof LinkedHashMap);
    }

    @Test
    public void testEntriesToMapOfType() {
        final Map<String, Integer> collected = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMapOfType(HashMap::new));
        assertEquals("value of 'one' should be 2", Integer.valueOf(2), collected.get("one"));
        assertTrue("collected map should be a HashMap", collected instanceof HashMap);
        assertFalse("collected map should not be a LinkedHashMap", collected instanceof LinkedHashMap);
    }

    @Test
    public void testEntriesToMapOfTypeWithMerger() {
        final Map<String, Integer> collected = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMapOfType(HashMap::new, keepLastMerger()));
        assertEquals("value of 'one' should be 2", Integer.valueOf(2), collected.get("one"));
        assertTrue("collected map should be a HashMap", collected instanceof HashMap);
        assertFalse("collected map should not be a LinkedHashMap", collected instanceof LinkedHashMap);
        final Map<String, Integer> collectedFirst = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMapOfType(TreeMap::new, keepFirstMerger()));
        assertEquals("value of 'one' should be 1", Integer.valueOf(1), collectedFirst.get("one"));
        assertTrue("collected map should be a TreeMap", collectedFirst instanceof TreeMap);
    }

    @Test(expected = IllegalStateException.class)
    public void testEntriesToMapOfTypeAndThrows() {
        final Map<String, Integer> collected = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMapOfType(TreeMap::new, throwingMerger()));
    }

    @Test
    public void testKeepFirstMerger() {
        final Map<String, Integer> collected = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMap(keepFirstMerger()));
        assertEquals("value of 'one' should be 1", Integer.valueOf(1), collected.get("one"));
    }

    @Test
    public void testKeepLastMerger() {
        final Map<String, Integer> collected = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMap(keepLastMerger()));
        assertEquals("value of 'one' should be 2", Integer.valueOf(2), collected.get("one"));
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowingMerger() {
        final Map<String, Integer> collected = Stream.of(
                toEntry("one", 1),
                toEntry("one", 2),
                toEntry("three", 3))
                .collect(entriesToMap(throwingMerger()));
    }

    @Test
    public void testEntriesToMapWithMerge() {
        final Map<String, Integer> parallelConstruction = new HashMap<>();
        parallelConstruction.put("one", 4);
        parallelConstruction.put("two", 2);
        final Map<String, Integer> collected = Stream.of(toEntry("one", 1), toEntry("two", 2), toEntry("one", 3))
                .collect(entriesToMap((int1, int2) -> int1 + int2));
        assertEquals("entriesToMap should match with puts", parallelConstruction, collected);
    }

    @Test
    public void testMapEntry() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        List<String> collected = refMap.entrySet().stream()
                .map(mapEntry((string, number) -> string + "_" + number)).collect(Collectors.toList());
        assertEquals("mapEntry should result in same list",
                Arrays.asList("one_1", "two_2", "three_3"), collected);
    }

    @Test
    public void testMapKey() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Set<String> collected = refMap.entrySet().stream()
                .map(mapKey(string -> string + "_" + string)).collect(entriesToMap()).keySet();
        assertEquals("mapKey should result in same list",
                new LinkedHashSet<>(Arrays.asList("one_one", "two_two", "three_three")), collected);
    }

    @Test
    public void testMapKeyBiFunction() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Set<String> collected = refMap.entrySet().stream()
                .map(mapKey((string, number) -> string + "_" + number)).collect(entriesToMap()).keySet();
        assertEquals("mapKey should result in same list",
                new LinkedHashSet<>(Arrays.asList("one_1", "two_2", "three_3")), collected);
    }

    @Test
    public void testMapValue() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        List<Integer> collected = refMap.entrySet().stream()
                .map(mapValue(number -> number * 2))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        assertEquals("mapValue should result in same list",
                Arrays.asList(2, 4, 6), collected);
    }

    @Test
    public void testMapValueBiFunction() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        List<Integer> collected = refMap.entrySet().stream()
                .map(mapValue((string, number) -> string.startsWith("t") ? number * 2 : number))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        assertEquals("mapValue bifunc should result in same list",
                Arrays.asList(1, 4, 6), collected);
    }

    @Test
    public void testOnEntry() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        refMap.entrySet().forEach(onEntry((string, number) -> latch.addAndGet(number)));
        assertEquals("onEntry should add 1, 2, and 3 (6) to latch", 6, latch.get());
    }

    @Test
    public void testOnKey() {
        final AtomicReference<String> latch = new AtomicReference<>("");
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        refMap.entrySet().forEach(onKey(string -> latch.accumulateAndGet(string, String::concat)));
        assertTrue("onKey latch should contain 'one'", latch.get().contains("one"));
        assertTrue("onKey latch should contain 'two'", latch.get().contains("two"));
        assertTrue("onKey latch should contain 'three'", latch.get().contains("three"));
    }

    @Test
    public void testOnValue() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        refMap.entrySet().forEach(onValue(latch::addAndGet));
        assertEquals("onValue should add 1, 2, and 3 (6) to latch", 6, latch.get());
    }

    @Test
    public void testTestEntry() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Map<String, Integer> collected = refMap.entrySet().stream()
                .filter(testEntry((string, number) -> number % 2 == 0))
                .collect(entriesToMap());
        assertEquals("testEntry map should match", Collections.singletonMap("two", 2), collected);
    }

    @Test
    public void testTestKey() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Map<String, Integer> collected = refMap.entrySet().stream()
                .filter(testKey(string -> string.endsWith("o")))
                .collect(entriesToMap());
        assertEquals("testKey map should match", Collections.singletonMap("two", 2), collected);
    }

    @Test
    public void testTestValue() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Map<String, Integer> collected = refMap.entrySet().stream()
                .filter(testValue(number -> number % 2 == 0))
                .collect(entriesToMap());
        assertEquals("testValue map should match", Collections.singletonMap("two", 2), collected);
    }

    @Test
    public void testInSet() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Set<String> set = refMap.keySet();

        String[] filtered = Stream.of("one", "two", "tree", "four").filter(inSet(set).negate()).toArray(String[]::new);
        assertArrayEquals("filtered should contain tree and four", new String[]{"tree", "four"}, filtered);
    }

    @Test
    public void testIsKeyIn() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);

        String[] filtered = Stream.of("one", "two", "tree", "four")
                .filter(isKeyIn(refMap).negate()).toArray(String[]::new);
        assertArrayEquals("filtered should contain tree and four", new String[]{"tree", "four"}, filtered);
    }

    @Test
    public void testIsValueIn() {
        final Map<String, Integer> refMap = new LinkedHashMap<>();
        refMap.put("one", 1);
        refMap.put("two", 2);
        refMap.put("three", 3);
        Integer[] filtered = Stream.of(5, 1, 2, 3, 4)
                .filter(isValueIn(refMap).negate()).toArray(Integer[]::new);
        assertArrayEquals("filtered should contain 5 and 4", new Integer[]{5, 4}, filtered);
    }

    @Test
    public void testComposeTry_noOnError() {
        final Fun.ThrowingFunction<String, Class<?>> classLoader = this.getClass().getClassLoader()::loadClass;
        final Function<String, Stream<Class<?>>> func =
                composeTry(Stream::of, Stream::empty, classLoader, null);
        final String notARealClassName = "net.adamcin.oakpal.core.NotARealClass";
        final Class<?>[] loadedClasses = Stream.of("java.lang.String", notARealClassName, "java.util.Map")
                .flatMap(func).toArray(Class<?>[]::new);
        assertArrayEquals("loadedClasses should contain String and Map",
                new Class<?>[]{String.class, Map.class}, loadedClasses);
    }

    @Test
    public void testComposeTry() {
        final Fun.ThrowingFunction<String, Class<?>> classLoader = this.getClass().getClassLoader()::loadClass;
        final Map<String, Exception> collectedErrors = new HashMap<>();
        final Function<String, Stream<Class<?>>> func =
                composeTry(Stream::of, Stream::empty, classLoader, collectedErrors::put);
        final String notARealClassName = "net.adamcin.oakpal.core.NotARealClass";
        final Class<?>[] loadedClasses = Stream.of("java.lang.String", notARealClassName, "java.util.Map")
                .flatMap(func).toArray(Class<?>[]::new);
        assertArrayEquals("loadedClasses should contain String and Map",
                new Class<?>[]{String.class, Map.class}, loadedClasses);
        assertTrue("collectedErrors should contain key " + notARealClassName,
                collectedErrors.containsKey(notARealClassName));
        final Exception error = collectedErrors.get(notARealClassName);
        assertTrue("error should be ClassNotFoundException " + error.getClass().getName(),
                error instanceof ClassNotFoundException);
    }

    @Test
    public void testComposeTryResult() {
        final Fun.ThrowingFunction<String, Class<?>> classLoader = this.getClass().getClassLoader()::loadClass;
        final Map<String, Exception> collectedErrors = new HashMap<>();
        final Function<String, Result<Class<?>>> func = composeTry(Result::success, Result::failure, classLoader);
        final String notARealClassName = "net.adamcin.oakpal.core.NotARealClass";
        final Map<String, Result<Class<?>>> results = Stream
                .of("java.lang.String", notARealClassName, "java.util.Map")
                .map(zipKeysWithValueFunc(func)).collect(entriesToMap());
        final Class<?>[] loadedClasses = results.values().stream().flatMap(Result::stream).toArray(Class<?>[]::new);

        results.entrySet().stream()
                .filter(testValue(Result::isFailure))
                .flatMap(entry -> entry.getValue().findCause(ClassNotFoundException.class)
                        .map(cause -> Stream.of(toEntry(entry.getKey(), cause))).orElse(Stream.empty()))
                .forEach(onEntry(collectedErrors::put));
        assertArrayEquals("loadedClasses should contain String and Map",
                new Class<?>[]{String.class, Map.class}, loadedClasses);
        assertTrue("collectedErrors should contain key " + notARealClassName,
                collectedErrors.containsKey(notARealClassName));
        final Exception error = collectedErrors.get(notARealClassName);
        assertTrue("error should be ClassNotFoundException " + error.getClass().getName(),
                error instanceof ClassNotFoundException);
    }

    @Test
    public void testComposeTry0() {
        final List<Exception> collectedErrors = new ArrayList<>();
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        Integer[] ints = Stream.generate(composeTry0(Stream::of, Stream::empty, supplier, collectedErrors::add))
                .limit(10).flatMap(Function.identity())
                .toArray(Integer[]::new);
        assertEquals("collected three errors", 3, collectedErrors.size());

        latch.set(0);

        Integer[] ints2 = Stream.generate(composeTry0(Stream::of, Stream::empty, supplier, null))
                .limit(10).flatMap(Function.identity())
                .toArray(Integer[]::new);
        assertArrayEquals("ints should contain no multiples of 3, up to ten 10",
                new Integer[]{1, 2, 4, 5, 7, 8, 10}, ints2);
    }

    @Test
    public void testComposeTry0Result() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        List<Result<Integer>> results = Stream.generate(composeTry0(Result::success, Result::<Integer>failure, supplier))
                .limit(10).collect(Collectors.toList());

        assertEquals("collected three errors", 3, results.stream().filter(Result::isFailure).count());
    }

    @Test
    public void testComposeTry2() {
        final List<Exception> collectedErrors = new ArrayList<>();
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .flatMap(mapEntry(composeTry2(Stream::of, Stream::empty, function,
                        (entry, error) -> collectedErrors.add(error))))
                .toArray(Integer[]::new);
        assertEquals("collected three errors", 3, collectedErrors.size());
    }

    @Test
    public void testComposeTry2Result() {
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        List<Result<Integer>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .map(mapEntry(composeTry2(Result::success, Result::<Integer>failure, function)))
                .collect(Collectors.toList());
        assertEquals("collected three errors", 3, results.stream().filter(Result::isFailure).count());
    }

    @Test
    public void testUncheck0() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        Integer[] ints = Stream.generate(uncheck0(supplier)).limit(2).toArray(Integer[]::new);
        assertArrayEquals("ints should contain 1 and 2", new Integer[]{1, 2}, ints);
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheck0AndThrow() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        // should throw
        Integer[] ints = Stream.generate(uncheck0(supplier)).limit(3).toArray(Integer[]::new);
    }

    @Test
    public void testResult0() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        final List<Result<Integer>> results = Stream.generate(result0(supplier)).limit(10).collect(Collectors.toList());
        assertEquals("results should have three failures", 3, results.stream().filter(Result::isFailure).count());
        final Integer[] ints = results.stream().flatMap(Result::stream).toArray(Integer[]::new);
        assertArrayEquals("ints should contain no multiples of 3, up to ten 10",
                new Integer[]{1, 2, 4, 5, 7, 8, 10}, ints);
    }

    @Test
    public void testUncheck1() {
        final Fun.ThrowingFunction<Integer, Integer> function = newValue -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(uncheck1(function)).limit(2).toArray(Integer[]::new);
        assertArrayEquals("ints should contain 1 and 2", new Integer[]{1, 2}, ints);
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheck1AndThrow() {
        final Fun.ThrowingFunction<Integer, Integer> function = newValue -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        // should throw
        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(uncheck1(function)).limit(3).toArray(Integer[]::new);
    }

    @Test
    public void testResult1() {
        final Fun.ThrowingFunction<Integer, Integer> function = newValue -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        final List<Result<Integer>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(result1(function)).limit(10).collect(Collectors.toList());
        assertEquals("results should have three failures", 3, results.stream().filter(Result::isFailure).count());
        final Integer[] ints = results.stream().flatMap(Result::stream).toArray(Integer[]::new);
        assertArrayEquals("ints should contain no multiples of 3, up to ten 10",
                new Integer[]{1, 2, 4, 5, 7, 8, 10}, ints);
    }

    @Test
    public void testUncheck2() {
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(zipValuesWithKeyFunc(String::valueOf)).map(mapEntry(uncheck2(function))).limit(2).toArray(Integer[]::new);
        assertArrayEquals("ints should contain 1 and 2", new Integer[]{1, 2}, ints);
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheck2AndThrow() {
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        // should throw
        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(zipValuesWithKeyFunc(String::valueOf)).map(mapEntry(uncheck2(function))).limit(3).toArray(Integer[]::new);
    }

    @Test
    public void testResult2() {
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        List<Result<Integer>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(zipValuesWithKeyFunc(String::valueOf)).map(mapEntry(result2(function))).limit(10)
                .collect(Collectors.toList());
        assertEquals("results should have three failures", 3, results.stream().filter(Result::isFailure).count());
        final Integer[] ints = results.stream().flatMap(Result::stream).toArray(Integer[]::new);
        assertArrayEquals("ints should contain no multiples of 3, up to ten 10",
                new Integer[]{1, 2, 4, 5, 7, 8, 10}, ints);
    }

    @Test
    public void testUncheckTest1() {
        final Fun.ThrowingPredicate<Integer> predicate = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return value % 2 == 0;
        };

        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(2)
                .filter(uncheckTest1(predicate)).toArray(Integer[]::new);
        assertArrayEquals("ints should contain only 2", new Integer[]{2}, ints);
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheckTest1AndThrow() {
        final Fun.ThrowingPredicate<Integer> predicate = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return value % 2 == 0;
        };

        // should throw
        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(3)
                .filter(uncheckTest1(predicate)).toArray(Integer[]::new);
    }

    @Test
    public void testUncheckTest2() {
        final Fun.ThrowingBiPredicate<String, Integer> predicate = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue % 2 == 0;
        };

        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(2)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .filter(testEntry(uncheckTest2(predicate)))
                .map(Map.Entry::getValue)
                .toArray(Integer[]::new);
        assertArrayEquals("ints should contain only 2", new Integer[]{2}, ints);
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheckTest2AndThrow() {
        final Fun.ThrowingBiPredicate<String, Integer> predicate = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue % 2 == 0;
        };

        // should throw
        Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(3)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .filter(testEntry(uncheckTest2(predicate)))
                .map(Map.Entry::getValue)
                .toArray(Integer[]::new);
    }

    @Test
    public void testUncheckVoid1() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingConsumer<Integer> consumer = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(value);
        };

        Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(2)
                .forEach(uncheckVoid1(consumer));
        assertEquals("latch value should be 3", 3, latch.get());
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheckVoid1AndThrow() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingConsumer<Integer> consumer = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(value);
        };

        // should throw
        Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(3)
                .forEach(uncheckVoid1(consumer));
    }

    @Test
    public void testResultNothing1() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingConsumer<Integer> consumer = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(value);
        };

        final List<Result<Nothing>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(resultNothing1(consumer)).limit(10).collect(Collectors.toList());
        assertEquals("results should have three failures",
                3, results.stream().filter(Result::isFailure).count());
    }

    @Test
    public void testUncheckVoid2() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingBiConsumer<String, Integer> consumer = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(newValue);
        };

        Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(2)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .forEach(onEntry(uncheckVoid2(consumer)));
        assertEquals("latch value should be 3", 3, latch.get());
    }

    @Test(expected = Fun.FunRuntimeException.class)
    public void testUncheckVoid2AndThrow() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingBiConsumer<String, Integer> consumer = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(newValue);
        };

        // should throw
        Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(3)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .forEach(onEntry(uncheckVoid2(consumer)));
    }

    @Test
    public void testResultNothing2() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingBiConsumer<String, Integer> consumer = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(newValue);
        };

        List<Result<Nothing>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .map(mapEntry(resultNothing2(consumer))).limit(10)
                .collect(Collectors.toList());
        assertEquals("results should have three failures",
                3, results.stream().filter(Result::isFailure).count());
    }

    @Test
    public void testTestOrDefault1() {
        final Fun.ThrowingPredicate<Integer> predicate = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return value % 2 == 0;
        };

        assertFalse("1 should be false for not even and not a multiple of 3",
                testOrDefault1(predicate, true).test(1));
        assertTrue("2 should be true for even and not a multiple of 3",
                testOrDefault1(predicate, true).test(2));
        assertTrue("3 should be true for multiple of 3",
                testOrDefault1(predicate, true).test(3));
    }

    @Test
    public void testTestOrDefault2() {
        final Fun.ThrowingBiPredicate<String, Integer> predicate = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue % 2 == 0;
        };

        assertFalse("1 should be false for not even and not a multiple of 3",
                testOrDefault2(predicate, true).test("", 1));
        assertTrue("2 should be true for even and not a multiple of 3",
                testOrDefault2(predicate, true).test("", 2));
        assertTrue("3 should be true for multiple of 3",
                testOrDefault2(predicate, true).test("", 3));
    }

    @Test
    public void testTryOrDefault0() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        final Integer[] ints = Stream.generate(tryOrDefault0(supplier, 0)).limit(10)
                .toArray(Integer[]::new);
        assertArrayEquals("ints contains three zeros, 1 for each multiple of 3",
                new Integer[]{1, 2, 0, 4, 5, 0, 7, 8, 0, 10}, ints);
    }

    @Test
    public void testTryOrDefault1() {
        final Fun.ThrowingFunction<Integer, Integer> function = newValue -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        final Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(tryOrDefault1(function, 0))
                .toArray(Integer[]::new);
        assertArrayEquals("ints contains three zeros, 1 for each multiple of 3",
                new Integer[]{1, 2, 0, 4, 5, 0, 7, 8, 0, 10}, ints);
    }

    @Test
    public void testTryOrDefault2() {
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };
        final Integer[] ints = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .map(mapEntry(tryOrDefault2(function, 0)))
                .toArray(Integer[]::new);
        assertArrayEquals("ints contains three zeros, 1 for each multiple of 3",
                new Integer[]{1, 2, 0, 4, 5, 0, 7, 8, 0, 10}, ints);
    }

    @Test
    public void testTryOrOptional0() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingSupplier<Integer> supplier = () -> {
            int newValue = latch.incrementAndGet();
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        final List<Optional<Integer>> results = Stream.generate(tryOrOptional0(supplier)).limit(10)
                .collect(Collectors.toList());
        Integer[] ints = results.stream().map(element -> element.orElse(0)).toArray(Integer[]::new);
        assertArrayEquals("ints contains three zeros, 1 for each multiple of 3",
                new Integer[]{1, 2, 0, 4, 5, 0, 7, 8, 0, 10}, ints);
    }

    @Test
    public void testTryOrOptional1() {
        final Fun.ThrowingFunction<Integer, Integer> function = newValue -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };

        final List<Optional<Integer>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(tryOrOptional1(function)).collect(Collectors.toList());
        Integer[] ints = results.stream().map(element -> element.orElse(0)).toArray(Integer[]::new);
        assertArrayEquals("ints contains three zeros, 1 for each multiple of 3",
                new Integer[]{1, 2, 0, 4, 5, 0, 7, 8, 0, 10}, ints);
    }

    @Test
    public void testTryOrOptional2() {
        final Fun.ThrowingBiFunction<String, Integer, Integer> function = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            return newValue;
        };
        final List<Optional<Integer>> results = Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .map(mapEntry(tryOrOptional2(function))).collect(Collectors.toList());
        Integer[] ints = results.stream().map(element -> element.orElse(0)).toArray(Integer[]::new);
        assertArrayEquals("ints contains three zeros, 1 for each multiple of 3",
                new Integer[]{1, 2, 0, 4, 5, 0, 7, 8, 0, 10}, ints);
    }

    @Test
    public void testTryOrVoid1() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingConsumer<Integer> consumer = value -> {
            if (value % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(value);
        };

        Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .forEach(tryOrVoid1(consumer));
        assertEquals("latch value should be", 1 + 2 + 4 + 5 + 7 + 8 + 10, latch.get());
    }

    @Test
    public void testTryOrVoid2() {
        final AtomicInteger latch = new AtomicInteger(0);
        final Fun.ThrowingBiConsumer<String, Integer> consumer = (key, newValue) -> {
            if (newValue % 3 == 0) {
                throw new Exception("multiples of three are disallowed");
            }
            latch.addAndGet(newValue);
        };

        Stream.generate(new AtomicInteger(0)::incrementAndGet).limit(10)
                .map(zipValuesWithKeyFunc(String::valueOf))
                .forEach(onEntry(tryOrVoid2(consumer)));
        assertEquals("latch value should be", 1 + 2 + 4 + 5 + 7 + 8 + 10, latch.get());
    }
}