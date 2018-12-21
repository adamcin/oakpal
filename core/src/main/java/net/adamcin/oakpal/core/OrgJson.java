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
    public interface ObjectConvertible {
        JSONObject toJSON();
    }

    public interface ArrayConvertible {
        JSONArray toJSON();
    }

    public interface HasValue {
        Value toValue();
    }

    public interface As<TYPE> {
        TYPE get();
    }

    /**
     * Ensures that an object reference is wrapped appropriately for use in the DSL.
     *
     * @param value null, an instance of {@link HasValue}, or some type supported by {@link JSONObject#wrap(Object)}.
     * @return a DSL-wrapped value
     */
    public static Value val(Object value) {
        if (value instanceof ObjectConvertible) {
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
     * @param jsonObject a starting point
     * @return a new obj
     */
    public static Obj obj(final JSONObject jsonObject) {
        final Obj object = new Obj();
        if (jsonObject != null) {
            object.and(jsonObject.toMap());
        }
        return object;
    }

    /**
     * Creates a key-value pair for use with {@link #obj(Key...)}. See also {@link Obj#key(String, Object)}.
     *
     * @param key   the key
     * @param value the value
     * @return a key-value pair.
     */
    public static Key key(String key, Object value) {
        return new Key(key, value);
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

        private Value(final Object value) {
            this.value = JSONObject.wrap(value);
        }

        public Object get() {
            return this.value;
        }

        public Arr val(final Object value) {
            return new Arr(this).val(value);
        }

        @Override
        public Value toValue() {
            return this;
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
            this.key = key;
            this.value = val(value);
        }

        String getKey() {
            return key;
        }

        Value getValue() {
            return value;
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
         * Append another {@link Key} instance. Essentially shorthand for {@code obj.and(key())} to use fewer parens
         * where desired.
         *
         * @param key   the key
         * @param value the value
         * @return this obj
         */
        public Obj key(final String key, final Object value) {
            if (key != null) {
                this.entries.add(new Key(key, val(value)));
            }
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
