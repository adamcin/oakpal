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

import java.io.File;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.webster.targets.JsonTargetFactory.CHECKLIST;
import static net.adamcin.oakpal.webster.targets.JsonTargetFactory.HINT_KEY_MORE_TARGETS;
import static net.adamcin.oakpal.webster.targets.JsonTargetFactory.KEY_TYPE;
import static net.adamcin.oakpal.webster.targets.JsonTargetFactory.NODETYPES;
import static net.adamcin.oakpal.webster.targets.JsonTargetFactory.PRIVILEGES;
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
                        .map(compose1(JsonTargetFactory::name, String::toUpperCase))
                        .allMatch(JsonTargetFactory::isTargetType));

        assertTrue("type name lowercase is target type",
                Stream.of(JsonTargetFactory.values())
                        .map(compose1(JsonTargetFactory::name, String::toLowerCase))
                        .allMatch(JsonTargetFactory::isTargetType));

        assertFalse("not a type name is not a target type", JsonTargetFactory.isTargetType(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromJson_unknownHint_throws() throws Exception {
        JsonTargetFactory.fromJson(new File("."), obj().get(), "unknown");
    }

    @Test
    public void testFromJson_validHints() throws Exception {
        final File baseDir = new File(".");
        assertTrue("is checklist target",
                JsonTargetFactory.fromJson(baseDir, obj().get(),
                        CHECKLIST.name()) instanceof WebsterChecklistTarget);
        assertTrue("is nodetypes target",
                JsonTargetFactory.fromJson(baseDir, obj().get(),
                        NODETYPES.name()) instanceof WebsterNodetypesTarget);
        assertTrue("is privileges target",
                JsonTargetFactory.fromJson(baseDir, obj().get(),
                        PRIVILEGES.name()) instanceof WebsterPrivilegesTarget);
    }

    @Test
    public void testFromJson_withTypeProperty() throws Exception {
        final File baseDir = new File(".");
        assertTrue("is checklist target",
                JsonTargetFactory.fromJson(baseDir,
                        key(KEY_TYPE, CHECKLIST.name())
                                .get(), "unknown") instanceof WebsterChecklistTarget);
        assertTrue("is nodetypes target",
                JsonTargetFactory.fromJson(baseDir,
                        key(KEY_TYPE, NODETYPES.name())
                                .get(), "unknown") instanceof WebsterNodetypesTarget);
        assertTrue("is privileges target",
                JsonTargetFactory.fromJson(baseDir,
                        key(KEY_TYPE, PRIVILEGES.name())
                                .get(), "unknown") instanceof WebsterPrivilegesTarget);
    }

    @Test
    public void testFromJsonHintMap() throws Exception {
        final File baseDir = new File(".");
        assertTrue("is unknown target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key("unknown", obj()).get()).isEmpty());
        assertTrue("is checklist target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(CHECKLIST.name(), obj()).get())
                        .get(0) instanceof WebsterChecklistTarget);
        assertTrue("is nodetypes target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(NODETYPES.name(), obj()).get())
                        .get(0) instanceof WebsterNodetypesTarget);
        assertTrue("is privileges target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(PRIVILEGES.name(), obj()).get())
                        .get(0) instanceof WebsterPrivilegesTarget);

        assertTrue("is checklist target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(CHECKLIST.name(), null).get())
                        .get(0) instanceof WebsterChecklistTarget);
        assertTrue("is nodetypes target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(NODETYPES.name(), null).get())
                        .get(0) instanceof WebsterNodetypesTarget);
        assertTrue("is privileges target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(PRIVILEGES.name(), null).get())
                        .get(0) instanceof WebsterPrivilegesTarget);

        assertTrue("is checklist target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(HINT_KEY_MORE_TARGETS, arr(key(KEY_TYPE, CHECKLIST.name()))).get())
                        .get(0) instanceof WebsterChecklistTarget);
        assertTrue("is nodetypes target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(HINT_KEY_MORE_TARGETS, arr(key(KEY_TYPE, NODETYPES.name()))).get())
                        .get(0) instanceof WebsterNodetypesTarget);
        assertTrue("is privileges target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(HINT_KEY_MORE_TARGETS, arr(key(KEY_TYPE, PRIVILEGES.name()))).get())
                        .get(0) instanceof WebsterPrivilegesTarget);

        assertTrue("is checklist target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(HINT_KEY_MORE_TARGETS, key(CHECKLIST.name(), obj())).get())
                        .get(0) instanceof WebsterChecklistTarget);
        assertTrue("is nodetypes target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(HINT_KEY_MORE_TARGETS, key(NODETYPES.name(), obj())).get())
                        .get(0) instanceof WebsterNodetypesTarget);
        assertTrue("is privileges target",
                JsonTargetFactory.fromJsonHintMap(baseDir, key(HINT_KEY_MORE_TARGETS, key(PRIVILEGES.name(), obj())).get())
                        .get(0) instanceof WebsterPrivilegesTarget);

    }
}