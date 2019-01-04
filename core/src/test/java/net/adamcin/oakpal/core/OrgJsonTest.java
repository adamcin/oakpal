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

import static net.adamcin.oakpal.core.OrgJson.arr;
import static net.adamcin.oakpal.core.OrgJson.key;
import static net.adamcin.oakpal.core.OrgJson.obj;
import static net.adamcin.oakpal.core.OrgJson.val;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class OrgJsonTest {

    @Test
    public void testObj() {
        JSONObject object = obj().get();

        assertNotNull("obj().get() results in non-null JSONObject", object);
        assertTrue("obj().get() results in empty JSONObject", object.keySet().isEmpty());

        assertEquals("obj deep equals obj(key, key) : obj().key(key, val).key(key2, val2)",
                obj(key("foo", "bar"), key("foo2", "bar2")).get().toMap(),
                obj().key("foo", "bar").key("foo2", "bar2").get().toMap());

        assertEquals("obj deep equals obj(key, key) : obj(key(key, val)).and(key(key2, val2))",
                obj(key("foo", "bar"), key("foo2", "bar2")).get().toMap(),
                obj(key("foo", "bar")).and(key("foo2", "bar2")).get().toMap());
    }

    @Test
    public void testArr() {
        JSONArray array = arr().get();

        assertNotNull("arr().get() results in non-null JSONArray", array);
        assertTrue("arr().get() results in empty JSONArray", !array.iterator().hasNext());

        assertEquals("easy numerical sequence",
                "[0,1,2,3,4]", arr(0, 1, 2, 3, 4).get().toString());

        assertEquals("easy numerical sequence as vals",
                "[0,1,2,3,4]",
                arr(val(0), val(1), val(2), val(3), val(4)).get().toString());

        assertEquals("numerical sequence as arr().and()",
                "[0,1,2,3,4]", arr(0, 1).and(2, 3, 4).get().toString());
    }

    @Test
    public void testVal() {
        assertSame("null is NULL", val(null).get(), JSONObject.NULL);
        assertSame("null is NULL is NULL", val(val(null)).get(), JSONObject.NULL);
        assertTrue("Number is Number", val(1).get() instanceof Number);
        assertTrue("Number is Number is Number", val(val(1)).get() instanceof Number);
        assertTrue("String is String", val("string").get() instanceof String);
        assertTrue("String is String is String", val(val("string")).get() instanceof String);
        assertTrue("Boolean is Boolean", val(true).get() instanceof Boolean);
        assertTrue("Boolean is Boolean is Boolean", val(val(true)).get() instanceof Boolean);
        assertTrue("obj is JSONObject", val(obj()).get() instanceof JSONObject);
        assertTrue("obj is obj is JSONObject", val(val(obj())).get() instanceof JSONObject);
        assertTrue("JSONObject is JSONObject", val(obj().get()).get() instanceof JSONObject);
        assertTrue("JSONObject is JSONObject is JSONObject", val(val(obj().get())).get() instanceof JSONObject);
        assertTrue("arr is JSONArray", val(arr()).get() instanceof JSONArray);
        assertTrue("arr is arr is JSONArray", val(val(arr())).get() instanceof JSONArray);
        assertTrue("JSONArray is JSONArray", val(arr().get()).get() instanceof JSONArray);
        assertTrue("JSONArray is JSONArray is JSONArray", val(val(arr().get())).get() instanceof JSONArray);

        boolean keyCursorErred = false;
        try {
            val(key("noVal"));
        } catch (final IllegalArgumentException e) {
            keyCursorErred = true;
        }
        assertTrue("val(key()) should throw IllegalArgumentException", keyCursorErred);

        boolean objCursorErred = false;
        try {
            val(obj().key("noVal"));
        } catch (final IllegalArgumentException e) {
            objCursorErred = true;
        }
        assertTrue("val(obj().key()) should throw IllegalArgumentException", objCursorErred);
    }


    @Test
    public void testKey() throws JSONException {
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

    }
}
