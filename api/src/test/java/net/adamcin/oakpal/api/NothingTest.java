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

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

public class NothingTest {
    @Test
    public void testVoidToNothing1() throws Exception {
        final String sentinel = "sentinel";
        final CompletableFuture<String> latch = new CompletableFuture<>();
        final Consumer<String> consumer = latch::complete;
        final Nothing nothing = Nothing.voidToNothing1(consumer).apply(sentinel);
        assertTrue("future should be done: " + latch, latch.isDone());
        assertSame("nothing should be returned", Nothing.instance, nothing);
        assertSame("latched should be same as input", sentinel, latch.get());
    }

    @Test
    public void testVoidToNothing2() throws Exception {
        final String sentinel = "sentinel";
        final CompletableFuture<String> latch = new CompletableFuture<>();
        final BiConsumer<String, Boolean> consumer = (string, test) -> latch.complete(string);
        final Nothing nothing = Nothing.voidToNothing2(consumer).apply(sentinel, true);
        assertTrue("future should be done: " + latch, latch.isDone());
        assertSame("nothing should be returned", Nothing.instance, nothing);
        assertSame("latched should be same as input", sentinel, latch.get());
    }

    @Test
    public void testCombine() {
        assertSame("combine Nothing to produce Nothing",
                Nothing.instance, Nothing.instance.combine(Nothing.instance));
    }
}