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

package net.adamcin.oakpal.webster.targets;

import org.junit.Test;

import java.util.stream.Stream;

import static net.adamcin.oakpal.core.Fun.compose;
import static org.junit.Assert.*;

public class JsonTargetFactoryTest {

    @Test
    public void testByType() {
        Stream.of(JsonTargetFactory.values()).forEachOrdered(type -> {
            assertSame("type name uppercase gets type", type,
                    JsonTargetFactory.byType(type.name().toUpperCase()));
        });

        Stream.of(JsonTargetFactory.values()).forEachOrdered(type -> {
            assertSame("type name lowercase gets type", type,
                    JsonTargetFactory.byType(type.name().toLowerCase()));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByType_throws() {
        JsonTargetFactory.byType("");
    }

    @Test
    public void testIsTargetType() {
        assertTrue("type name uppercase is target type",
                Stream.of(JsonTargetFactory.values())
                        .map(compose(JsonTargetFactory::name, String::toUpperCase))
                        .allMatch(JsonTargetFactory::isTargetType));

        assertTrue("type name lowercase is target type",
                Stream.of(JsonTargetFactory.values())
                        .map(compose(JsonTargetFactory::name, String::toLowerCase))
                        .allMatch(JsonTargetFactory::isTargetType));

        assertFalse("not a type name is not a target type", JsonTargetFactory.isTargetType(""));
    }

    @Test
    public void testFromJson() {

    }
}