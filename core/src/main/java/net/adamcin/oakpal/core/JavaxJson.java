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

import static net.adamcin.oakpal.core.Util.composeTry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonCollectors;

/**
 * Simple DSL for constructing javax.json objects for {@link ProgressCheckFactory} configs using only three-letter identifiers.
 * <p>
 * Recommend using {@code import static net.adamcin.oakpal.core.JavaxJson.*;}
 */
public final class JavaxJson {
    private JavaxJson() {
        /* no instantiation */
    }

    /**
     * Custom pojo types which should be usable within this DSL should implement this method to provide a
     * {@link JsonObject}, which can be wrapped quickly by {@link #val(Object)}.
     */
    public interface ObjectConvertible {
        JsonObject toJson();
    }

    /**
     * Custom pojo types which should be usable within this DSL should implement this method to provide a
     * {@link JsonArray}, which can be wrapped quickly by {@link #val(Object)}.
     */
    public interface ArrayConvertible {
        JsonArray toJson();
    }

    /**
     * Defines a method toValue which coalesces the underlying value to prevent over-wrapping by the
     * {@link #val(Object)} method.
     */
    public interface HasValue {
        Value toValue();
    }

    /**
     * Defines a get method returns the constructed type parameter.
     *
     * @param <TYPE>
     */
    public interface As<TYPE> {
        TYPE get();
    }

    /**
     * Type which allows a different fluent style for building keys, i.e. {@code key(String).val(obj)}.
     */
    public interface Cursor {
        String getKey();
    }

    /**
     * Ensures that a Java typed object is wrapped with the appropriate {@link JsonValue} type.
     * Performs the reverse of {@link #unwrap(JsonValue)}.
     *
     * @param object the Java type to wrap
     * @return a JsonValue-wrapped object
     */
    public static JsonValue wrap(final Object object) {
        if (object == null) {
            return JsonValue.NULL;
        } else if (object instanceof JsonValue) {
            return (JsonValue) object;
        } else if (object instanceof String) {
            return Json.createArrayBuilder().add((String) object).build().get(0);
        } else if (object instanceof Boolean) {
            return ((Boolean) object) ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (object instanceof Number) {
            if (object instanceof Integer) {
                return Json.createArrayBuilder().add((Integer) object).build().get(0);
            } else if (object instanceof Long) {
                return Json.createArrayBuilder().add((Long) object).build().get(0);
            } else if (object instanceof Double) {
                return Json.createArrayBuilder().add((Double) object).build().get(0);
            } else if (object instanceof BigInteger) {
                return Json.createArrayBuilder().add((BigInteger) object).build().get(0);
            } else if (object instanceof BigDecimal) {
                return Json.createArrayBuilder().add((BigDecimal) object).build().get(0);
            } else {
                return Json.createArrayBuilder().add(new BigDecimal(object.toString())).build().get(0);
            }
        } else if (object instanceof Map) {
            return ((Map<?, ?>) object).entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            entry -> wrap(entry.getValue()))).entrySet().stream()
                    .collect(JsonCollectors.toJsonObject());
        } else if (object instanceof Collection) {
            return ((Collection<?>) object).stream().map(JavaxJson::wrap)
                    .collect(JsonCollectors.toJsonArray());
        }
        return Json.createArrayBuilder().add(String.valueOf(object)).build().get(0);
    }

    /**
     * This method performs the reverse of {@link #wrap(Object)}.
     * Unwraps a JsonValue to its associated non-Json value.
     * {@link JsonString} -> {@link String},
     * {@link JsonNumber} -> {@link Number},
     * {@link JsonValue#NULL} -> {@code null},
     * {@link JsonValue#TRUE} -> {@code true},
     * {@link JsonValue#FALSE} -> {@code false},
     * {@link JsonArray} -> {@code List<Object>},
     * {@link JsonObject}-> {@code Map<String, Object>}.
     *
     * @param jsonValue the wrapped JsonValue
     * @return the associated Java value
     */
    public static Object unwrap(final JsonValue jsonValue) {
        if (jsonValue == null) {
            return null;
        } else {
            switch (jsonValue.getValueType()) {
                case STRING:
                    return ((JsonString) jsonValue).getString();
                case NUMBER:
                    return ((JsonNumber) jsonValue).numberValue();
                case NULL:
                    return null;
                case ARRAY:
                    return unwrapArray(jsonValue.asJsonArray());
                case OBJECT:
                    return unwrapObject(jsonValue.asJsonObject());
                default:
                    return jsonValue == JsonValue.TRUE;
            }
        }
    }

    /**
     * Supports {@link #unwrap(JsonValue)} for typed unwrapping of JsonObject -> {@code Map<String, Object>}.
     *
     * @param jsonObject the json object to unwrap
     * @return the equivalent map
     */
    public static Map<String, Object> unwrapObject(final JsonObject jsonObject) {
        return jsonObject.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JavaxJson.unwrap(entry.getValue())));
    }

    /**
     * Supports {@link #unwrap(JsonValue)} for typed unwrapping of JsonArray -> {@code List<Object>}.
     *
     * @param jsonArray the json array to unwrap
     * @return the equivalent list
     */
    public static List<Object> unwrapArray(final JsonArray jsonArray) {
        return jsonArray.getValuesAs(JavaxJson::unwrap);
    }

    /**
     * Discrete value wrapper. Delegates to {@link #wrap(Object)} for null-handling, etc.
     */
    public static final class Value implements HasValue, As<JsonValue> {
        private final JsonValue value;

        /**
         * Wraps the value with {@link #wrap(Object)}. Nulls are allowed, Collections and Maps get special
         * treatment, and javax.json types are returned unwrapped.
         *
         * @param value the value to wrap.
         */
        private Value(final Object value) {
            this.value = wrap(value);
        }

        public JsonValue get() {
            return this.value;
        }

        /**
         * Conveniently allows one to start an array without the use of {@link #arr(Object...)}.
         *
         * @param value a second value to append to this one as a new 2-element array
         * @return a new array
         */
        public Arr val(final Object value) {
            return new Arr(this).val(value);
        }

        @Override
        public Value toValue() {
            return this;
        }
    }

    /**
     * Ensures that an object reference is wrapped appropriately for use in the DSL.
     *
     * @param value null, an instance of {@link HasValue}, or some type supported by {@link #wrap(Object)}.
     * @return a DSL-wrapped value
     */
    public static Value val(Object value) {
        if (value instanceof Cursor) {
            throw new IllegalArgumentException("dangling cursor for key: " + ((Cursor) value).getKey());
        } else if (value instanceof ObjectConvertible) {
            return val(((ObjectConvertible) value).toJson());
        } else if (value instanceof ArrayConvertible) {
            return val(((ArrayConvertible) value).toJson());
        } else if (value instanceof HasValue) {
            return ((HasValue) value).toValue();
        } else {
            return new Value(value);
        }
    }

    /**
     * Create an object with the provided key-value pairs.
     *
     * @param keys 0-many key-value pairs
     * @return a new obj
     */
    public static Obj obj(final Key... keys) {
        return new Obj(keys);
    }

    /**
     * Create an object, and add all the keys from the provided json object to it.
     *
     * @param jsonObject a starting point
     * @return a new obj
     */
    public static Obj obj(final JsonObject jsonObject) {
        if (jsonObject != null) {
            return obj(unwrapObject(jsonObject));
        } else {
            return new Obj();
        }
    }

    /**
     * Create an object, and add all the keys from the provided map to it.
     *
     * @param map a starting point
     * @return a new obj
     */
    public static Obj obj(final Map<?, ?> map) {
        return new Obj().and(map);
    }

    /**
     * Creates a key-value pair for use with {@link #obj(Key...)}. See also {@link Obj#key(String, Object)}.
     *
     * @param key   the key
     * @param value the value
     * @return a key-value pair.
     */
    public static Key key(final String key, final Object value) {
        return new Key(key, value);
    }

    /**
     * Begin a key-value pair without providing a value argument. {@code val(Object)} must be called to provide a value
     * for the key-value pair before more keys are appended.
     *
     * @param key the key
     * @return a cursor waiting for a value
     */
    public static KeyCursor key(final String key) {
        return new KeyCursor(key);
    }

    /**
     * Creates an array with the provided values.
     *
     * @param values the value elements of the array
     * @return a new array
     */
    public static Arr arr(Object... values) {
        if (values != null) {
            return new Arr(Arrays.asList(values));
        } else {
            return new Arr(Collections.emptyList());
        }
    }

    /**
     * Cursor type originating from a call to {@link JavaxJson#key(String)}, and which therefore returns a lone
     * {@link Key} for {@link KeyCursor#val(Object)}.
     */
    public static final class KeyCursor implements Cursor {
        private final String key;

        private KeyCursor(final String key) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            this.key = key;
        }

        public Key val(final Object value) {
            return key(this.key, value);
        }

        @Override
        public String getKey() {
            return this.key;
        }
    }

    /**
     * Cursor type originating from a call to {@link Key#key(String)} or {@link Obj#key(String)}, and which therefore
     * returns a new {@link Obj} with the newly-finished key appended internally.
     */
    public static final class ObjCursor implements Cursor {
        private final Obj obj;
        private final String key;

        private ObjCursor(final Obj obj, final String key) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            this.obj = obj;
            this.key = key;
        }

        public Obj val(final Object value) {
            return obj.key(this.key, value);
        }

        public Obj getObj() {
            return this.obj;
        }

        @Override
        public String getKey() {
            return key;
        }
    }

    /**
     * Iterative key-value pair type for building {@link Obj} instances. A single key will build an object with
     * {@link #get()}.
     */
    public static final class Key implements HasValue, As<JsonObject> {
        private final String key;
        private final Value value;

        private Key(final String key, final Object value) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            this.key = key;
            this.value = val(value);
        }

        String getKey() {
            return key;
        }

        Value getValue() {
            return value;
        }

        public ObjCursor key(final String key) {
            return new ObjCursor(new Obj(this), key);
        }

        public Obj key(final String key, final Object value) {
            return new Obj(this).key(key, value);
        }

        @Override
        public JsonObject get() {
            return new Obj(this).get();
        }

        @Override
        public Value toValue() {
            return new Obj(this).toValue();
        }
    }

    /**
     * Constructs an object by iterating over a list of {@link Key} instances.
     */
    public static final class Obj implements HasValue, As<JsonObject> {
        final List<Key> entries = new ArrayList<>();

        private Obj(final Key... entries) {
            if (entries != null) {
                this.entries.addAll(Arrays.asList(entries));
            }
        }

        /**
         * Append more {@link Key} instances.
         *
         * @param entries 0-many key-value pairs to add to the obj
         * @return this obj
         */
        public Obj and(final Key... entries) {
            if (entries != null) {
                this.entries.addAll(Arrays.asList(entries));
            }
            return this;
        }

        /**
         * Append the entries in {@code map} as {@link Key} instances.
         *
         * @param map a collection of key-value pairs?
         * @return this obj
         */
        public Obj and(final Map<?, ?> map) {
            if (map != null) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    this.entries.add(new Key(String.valueOf(entry.getKey()), entry.getValue()));
                }
            }
            return this;
        }

        /**
         * Begin another {@link Key} instance without providing an associated value. {@code val(Object value)} must be
         * called on the returned cursor to continue building the obj.
         *
         * @param key the key
         * @return a cursor waiting for a val.
         */
        public ObjCursor key(final String key) {
            return new ObjCursor(this, key);
        }


        /**
         * Append another {@link Key} instance. Essentially shorthand for {@code obj.and(key())} to use fewer parens
         * where desired.
         *
         * @param key   the key
         * @param value the value
         * @return this obj
         */
        public Obj key(final String key, final Object value) {
            this.entries.add(new Key(key, val(value)));
            return this;
        }

        /**
         * Builds a new {@link JsonObject} instance putting all the key value pairs in order. Duplicate keys are allowed,
         * but will overwrite previously put values.
         *
         * @return a new JSON object.
         */
        public JsonObject get() {
            JsonObjectBuilder obj = Json.createObjectBuilder();
            for (Key entry : entries) {
                if (entry != null && entry.getKey() != null) {
                    obj.add(entry.getKey(), entry.getValue().get());
                }
            }
            return obj.build();
        }

        /**
         * Transform to an array consisting of the object keys, un-deduplicated.
         *
         * @return an array of the object's keys
         */
        public Arr arrKeys() {
            return new Arr(entries.stream().map(Key::getKey).collect(Collectors.toList()));
        }

        /**
         * Transform to an array consisting of the object values.
         *
         * @return an array of the object's values
         */
        public Arr arrVals() {
            return new Arr(entries.stream().map(Key::getValue).collect(Collectors.toList()));
        }

        @Override
        public JavaxJson.Value toValue() {
            return new JavaxJson.Value(get());
        }
    }

    /**
     * Constructs an array by iterating over a list of {@link Value} instances.
     */
    public static final class Arr implements HasValue, As<JsonArray> {
        final List<Value> values = new ArrayList<>();

        private Arr(final List<Object> values) {
            if (values != null) {
                this.values.addAll(values.stream().map(JavaxJson::val).collect(Collectors.toList()));
            }
        }

        private Arr(final Value initial) {
            this.values.add(JavaxJson.val(initial));
        }

        public Arr and(final Object... values) {
            if (values != null) {
                this.values.addAll(Arrays.stream(values).map(JavaxJson::val).collect(Collectors.toList()));
            }
            return this;
        }

        public Arr val(final Object value) {
            this.values.add(JavaxJson.val(value));
            return this;
        }

        public JsonArray get() {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            for (Value value : values) {
                arr.add(value.get());
            }
            return arr.build();
        }

        @Override
        public Value toValue() {
            return new Value(get());
        }
    }

    /**
     * Retrieve a value as an object if the specified key is present, or empty() if not.
     *
     * @param json the object to retrieve an object value from
     * @param key  the array's key
     * @return an optional array
     */
    public static Optional<JsonObject> optObject(final JsonObject json, final String key) {
        return Optional.ofNullable(json.get(key)).filter(JsonObject.class::isInstance).map(JsonObject.class::cast);
    }

    /**
     * Retrieve a value as an object, or return the empty object if key is not present or if the value is the wrong type.
     *
     * @param json the object to retrieve an object value from
     * @param key  the key to retrieve
     * @return a JsonObject, always
     */
    public static JsonObject objectOrEmpty(final JsonObject json, final String key) {
        return optObject(json, key).orElse(JsonValue.EMPTY_JSON_OBJECT);
    }

    /**
     * Retrieve a value as an array if the specified key is present, or empty() if not.
     *
     * @param json the object to retrieve an array from
     * @param key  the array's key
     * @return an optional array
     */
    public static Optional<JsonArray> optArray(final JsonObject json, final String key) {
        return Optional.ofNullable(json.get(key)).filter(JsonArray.class::isInstance).map(JsonArray.class::cast);
    }

    /**
     * Retrieve a value as an array, or return the empty array if key is not present or if the value is the wrong type.
     *
     * @param json the object to retrieve an array value from
     * @param key  the key to retrieve
     * @return a JsonArray, always
     */
    public static JsonArray arrayOrEmpty(final JsonObject json, final String key) {
        return optArray(json, key).orElse(JsonValue.EMPTY_JSON_ARRAY);
    }

    /**
     * Filters and maps the provided {@link JsonArray} into a {@code List<String>}.
     *
     * @param jsonArray the array of strings
     * @return a list of strings
     */
    public static List<String> mapArrayOfStrings(final JsonArray jsonArray) {
        return mapArrayOfStrings(jsonArray, Function.identity());
    }

    /**
     * Filters and maps the provided {@link JsonArray} into a {@code List<String>}, then applies the provided mapFunction
     * to each result to return a typed list.
     *
     * @param jsonArray   the input array
     * @param mapFunction the function mapping String -> R
     * @param <R>         the mapFunction result type
     * @return a list of the same type as the mapFunction result type
     */
    public static <R> List<R> mapArrayOfStrings(final JsonArray jsonArray,
                                                final Function<String, R> mapFunction) {
        return mapArrayOfStrings(jsonArray, mapFunction, false);
    }

    /**
     * Filters and maps the provided {@link JsonArray} into a {@code List<String>}, then applies the provided mapFunction
     * to each result to return a typed list.
     *
     * @param jsonArray    the input array
     * @param mapFunction  the function mapping String -> R
     * @param discardNulls true to filter out null results returned by the mapFunction (default false).
     * @param <R>          the mapFunction result type
     * @return a list of the same type as the mapFunction result type
     */
    public static <R> List<R> mapArrayOfStrings(final JsonArray jsonArray,
                                                final Function<String, R> mapFunction,
                                                final boolean discardNulls) {
        return Optional.ofNullable(jsonArray).orElse(JsonValue.EMPTY_JSON_ARRAY).stream()
                .filter(JsonString.class::isInstance)
                .map(Util.compose(JsonString.class::cast, Util.compose(JsonString::getString, mapFunction)))
                .filter(discardNulls ? Objects::nonNull : (elem) -> true)
                .collect(Collectors.toList());
    }

    /**
     * Filters and maps the provided {@link JsonArray} into a {@code List<JsonObject>}, then applies the provided
     * mapFunction to each result to return a typed list.
     *
     * @param jsonArray   the input array
     * @param mapFunction the function mapping JsonObject -> R
     * @param <R>         the mapFunction result type
     * @return a list of the same type as the mapFunction result type
     */
    public static <R> List<R> mapArrayOfObjects(final JsonArray jsonArray,
                                                final Function<JsonObject, R> mapFunction) {
        return mapArrayOfObjects(jsonArray, mapFunction, false);
    }

    /**
     * Filters and maps the provided {@link JsonArray} into a {@code List<JsonObject>}, then applies the provided
     * mapFunction to each result to return a typed list.
     *
     * @param jsonArray    the input array
     * @param mapFunction  the function mapping JsonObject -> R
     * @param discardNulls true to filter out null results returned by the mapFunction (default false).
     * @param <R>          the mapFunction result type
     * @return a list of the same type as the mapFunction result type
     */
    public static <R> List<R> mapArrayOfObjects(final JsonArray jsonArray,
                                                final Function<JsonObject, R> mapFunction,
                                                final boolean discardNulls) {
        return Optional.ofNullable(jsonArray).orElse(JsonValue.EMPTY_JSON_ARRAY).stream()
                .filter(value -> value.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonObject.class::cast)
                .map(mapFunction)
                .filter(discardNulls ? Objects::nonNull : (elem) -> true)
                .collect(Collectors.toList());
    }

    /**
     * Parse string elements of an array into arbitrary objects, (like URLs).
     *
     * @param jsonArray     the array of strings
     * @param parser        the parser function
     * @param errorConsumer an optional error handler
     * @param <R>           the mapFunction result type
     * @return a list of parser results
     * @see Util#composeTry(Function, Supplier, Util.TryFunction, BiConsumer)
     */
    public static <R> List<R> parseFromArray(final JsonArray jsonArray,
                                             final Util.TryFunction<String, R> parser,
                                             final BiConsumer<String, Exception> errorConsumer) {
        return jsonArray.stream()
                .map(String::valueOf)
                .flatMap(composeTry(Stream::of, Stream::empty, parser, errorConsumer))
                .collect(Collectors.toList());
    }

    /**
     * Convenience method to check to avoid type errors when getting a typed value backed by a JsonValue.NULL.
     *
     * @param json the json object to check a key on.
     * @param key  the key to check for presence and non-nullness.
     * @return true if key is present and mapped to non-null value.
     */
    public static boolean hasNonNull(final JsonObject json, final String key) {
        return json.containsKey(key) && !json.isNull(key);
    }
}


