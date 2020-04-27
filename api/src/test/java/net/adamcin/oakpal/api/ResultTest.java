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

import static net.adamcin.oakpal.api.Fun.result1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class ResultTest {
    private static final String specialLoggerInfo = "oakpal_special.info";
    private static final String specialLoggerError = "oakpal_special.error";

    @Test
    public void testIsSuccess() {
        assertTrue("success is success", Result.success("success").isSuccess());
        assertFalse("failure is not success", Result.failure("failure").isSuccess());
    }

    @Test
    public void testIsFailure() {
        assertFalse("success is not failure", Result.success("success").isFailure());
        assertTrue("failure is failure", Result.failure("failure").isFailure());
    }

    @Test
    public void testToOptional() {
        assertTrue("success is present", Result.success("success").toOptional().isPresent());
        assertFalse("failure is not present", Result.failure("failure").toOptional().isPresent());
    }

    @Test
    public void testFindCause_predicate() {
        assertFalse("success has no cause", Result.success("success").findCause(cause -> true).isPresent());
        assertTrue("failure has cause", Result.failure("failure").findCause(cause -> true).isPresent());
    }

    private static class SignatureCause extends RuntimeException {

    }

    @Test
    public void testFindCause_class() {
        assertFalse("success has no cause",
                Result.success("success").findCause(SignatureCause.class).isPresent());
        assertFalse("failure has different cause",
                Result.failure("failure").findCause(SignatureCause.class).isPresent());
        assertTrue("failure has cause",
                Result.failure(new SignatureCause()).findCause(SignatureCause.class).isPresent());
    }

    @Test
    public void testThrowCause() {
        Result.success("success").throwCause(SignatureCause.class);
        Result.failure("failure").throwCause(SignatureCause.class);
    }

    @Test(expected = SignatureCause.class)
    public void testThrowCause_throws() {
        Result.failure(new SignatureCause()).throwCause(SignatureCause.class);
    }

    @Test
    public void testFailure_constructMessageError() {
        final Result<String> result = Result.failure("failure", new SignatureCause());
        assertEquals("get message", "Failure(failure)", result.toString());
    }

    @Test
    public void testGetOrDefault() {
        final String defaultValue = "defaultValue";
        assertNotEquals("success gets non-default",
                defaultValue, Result.success("success").getOrDefault(defaultValue));
        assertEquals("failure gets default",
                defaultValue, Result.failure("failure").getOrDefault(defaultValue));
    }

    @Test
    public void testGetOrElse() {
        final String defaultValue = "defaultValue";
        assertNotEquals("success gets non-default",
                defaultValue, Result.success("success").getOrElse(() -> defaultValue));
        assertEquals("failure gets default",
                defaultValue, Result.failure("failure").getOrElse(() -> defaultValue));
    }

    @Test
    public void testOrElse() {
        final Result<String> defaultValue = Result.success("defaultValue");
        assertNotSame("success gets non-default",
                defaultValue, Result.success("success").orElse(() -> defaultValue));
        assertSame("failure gets default",
                defaultValue, Result.<String>failure("failure").orElse(() -> defaultValue));
    }

    @Test
    public void testTeeLogError() {
        final Result<String> success = Result.success("success");
        final Result<String> failure = Result.failure("failure");
        assertSame("success tees itself", success, success.teeLogError());
        assertSame("failure tees itself", failure, failure.teeLogError());
    }

    @Test
    public void testForEach() {
        final CompletableFuture<String> slotSuccess = new CompletableFuture<>();
        Result.success("success").forEach(slotSuccess::complete);
        assertEquals("success does do", "success", slotSuccess.getNow(null));

        final CompletableFuture<String> slotFailure = new CompletableFuture<>();
        Result.<String>failure("failure").forEach(slotFailure::complete);
        assertFalse("failure doesn't do", slotFailure.isDone());
    }

    @Test
    public void testMap() {
        final Result<String> success = Result.success("success");
        final Result<String> successMapped = success.map(String::toUpperCase);
        assertEquals("success maps to SUCCESS",
                "SUCCESS", successMapped.getOrDefault("success"));

        final Result<String> failure = Result.failure("failure");
        final Result<String> failureMapped = failure.map(String::toUpperCase);
        assertSame("failures have same exception",
                failure.getError().get(), failureMapped.getError().get());
    }

    @Test
    public void testToString() {
        final Result<String> success = Result.success("success");
        final Result<String> failure = Result.failure("failure");
        assertEquals("success tees itself", "Success(success)", success.toString());
        assertEquals("failure tees itself", "Failure(failure)", failure.toString());
    }

    @Test
    public void testBuilder() {
        final Result<Set<String>> setResult = Arrays.asList(
                Result.success("a"),
                Result.success("b"),
                Result.success("c"))
                .stream().collect(Result.tryCollect(Collectors.toSet()));

        assertTrue("collect success", setResult.isSuccess());
    }

    @Test
    public void testCollectOrFailOnFirst() {
        List<Result<Integer>> rawValues = Stream.of(0, 1, 2, 3, 4, 5, 6)
                .map(result1(value -> value / (value % 3)))
                .collect(Collectors.toList());

        Result<List<Integer>> allAreGood = rawValues.stream().collect(Result.tryCollect(Collectors.toList()));

        assertTrue("allAreGood is a failure", allAreGood.isFailure());
        assertTrue("failure is an " + allAreGood.getError().get().getClass().getName(),
                allAreGood.findCause(ArithmeticException.class::isInstance).isPresent());
    }

    @Test
    public void testLogAndRestream() {
        final Set<Result<String>> original = Stream.of("a", "b", "c")
                .map(Result::success).collect(Collectors.toSet());
        assertEquals("original same as collected", original,
                original.stream().collect(Result.logAndRestream()).collect(Collectors.toSet()));
        assertEquals("original same as collected with message", original,
                original.stream().collect(Result.logAndRestream("collect message")).collect(Collectors.toSet()));
        assertEquals("original same as collected", original, original.stream()
                .collect(new Result.RestreamLogCollector<>(LoggerFactory.getLogger(specialLoggerError), "withError"))
                .collect(Collectors.toSet()));

    }

    @Test
    public void testTryCollect_combiner() {
        final Set<Result<String>> original = Stream.of("a", "b", "c", "d", "e")
                .map(Result::success).collect(Collectors.toSet());
        Result<List<String>> collected = StreamSupport.stream(original.spliterator(), true)
                .collect(Result.tryCollect(Collectors.toList()));
    }

    @Test
    public void testLogAndRestream_combiner() {
        final Set<Result<String>> original = Stream.of("a", "b", "c", "d", "e")
                .map(Result::success).collect(Collectors.toSet());
        assertEquals("original same as collected", original,
                StreamSupport.stream(original.spliterator(), true).collect(Result.logAndRestream()).collect(Collectors.toSet()));
        assertEquals("original same as collected with message", original,
                StreamSupport.stream(original.spliterator(), true).collect(Result.logAndRestream("collect message")).collect(Collectors.toSet()));
        assertEquals("original same as collected", original, StreamSupport.stream(original.spliterator(), true)
                .collect(new Result.RestreamLogCollector<>(LoggerFactory.getLogger(specialLoggerError), "withError"))
                .collect(Collectors.toSet()));
    }
}