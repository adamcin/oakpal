/*
 * Copyright 2018 Mark Adamcin
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Simple DSL for constructing org.json objects for {@link ProgressCheckFactory} configs using only three-letter identifiers.
 * <p>
 * Recommend using {@code import static net.adamcin.oakpal.core.OrgJson.*;}
 */
public final class OrgJson {

    private OrgJson() {
        // no construction
    }

    /**
     * Custom pojo types which should be usable within this DSL should implement this method to provide a
     * {@link JSONObject}, which can be wrapped quickly by {@link #val(Object)}.
     */
    public interface ObjectConvertible {
        JSONObject toJSON();
    }

    /**
     * Custom pojo types which should be usable within this DSL should implement this method to provide a
     * {@link JSONArray}, which can be wrapped quickly by {@link #val(Object)}.
     */
    public interface ArrayConvertible {
        JSONArray toJSON();
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
     * Ensures that an object reference is wrapped appropriately for use in the DSL.
     *
     * @param value null, an instance of {@link HasValue}, or some type supported by {@link JSONObject#wrap(Object)}.
     * @return a DSL-wrapped value
     */
    public static Value val(Object value) {
        if (value instanceof Cursor) {
            throw new IllegalArgumentException("dangling cursor for key: " + ((Cursor) value).getKey());
        } else if (value instanceof ObjectConvertible) {
            return val(((ObjectConvertible) value).toJSON());
        } else if (value instanceof ArrayConvertible) {
            return val(((ArrayConvertible) value).toJSON());
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
    public static Obj obj(final JSONObject jsonObject) {
        if (jsonObject != null) {
            return obj(jsonObject.toMap());
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
     * Discrete value wrapper. Delegates to {@link JSONObject#wrap(Object)} for null-handling, etc.
     */
    public static final class Value implements HasValue, As<Object> {
        private final Object value;

        /**
         * Wraps the value with {@link JSONObject#wrap(Object)}. Nulls are allowed, Collections and Maps get special
         * treatment, and org.json types are returned unwrapped.
         *
         * @param value the value to wrap.
         */
        private Value(final Object value) {
            this.value = JSONObject.wrap(value);
        }

        public Object get() {
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
     * Cursor type originating from a call to {@link OrgJson#key(String)}, and which therefore returns a lone
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
    public static final class Key implements HasValue, As<JSONObject> {
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
        public JSONObject get() {
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
    public static final class Obj implements HasValue, As<JSONObject> {
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
         * Builds a new {@link JSONObject} instance putting all the key value pairs in order. Duplicate keys are allowed,
         * but will overwrite previously put values.
         *
         * @return a new JSON object.
         */
        public JSONObject get() {
            JSONObject obj = new JSONObject();
            for (Key entry : entries) {
                if (entry != null && entry.getKey() != null) {
                    obj.put(entry.getKey(), entry.getValue().get());
                }
            }
            return obj;
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
        public Value toValue() {
            return new Value(get());
        }
    }

    /**
     * Constructs an array by iterating over a list of {@link Value} instances.
     */
    public static final class Arr implements HasValue, As<JSONArray> {
        final List<Value> values = new ArrayList<>();

        private Arr(final List<Object> values) {
            if (values != null) {
                this.values.addAll(values.stream().map(OrgJson::val).collect(Collectors.toList()));
            }
        }

        private Arr(final Value initial) {
            this.values.add(OrgJson.val(initial));
        }

        public Arr and(final Object... values) {
            if (values != null) {
                this.values.addAll(Arrays.stream(values).map(OrgJson::val).collect(Collectors.toList()));
            }
            return this;
        }

        public Arr val(final Object value) {
            this.values.add(OrgJson.val(value));
            return this;
        }

        public JSONArray get() {
            JSONArray arr = new JSONArray();
            for (Value value : values) {
                arr.put(value.get());
            }
            return arr;
        }

        @Override
        public Value toValue() {
            return new Value(get());
        }
    }
}
