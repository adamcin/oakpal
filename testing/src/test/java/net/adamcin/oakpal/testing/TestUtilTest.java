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

package net.adamcin.oakpal.testing;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertTrue;

public class TestUtilTest {

    @Test
    public void testTestBlock() throws Exception {
        CompletableFuture<Boolean> latch = new CompletableFuture<>();
        TestUtil.testBlock(() -> {
            latch.complete(true);
        });
        assertTrue("expect completed true", latch.getNow(false));
    }

    @Test(expected = Exception.class)
    public void testTestBlock_throws() throws Exception {
        TestUtil.testBlock(() -> {
            throw new Exception("Expected");
        });
    }
}