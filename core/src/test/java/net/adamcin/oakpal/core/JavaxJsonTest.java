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

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.core.JavaxJson.val;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.junit.Test;

public class JavaxJsonTest {

    @Test
    public void testObj() {
        JsonObject object = obj().get();

        assertNotNull("obj().get() results in non-null JsonObject", object);
        assertTrue("obj().get() results in empty JsonObject", object.keySet().isEmpty());

        JsonObject nullJSON = obj((JsonObject) null).get();
        assertNotNull("obj((JsonObject) null).get() results in non-null JsonObject", nullJSON);
        assertTrue("obj((JsonObject) null).get() results in empty JsonObject", nullJSON.keySet().isEmpty());

        JsonObject reJSON = obj(key("color", "red").get()).get();
        assertEquals("JsonObject to obj to JsonObject", "red", reJSON.getString("color"));

        Map<String, Object> reMap = new HashMap<>();
        reMap.put("color", "blue");
        JsonObject reMapJSON = obj(reMap).get();
        assertEquals("Map to obj to JsonObject", "blue", reMapJSON.getString("color"));

        assertEquals("obj deep equals obj(key, key) : obj().key(key, val).key(key2, val2)",
                obj(key("foo", "bar"), key("foo2", "bar2")).get(),
                obj().key("foo", "bar").key("foo2", "bar2").get());

        assertEquals("obj deep equals obj(key, key) : obj(key(key, val)).and(key(key2, val2))",
                obj(key("foo", "bar"), key("foo2", "bar2")).get(),
                obj(key("foo", "bar")).and(key("foo2", "bar2")).get());

        assertEquals("obj.arrKeys() retains order and duplicates",
                "[\"one\",\"two\",\"three\",\"two\"]",
                key("one", 1).key("two", 2).key("three", 3).key("two", -2).arrKeys().get().toString());

        assertEquals("obj.arrVals() retains order and duplicates",
                "[1,2,3,-2]",
                key("one", 1).key("two", 2).key("three", 3).key("two", -2).arrVals().get().toString());
    }

    @Test
    public void testArr() {

        JsonArray array = arr().get();
        assertNotNull("arr().get() results in non-null JsonArray", array);
        assertTrue("arr().get() results in empty JsonArray", !array.iterator().hasNext());

        JsonArray nullArray = arr((Object[]) null).get();
        assertNotNull("arr(null).get() results in non-null JsonArray", nullArray);
        assertTrue("arr(null).get() results in empty JsonArray", !nullArray.iterator().hasNext());

        assertEquals("easy numerical sequence",
                "[0,1,2,3,4]", arr(0, 1, 2, 3, 4).get().toString());

        assertEquals("easy numerical sequence as vals",
                "[0,1,2,3,4]",
                arr(val(0), val(1), val(2), val(3), val(4)).get().toString());

        assertEquals("easy numerical sequence as dotted vals",
                "[0,1,2,3,4]",
                arr().val(0).val(1).val(2).val(3).val(4).get().toString());

        assertEquals("easy numerical sequence as dotted vals, no arr() initiator",
                "[0,1,2,3,4]",
                val(0).val(1).val(2).val(3).val(4).get().toString());

        assertEquals("numerical sequence as arr().and()",
                "[0,1,2,3,4]", arr(0, 1).and(2, 3, 4).get().toString());
    }

    @Test
    public void testVal() {
        assertSame("null is NULL", val(null).get(), JsonObject.NULL);
        assertSame("null is NULL is NULL", val(val(null)).get(), JsonObject.NULL);
        assertTrue("Number is JsonNumber", val(1).get() instanceof JsonNumber);
        assertTrue("Number is JsonNumber is JsonNumber", val(val(1)).get() instanceof JsonNumber);
        assertTrue("String is JsonString", val("string").get() instanceof JsonString);
        assertTrue("String is JsonString is JsonString", val(val("string")).get() instanceof JsonString);
        assertTrue("Boolean is JsonValue.TRUE or FALSE", val(true).get() == JsonValue.TRUE);
        assertTrue("Boolean is JsonValue is JsonValue", val(val(true)).get() == JsonValue.TRUE);
        assertTrue("obj is JsonObject", val(obj()).get() instanceof JsonObject);
        assertTrue("obj is obj is JsonObject", val(val(obj())).get() instanceof JsonObject);
        assertTrue("JsonObject is JsonObject", val(obj().get()).get() instanceof JsonObject);
        assertTrue("JsonObject is JsonObject is JsonObject", val(val(obj().get())).get() instanceof JsonObject);
        assertTrue("arr is JsonArray", val(arr()).get() instanceof JsonArray);
        assertTrue("arr is arr is JsonArray", val(val(arr())).get() instanceof JsonArray);
        assertTrue("JsonArray is JsonArray", val(arr().get()).get() instanceof JsonArray);
        assertTrue("JsonArray is JsonArray is JsonArray", val(val(arr().get())).get() instanceof JsonArray);

    }

    @Test
    public void testCursorVal() {
        boolean keyCursorNull = false;
        try {
            key(null);
        } catch (final NullPointerException e) {
            keyCursorNull = true;
        }
        assertTrue("key(null) should throw NullPointerException", keyCursorNull);

        boolean keyCursorErred = false;
        try {
            val(key("noVal"));
        } catch (final IllegalArgumentException e) {
            keyCursorErred = true;
        }
        assertTrue("val(key()) should throw IllegalArgumentException", keyCursorErred);

        boolean objCursorNull = false;
        try {
            obj().key(null);
        } catch (final NullPointerException e) {
            objCursorNull = true;
        }
        assertTrue("obj().key(null) should throw NullPointerException", objCursorNull);

        boolean objCursorErred = false;
        try {
            val(obj().key("noVal"));
        } catch (final IllegalArgumentException e) {
            objCursorErred = true;
        }
        assertTrue("val(obj().key()) should throw IllegalArgumentException", objCursorErred);

        JavaxJson.Obj precursor = obj().key("foo", "bar");
        assertEquals("obj().key(key).obj() should return precursor obj.",
                precursor, precursor.key("cursor").getObj());
    }

    @Test
    public void testObjectConvertible() {
        JavaxJson.ObjectConvertible conv = () -> key("apple", "pie").get();
        assertEquals("ObjectConvertible val is JsonObject",
                "pie",
                ((JsonObject) val(conv).get()).getString("apple"));
    }

    @Test
    public void testArrayConvertible() {
        JavaxJson.ArrayConvertible conv = () -> arr("one", "two").get();
        assertEquals("ArrayConvertible val is JsonArray",
                "two",
                ((JsonArray) val(conv).get()).getString(1));
    }

    @Test
    public void testKey() throws Exception {
        boolean keyNull = false;
        try {
            key(null, "val");
        } catch (final NullPointerException e) {
            keyNull = true;
        }
        assertTrue("key(null, val) should throw NullPointerException", keyNull);

        assertEquals("key cursor getKey() returns input",
                "foo", key("foo").getKey());
        assertEquals("key(key, val) is retrievable",
                "bar", key("foo", "bar").get().getString("foo"));
        assertEquals("key(key, val).key(key2, val2) is retrievable",
                "bar2", key("foo", "bar").key("foo2", "bar2").get().getString("foo2"));
        assertEquals("key(key).val(val) is retrievable",
                "bar", key("foo").val("bar").get().getString("foo"));
        assertEquals("key(key).val(val).key(key2).val(val2) is retrievable",
                "bar2", key("foo").val("bar").key("foo2").val("bar2").get().getString("foo2"));

        assertTrue("Key.toValue should return a JsonObject.",
                key("foo", "bar").toValue().get() instanceof JsonObject);
        assertEquals("Key.toValue obj should have appropriate key.", "bar",
                ((JsonObject) key("foo", "bar").toValue().get()).getString("foo"));
    }
}
