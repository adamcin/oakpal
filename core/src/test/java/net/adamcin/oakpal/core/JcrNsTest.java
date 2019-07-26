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

import static net.adamcin.oakpal.core.JavaxJson.key;
import static org.junit.Assert.*;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;

public class JcrNsTest {
    final String prefix = "foo";
    final String uri = "http://foo.com";

    @Test
    public void testFromJson() {
        assertNull("should be null if either prefix and uri are missing",
                JcrNs.fromJson(JsonValue.EMPTY_JSON_OBJECT));
        assertNull("should be null prefix is missing",
                JcrNs.fromJson(key(JcrNs.KEY_PREFIX, prefix).get()));
        assertNull("should be null if uri is missing",
                JcrNs.fromJson(key(JcrNs.KEY_URI, uri).get()));
        JcrNs complete = JcrNs.fromJson(key(JcrNs.KEY_PREFIX, prefix).key(JcrNs.KEY_URI, uri).get());
        assertNotNull("complete should not be null", complete);
        assertEquals("complete prefix should be", prefix, complete.getPrefix());
        assertEquals("complete prefix should be", uri, complete.getUri());
    }

    @Test
    public void testCreate() {

        JcrNs complete = JcrNs.create(prefix, uri);
        assertEquals("complete prefix should be", prefix, complete.getPrefix());
        assertEquals("complete prefix should be", uri, complete.getUri());
    }

    @Test
    public void testToJson() {
        JsonObject expect = key(JcrNs.KEY_PREFIX, prefix).key(JcrNs.KEY_URI, uri).get();
        JcrNs complete = JcrNs.create(prefix, uri);
        assertEquals("expect json", expect, complete.toJson());
    }

    @Test
    public void testToString() {
        String expect = key(JcrNs.KEY_PREFIX, prefix).key(JcrNs.KEY_URI, uri).get().toString();
        JcrNs complete = JcrNs.create(prefix, uri);
        assertEquals("expect json string", expect, complete.toString());
    }

    @Test
    public void testCompareTo() {
        JcrNs foo = JcrNs.create(prefix, uri);
        JcrNs bar = JcrNs.create("bar", "http://bar.com");
        assertEquals("compare ns based on prefix", "foo".compareTo("bar"), foo.compareTo(bar));
    }
}