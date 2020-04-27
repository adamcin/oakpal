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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.Fun;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson}
 */
@Deprecated
public final class JavaxJson {
    private JavaxJson() {
        /* no instantiation */
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#nonEmptyValue(JsonValue)}
     */
    @Deprecated
    public static boolean nonEmptyValue(final @Nullable JsonValue value) {
        return net.adamcin.oakpal.api.JavaxJson.nonEmptyValue(value);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#nonDefaultValue(JsonValue, JsonValue)}
     */
    @Deprecated
    public static boolean nonDefaultValue(final @Nullable JsonValue value, final @NotNull JsonValue defaultValue) {
        return net.adamcin.oakpal.api.JavaxJson.nonDefaultValue(value, defaultValue);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#wrap(Object)}
     */
    @Deprecated
    public static JsonValue wrap(final Object object) {
        return net.adamcin.oakpal.api.JavaxJson.wrap(object);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#unwrap(JsonValue)}
     */
    @Deprecated
    public static Object unwrap(final JsonValue jsonValue) {
        return net.adamcin.oakpal.api.JavaxJson.unwrap(jsonValue);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#JSON_VALUE_STRING}
     */
    @Deprecated
    public static final Function<JsonValue, String> JSON_VALUE_STRING = net.adamcin.oakpal.api.JavaxJson.JSON_VALUE_STRING;

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#unwrapObject(JsonObject)}
     */
    @Deprecated
    public static Map<String, Object> unwrapObject(final JsonObject jsonObject) {
        return net.adamcin.oakpal.api.JavaxJson.unwrapObject(jsonObject);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#unwrapArray(JsonArray)}
     */
    @Deprecated
    public static List<Object> unwrapArray(final JsonArray jsonArray) {
        return net.adamcin.oakpal.api.JavaxJson.unwrapArray(jsonArray);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#val(Object)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.Value val(Object value) {
        return net.adamcin.oakpal.api.JavaxJson.val(value);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#obj(net.adamcin.oakpal.api.JavaxJson.Key...)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.Obj obj(final net.adamcin.oakpal.api.JavaxJson.Key... keys) {
        return net.adamcin.oakpal.api.JavaxJson.obj(keys);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#obj(JsonObject)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.Obj obj(final JsonObject jsonObject) {
        return net.adamcin.oakpal.api.JavaxJson.obj(jsonObject);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#obj(Map)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.Obj obj(final Map<?, ?> map) {
        return net.adamcin.oakpal.api.JavaxJson.obj(map);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#key(String, Object)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.Key key(final String key, final Object value) {
        return net.adamcin.oakpal.api.JavaxJson.key(key, value);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#key(String)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.KeyCursor key(final String key) {
        return net.adamcin.oakpal.api.JavaxJson.key(key);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#arr(Object...)}
     */
    @Deprecated
    public static net.adamcin.oakpal.api.JavaxJson.Arr arr(Object... values) {
        return net.adamcin.oakpal.api.JavaxJson.arr(values);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#optObject(JsonObject, String)}
     */
    @Deprecated
    public static Optional<JsonObject> optObject(final JsonObject json, final String key) {
        return net.adamcin.oakpal.api.JavaxJson.optObject(json, key);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#objectOrEmpty(JsonObject, String)}
     */
    @Deprecated
    public static JsonObject objectOrEmpty(final JsonObject json, final String key) {
        return net.adamcin.oakpal.api.JavaxJson.objectOrEmpty(json, key);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#optArray(JsonObject, String)}
     */
    @Deprecated
    public static Optional<JsonArray> optArray(final JsonObject json, final String key) {
        return net.adamcin.oakpal.api.JavaxJson.optArray(json, key);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#arrayOrEmpty(JsonObject, String)}
     */
    @Deprecated
    public static JsonArray arrayOrEmpty(final JsonObject json, final String key) {
        return net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty(json, key);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#mapArrayOfStrings(JsonArray)}
     */
    @Deprecated
    public static List<String> mapArrayOfStrings(final JsonArray jsonArray) {
        return net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings(jsonArray);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#mapArrayOfStrings(JsonArray, Function)}
     */
    @Deprecated
    public static <R> List<R> mapArrayOfStrings(final JsonArray jsonArray,
                                                final Function<String, R> mapFunction) {
        return net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings(jsonArray, mapFunction);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#mapArrayOfStrings(JsonArray, Function, boolean)}
     */
    @Deprecated
    public static <R> List<R> mapArrayOfStrings(final JsonArray jsonArray,
                                                final Function<String, R> mapFunction,
                                                final boolean discardNulls) {
        return net.adamcin.oakpal.api.JavaxJson.mapArrayOfStrings(jsonArray, mapFunction, discardNulls);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#mapArrayOfObjects(JsonArray, Function)}
     */
    @Deprecated
    public static <R> List<R> mapArrayOfObjects(final JsonArray jsonArray,
                                                final Function<JsonObject, R> mapFunction) {
        return net.adamcin.oakpal.api.JavaxJson.mapArrayOfObjects(jsonArray, mapFunction);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#mapArrayOfObjects(JsonArray, Function, boolean)}
     */
    @Deprecated
    public static <R> List<R> mapArrayOfObjects(final JsonArray jsonArray,
                                                final Function<JsonObject, R> mapFunction,
                                                final boolean discardNulls) {
        return net.adamcin.oakpal.api.JavaxJson.mapArrayOfObjects(jsonArray, mapFunction, discardNulls);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#mapObjectValues(JsonObject, BiFunction, boolean)}
     */
    @Deprecated
    public static <R> List<R> mapObjectValues(final JsonObject jsonObject,
                                              final BiFunction<String, JsonObject, R> mapBiFunction,
                                              final boolean discardNulls) {
        return net.adamcin.oakpal.api.JavaxJson.mapObjectValues(jsonObject, mapBiFunction, discardNulls);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#parseFromArray(JsonArray, Fun.ThrowingFunction, BiConsumer)}
     */
    @Deprecated
    public static <R> List<R> parseFromArray(final @NotNull JsonArray jsonArray,
                                             final @NotNull Fun.ThrowingFunction<String, R> parser,
                                             final @Nullable BiConsumer<String, Exception> errorConsumer) {
        return net.adamcin.oakpal.api.JavaxJson.parseFromArray(jsonArray, parser, errorConsumer);
    }

    /**
     * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.JavaxJson#hasNonNull(JsonObject, String)}
     */
    @Deprecated
    public static boolean hasNonNull(final @NotNull JsonObject json, final String key) {
        return net.adamcin.oakpal.api.JavaxJson.hasNonNull(json, key);
    }
}
