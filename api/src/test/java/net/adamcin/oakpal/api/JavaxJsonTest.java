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

import org.apache.jackrabbit.util.ISO8601;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.mapValue;
import static net.adamcin.oakpal.api.Fun.toEntry;
import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.mapArrayOfObjects;
import static net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.api.JavaxJson.mapObjectValues;
import static net.adamcin.oakpal.api.JavaxJson.shallowMergeObjects;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.api.JavaxJson.objectOrEmpty;
import static net.adamcin.oakpal.api.JavaxJson.optArray;
import static net.adamcin.oakpal.api.JavaxJson.optObject;
import static net.adamcin.oakpal.api.JavaxJson.unwrap;
import static net.adamcin.oakpal.api.JavaxJson.val;
import static net.adamcin.oakpal.api.JavaxJson.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JavaxJsonTest {
    static final String KEY_PREFIX = "prefix";
    static final String KEY_URI = "uri";

    private static class JsonNamespace implements JsonObjectConvertible {
        private String prefix;
        private String uri;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public static @Nullable JavaxJsonTest.JsonNamespace fromJson(final @NotNull JsonObject json) {
            if (!json.containsKey(KEY_PREFIX) || !json.containsKey(KEY_URI)) {
                return null;
            }
            JsonNamespace jsonNamespace = new JsonNamespace();
            jsonNamespace.setPrefix(json.getString(KEY_PREFIX, ""));
            jsonNamespace.setUri(json.getString(KEY_URI, ""));
            return jsonNamespace;
        }

        @Override
        public JsonObject toJson() {
            return key(KEY_PREFIX, getPrefix()).key(KEY_URI, getUri()).get();
        }

        public static @NotNull JavaxJsonTest.JsonNamespace create(final @NotNull String prefix, final @NotNull String uri) {
            final JsonNamespace ns = new JsonNamespace();
            ns.setPrefix(prefix);
            ns.setUri(uri);
            return ns;
        }
    }

    @Test
    public void testNonEmptyValue() {
        assertFalse("null is empty", JavaxJson.nonEmptyValue(null));
        assertFalse("JsonObject.NULL is empty",
                JavaxJson.nonEmptyValue(JsonValue.NULL));
        assertFalse("an empty JsonArray is empty",
                JavaxJson.nonEmptyValue(Json.createArrayBuilder().build()));
        assertFalse("an empty JsonObject is empty",
                JavaxJson.nonEmptyValue(Json.createObjectBuilder().build()));
        assertTrue("some string is non-empty", JavaxJson.nonEmptyValue(wrap("foo")));
    }

    @Test
    public void testNonDefaultValue() {
        assertFalse("is not", JavaxJson.nonDefaultValue(JsonValue.NULL, JsonValue.NULL));
        assertTrue("is", JavaxJson.nonDefaultValue(wrap("foo"), wrap("bar")));
        assertFalse("is not", JavaxJson.nonDefaultValue(wrap("bar"), wrap("bar")));
        assertFalse("is not", JavaxJson.nonDefaultValue(wrap(100), wrap(100)));
        assertTrue("is", JavaxJson.nonDefaultValue(wrap(99), wrap(100)));
        assertTrue("is", JavaxJson.nonDefaultValue(wrap(false), wrap(true)));
        assertFalse("is not", JavaxJson.nonDefaultValue(wrap(true), wrap(true)));
    }

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

        assertEquals("key(string).opt(null) should leave empty object",
                Json.createObjectBuilder().build(), obj().key("key").opt(null).get());
        assertEquals("key(string).opt(null, ifNotValue) should return key and JsonObject.NULL when ifNotValue is provided",
                Json.createObjectBuilder().add("key", JsonValue.NULL).build(),
                obj().key("key").opt(null, "value").get());
        assertEquals("key(string).opt(obj()) should leave empty object",
                Json.createObjectBuilder().build(), obj().key("key").opt(obj()).get());
        assertEquals("key(string).opt(arr()) should leave empty object",
                Json.createObjectBuilder().build(), obj().key("key").opt(arr()).get());
        assertEquals("key(string).opt(JsonObject.NULL) should leave empty object",
                Json.createObjectBuilder().build(), obj().key("key").opt(JsonObject.NULL).get());
        assertEquals("key(string).opt(string) should return object with key",
                Json.createObjectBuilder().add("key", "value").build(),
                obj().key("key").opt("value").get());
        assertEquals("key(string).opt(string, default) should return object with key",
                Json.createObjectBuilder().add("key", "value").build(),
                obj().key("key").opt("value", "notValue").get());
        assertEquals("key(string).opt(string, ifNotValue) should return object with key when value is not ifNotValue",
                Json.createObjectBuilder().add("key", "value").build(),
                obj().key("key").opt("value", "notValue").get());
        assertEquals("key(string).opt(string, ifNotValue) should return empty object when value is ifNotValue",
                Json.createObjectBuilder().build(),
                obj().key("key").opt("value", "value").get());

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

        assertEquals("arr().opt(null) should leave empty object",
                Json.createArrayBuilder().build(), arr().opt(null).get());
        assertEquals("arr().opt(null, ifNotValue) should return with JsonObject.NULL when ifNotValue is provided",
                Json.createArrayBuilder().add(JsonValue.NULL).build(),
                arr().opt(null, "value").get());
        assertEquals("arr().opt(obj()) should leave empty object",
                Json.createArrayBuilder().build(),
                arr().opt(obj()).get());
        assertEquals("arr().opt(arr()) should leave empty object",
                Json.createArrayBuilder().build(),
                arr().opt(arr()).get());
        assertEquals("arr().opt(JsonObject.NULL) should leave empty object",
                Json.createArrayBuilder().build(),
                arr().opt(JsonObject.NULL).get());
        assertEquals("arr().opt(string) should return object with key",
                Json.createArrayBuilder().add("value").build(),
                arr().opt("value").get());
        assertEquals("arr().opt(string, default) should return object with key",
                Json.createArrayBuilder().add("value").build(),
                arr().opt("value", "notValue").get());
        assertEquals("arr().opt(string, ifNotValue) should return object with key when value is not ifNotValue",
                Json.createArrayBuilder().add("value").build(),
                arr().opt("value", "notValue").get());
        assertEquals("arr().opt(string, ifNotValue) should return empty object when value is ifNotValue",
                Json.createArrayBuilder().build(),
                arr().opt("value", "value").get());
    }

    @Test
    public void testUnwrap() {
        assertNull("null unwrap to null", unwrap(null));
        assertNull("JsonObject.NULL unwrap to null", unwrap(JsonObject.NULL));
        assertEquals("JsonString unwrap to string", "foo", unwrap(wrap("foo")));
        assertEquals("JsonNumber unwrap to number", 100L, unwrap(wrap(100)));
        List<String> ordinals = Arrays.asList("one", "two", "three");
        assertEquals("unwrap a jsonarray to a List of objects", ordinals,
                unwrap(Json.createArrayBuilder().add("one").add("two").add("three").build()));

        Map<String, Object> someMap = Collections.singletonMap("foo", 5L);
        assertEquals("unwrap a jsonobject to a Map of String to Object", someMap,
                unwrap(Json.createObjectBuilder().add("foo", 5L).build()));
        assertTrue("unwrap JsonObject.TRUE to true",
                Optional.ofNullable(unwrap(JsonObject.TRUE)).map(Boolean.class::cast).orElse(false));
        assertFalse("unwrap JsonObject.FALSE to false",
                Optional.ofNullable(unwrap(JsonObject.FALSE)).map(Boolean.class::cast).orElse(true));
    }

    @Test
    public void testWrap() {
        assertSame("null is JsonObject.NULL", JsonObject.NULL, wrap(null));
        assertSame("same true", JsonObject.TRUE, wrap(true));
        assertSame("same false", JsonObject.FALSE, wrap(false));
        final JsonValue value = Json.createArrayBuilder().add("foo").build().get(0);
        assertSame("same value", value, wrap(value));
        assertEquals("equal string value", value, wrap("foo"));
        final JsonObject object = key("foo", "bar").get();
        final JsonObjectConvertible objectible = () -> object;
        assertSame("same value", object, wrap(objectible));
        final JsonArray array = arr().val("foo1").val("foo2").val("foo3").get();
        final JsonArrayConvertible arrayable = () -> array;
        assertSame("same value", array, wrap(arrayable));
        final Calendar now = Calendar.getInstance();
        assertEquals("same date", wrap(ISO8601.format(now)), wrap(now));

        assertEquals("same int",
                10, ((JsonNumber) wrap(10)).intValue());
        assertEquals("same long",
                Integer.MAX_VALUE + 1L, ((JsonNumber) wrap(Integer.MAX_VALUE + 1L)).longValue());
        assertEquals("same double",
                42.0D, ((JsonNumber) wrap(42.0D)).doubleValue(), 1.0D);

        final BigInteger bigInteger = new BigInteger(Long.toString(Integer.MAX_VALUE + 1L));
        assertEquals("same bigint", bigInteger,
                ((JsonNumber) wrap(bigInteger)).bigIntegerValue());
        final BigDecimal bigDecimal = new BigDecimal(Double.toString(Integer.MAX_VALUE + 1L));
        assertEquals("same bigd", bigDecimal,
                ((JsonNumber) wrap(bigDecimal)).bigDecimalValue());
        final float fval = 4.0f;
        assertEquals("same float", Float.valueOf(fval).doubleValue(),
                ((JsonNumber) wrap(fval)).doubleValue(), 1.0D);

        final String[] foos = {"foo"};
        assertEquals("strings should be wrapped inside of a JsonArray",
                Json.createArrayBuilder().add("foo").build(), wrap(foos));

        assertEquals("ints should be wrapped inside of a JsonArray",
                Json.createArrayBuilder().add(42).build(), wrap(new int[]{42}));
        assertEquals("longs should be wrapped inside of a JsonArray",
                Json.createArrayBuilder().add(42L).build(), wrap(new long[]{42L}));
        assertEquals("doubles should be wrapped inside of a JsonArray",
                Json.createArrayBuilder().add(42.0D).build(), wrap(new double[]{42.0D}));
        assertEquals("chars should be wrapped inside of a String",
                Json.createArrayBuilder().add(String.valueOf(new char[]{'c', 'a', 't'})).build().get(0),
                wrap(new char[]{'c', 'a', 't'}));
        final byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
        final String bytesEncoded = Base64.getUrlEncoder().encodeToString(bytes);
        assertEquals("bytes should be Base64 encoded in a String",
                Json.createArrayBuilder().add(bytesEncoded).build().get(0),
                wrap(bytes));

        assertEquals("floats should be converted to doubles and wrapped inside of a JsonArray",
                Json.createArrayBuilder().add(4.0D).build(), wrap(new float[]{4.0F}));

        assertEquals("booleans should be wrapped inside of a JsonArray",
                Json.createArrayBuilder().add(true).build(), wrap(new boolean[]{true}));

        final String prefix = "foo";
        final String uri = "http://foo.com/1.0";
        final Map<Integer, JsonNamespace> convMap = Collections
                .singletonMap(5, JsonNamespace.create(prefix, uri));
        assertEquals("convert map to json obj",
                key("5", key(KEY_PREFIX, prefix).key(KEY_URI, uri)).get(), wrap(convMap));

        final List<String> ordinals = Arrays.asList("one", "two", "three");
        assertEquals("collection to json array of wrapped elements",
                Json.createArrayBuilder().add("one").add("two").add("three").build(),
                wrap(ordinals));

        final String otherwiseString = "justAString";
        final Object otherwise = new Object() {
            @Override
            public String toString() {
                return otherwiseString;
            }
        };
        assertEquals("otherwise, just toString the object to a json string",
                Json.createArrayBuilder().add(otherwiseString).build().get(0),
                wrap(otherwise));
    }

    @Test
    public void testVal() {
        assertSame("null is NULL", val(null).get(), JsonObject.NULL);
        assertSame("null is NULL is NULL", val(val(null)).get(), JsonObject.NULL);
        assertTrue("Number is JsonNumber", val(1).get() instanceof JsonNumber);
        assertTrue("Number is JsonNumber is JsonNumber", val(val(1)).get() instanceof JsonNumber);
        assertTrue("String is JsonString", val("string").get() instanceof JsonString);
        assertTrue("String is JsonString is JsonString", val(val("string")).get() instanceof JsonString);
        assertSame("Boolean is JsonValue.TRUE or FALSE", val(true).get(), JsonValue.TRUE);
        assertSame("Boolean is JsonValue is JsonValue", val(val(true)).get(), JsonValue.TRUE);
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

        assertEquals("key(string).opt(null) should leave empty object",
                Json.createObjectBuilder().build(), key("key").opt(null).get());
        assertEquals("key(string).opt(null, ifNotValue) should return key and JsonObject.NULL when ifNotValue is provided",
                Json.createObjectBuilder().add("key", JsonValue.NULL).build(),
                key("key").opt(null, "value").get());
        assertEquals("key(string).opt(obj()) should leave empty object",
                Json.createObjectBuilder().build(), key("key").opt(obj()).get());
        assertEquals("key(string).opt(arr()) should leave empty object",
                Json.createObjectBuilder().build(), key("key").opt(arr()).get());
        assertEquals("key(string).opt(JsonObject.NULL) should leave empty object",
                Json.createObjectBuilder().build(), key("key").opt(JsonObject.NULL).get());
        assertEquals("key(string).opt(string) should return object with key",
                Json.createObjectBuilder().add("key", "value").build(),
                key("key").opt("value").get());

        assertEquals("key(string).opt(string, default) should return object with key",
                Json.createObjectBuilder().add("key", "value").build(),
                key("key").opt("value", "notValue").get());
        assertEquals("key(string).opt(string, ifNotValue) should return object with key when value is not ifNotValue",
                Json.createObjectBuilder().add("key", "value").build(),
                key("key").opt("value", "notValue").get());
        assertEquals("key(string).opt(string, ifNotValue) should return empty object when value is ifNotValue",
                Json.createObjectBuilder().build(),
                key("key").opt("value", "value").get());
    }

    @Test
    public void testObjectConvertible() {
        JsonObjectConvertible conv = () -> key("apple", "pie").get();
        assertEquals("ObjectConvertible val is JsonObject",
                "pie",
                ((JsonObject) val(conv).get()).getString("apple"));
    }

    @Test
    public void testArrayConvertible() {
        JsonArrayConvertible conv = () -> arr("one", "two").get();
        assertEquals("ArrayConvertible val is JsonArray",
                "two",
                ((JsonArray) val(conv).get()).getString(1));
    }

    @Test
    public void testKey() {
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

    @Test
    public void testOptObject() {
        assertFalse("empty object returns empty object for any key",
                optObject(JsonValue.EMPTY_JSON_OBJECT, "someKey").isPresent());
        assertFalse("object returns empty object for present key with non-object value",
                optObject(key("someKey", arr()).get(), "someKey").isPresent());
        assertFalse("object returns empty object for present key with null value",
                optObject(key("someKey", null).get(), "someKey").isPresent());
        assertTrue("object returns present object for present key with object value",
                optObject(key("someKey", key("foo", "bar")).get(), "someKey").isPresent());
        assertEquals("object returns correct object value for key with object value",
                "bar",
                optObject(obj()
                        .key("someKey",
                                key("foo", "bar"))
                        .key("otherKey",
                                key("bar", "foo")).get(), "someKey")
                        .map(object -> object.getString("foo")).orElse(""));
    }

    @Test
    public void testObjectOrEmpty() {
        assertSame("empty object returns empty object for any key",
                JsonValue.EMPTY_JSON_OBJECT,
                objectOrEmpty(JsonValue.EMPTY_JSON_OBJECT, "someKey"));
        assertEquals("object returns present object for present key with object value",
                Json.createObjectBuilder().add("foo", "bar").build(),
                objectOrEmpty(key("someKey", key("foo", "bar")).get(), "someKey"));
    }

    @Test
    public void testOptArray() {
        assertFalse("empty object returns empty object for any key",
                optArray(JsonValue.EMPTY_JSON_OBJECT, "someKey").isPresent());
        assertFalse("object returns empty object for present key with non-object value",
                optArray(key("someKey", obj()).get(), "someKey").isPresent());
        assertFalse("object returns empty object for present key with null value",
                optArray(key("someKey", null).get(), "someKey").isPresent());
        assertTrue("object returns present object for present key with object value",
                optArray(key("someKey", arr("foo", "bar")).get(), "someKey").isPresent());
        assertEquals("object returns correct object value for key with object value",
                "bar",
                optArray(obj()
                        .key("someKey",
                                arr("foo", "bar"))
                        .key("otherKey",
                                arr("bar", "foo")).get(), "someKey")
                        .map(array -> array.getString(1)).orElse(""));
    }

    @Test
    public void testArrayOrEmpty() {
        assertSame("empty object returns empty array for any key",
                JsonValue.EMPTY_JSON_ARRAY,
                arrayOrEmpty(JsonValue.EMPTY_JSON_OBJECT, "someKey"));
        assertEquals("object returns present object for present key with object value",
                Json.createArrayBuilder().add("foo").add("bar").build(),
                arrayOrEmpty(key("someKey", arr("foo", "bar")).get(), "someKey"));
    }

    @Test
    public void testMapArrayOfStrings() {
        final JsonArray jsonOrdinals = Json.createArrayBuilder().add("one").add("two").add("three").build();
        final List<String> ordinals = Arrays.asList("one", "two", "three");
        assertEquals("no args returns list of strings for JsonArray containing strings",
                ordinals, mapArrayOfStrings(jsonOrdinals));

        final List<String> upperOrdinals = Arrays.asList("ONE", "TWO", "THREE");
        assertEquals("reverse function applied to strings", upperOrdinals,
                mapArrayOfStrings(jsonOrdinals, String::toUpperCase));
    }

    @Test
    public void testMapArrayOfObjects() {
        final JsonArray jsonInputs = arr()
                .val(key("one", "eno"))
                .val(key("two", "owt"))
                .val(key("three", "eerht")).get();
        final List<Map.Entry<String, String>> expected = Arrays.asList(
                toEntry("one", "eno"),
                toEntry("two", "owt"),
                toEntry("three", "eerht"));
        assertEquals("should create list of entries", expected,
                mapArrayOfObjects(jsonInputs, jsonObject -> jsonObject.entrySet().stream()
                        .map(compose1(mapValue(JsonString.class::cast), mapValue(JsonString::getString)))
                        .findFirst().orElse(null)));

        final JsonArray jsonInputsWithNonObjects = arr()
                .val(key("one", "eno"))
                .val(null)
                .val(key("two", "owt"))
                .val(4)
                .val(key("three", "eerht")).get();
        assertEquals("should create list of entries", expected,
                mapArrayOfObjects(jsonInputsWithNonObjects, jsonObject -> jsonObject.entrySet().stream()
                        .map(compose1(mapValue(JsonString.class::cast), mapValue(JsonString::getString)))
                        .findFirst().orElse(null)));
    }

    @Test
    public void testMapObjectValues() {
        final JsonObject input = obj()
                .key("one", key("num", 1))
                .key("two", key("num", 2))
                .key("three", key("num", 3))
                .get();
        final List<Integer> expected = Arrays.asList(2, 3, 4);
        assertEquals("", expected,
                mapObjectValues(input, (key, value) -> value.getInt("num") + 1,
                        false));
    }

    @Test
    public void testParseFromArray() {
        final JsonArray input = arr().val("foo1:bar1").val("foo2:bar2").val("notvalid").val("foo3:bar3").get();
        final List<Map.Entry<String, String>> expected = Arrays.asList(
                toEntry("foo1", "bar1"),
                toEntry("foo2", "bar2"),
                toEntry("foo3", "bar3")
        );
        final CompletableFuture<Map.Entry<String, Exception>> errorLatch = new CompletableFuture<>();
        final List<Map.Entry<String, String>> parsed = JavaxJson.parseFromArray(input, value -> {
            if (value.contains(":")) {
                final String[] parts = value.split(":", 2);
                return toEntry(parts[0], parts[1]);
            } else {
                throw new IllegalArgumentException("missing colon");
            }
        }, (value, error) -> errorLatch.complete(toEntry(value, error)));
        assertEquals("parse function should work", expected, parsed);
        assertTrue("error latch is done", errorLatch.isDone());
        final Map.Entry<String, Exception> fromLatch = errorLatch.getNow(toEntry("", new Exception("")));
        assertEquals("error latch key is", "notvalid",
                fromLatch.getKey());
        assertEquals("error latch error message is", "missing colon",
                fromLatch.getValue().getMessage());
    }

    @Test
    public void testHasNonNull() {
        assertFalse("not hasNonNull on empty object for any key",
                hasNonNull(JsonValue.EMPTY_JSON_OBJECT, "any"));
        assertFalse("not hasNonNull for key with json null",
                hasNonNull(key("foo", null).get(), "foo"));
        assertTrue("hasNonNull for key with non-null json value",
                hasNonNull(key("foo", "bar").get(), "foo"));
    }

    @Test
    public void testShallowMergeObjects() {
        assertEquals("expect empty object from nulls", JsonObject.EMPTY_JSON_OBJECT,
                JavaxJson.shallowMergeObjects(null, null));

        assertEquals("expect empty object from null and empty", JsonObject.EMPTY_JSON_OBJECT,
                JavaxJson.shallowMergeObjects(null, JsonValue.EMPTY_JSON_OBJECT));

        assertEquals("expect empty object from empty and null", JsonObject.EMPTY_JSON_OBJECT,
                JavaxJson.shallowMergeObjects(JsonValue.EMPTY_JSON_OBJECT, null));

        final JsonObject base = obj()
                .key("baseKey", "unmodified")
                .key("keyString", "base")
                .key("keyObject", key("baseKey", "unmodified").key("keyString", "base"))
                .get();

        assertEquals("expect equal base for null overlay", base, shallowMergeObjects(base, null));
        assertEquals("expect equal base for empty overlay", base, shallowMergeObjects(base,
                JsonValue.EMPTY_JSON_OBJECT));
        assertEquals("expect equal base as overlay with null base", base, shallowMergeObjects(null, base));
        assertEquals("expect equal base as overlay with empty base", base,
                shallowMergeObjects(JsonValue.EMPTY_JSON_OBJECT, base));

        final JsonObject overlay = obj()
                .key("overlayKey", "unmodified")
                .key("keyString", "overlay")
                .key("keyObject", key("overlayKey", "unmodified").key("keyString", "overlay"))
                .get();

        assertEquals("expect merged base <- overlay", obj()
                        .key("baseKey", "unmodified")
                        .key("overlayKey", "unmodified")
                        .key("keyString", "overlay")
                        .key("keyObject", key("overlayKey", "unmodified").key("keyString", "overlay"))
                        .get(),
                shallowMergeObjects(base, overlay));

        assertEquals("expect merged overlay <- base", obj()
                        .key("baseKey", "unmodified")
                        .key("overlayKey", "unmodified")
                        .key("keyString", "base")
                        .key("keyObject", key("baseKey", "unmodified").key("keyString", "base"))
                        .get(),
                shallowMergeObjects(overlay, base));
    }
}
