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

package net.adamcin.oakpal.maven.component;

import net.adamcin.oakpal.core.JavaxJson;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * This implementation converts a deeply-nested {@link PlexusConfiguration} graph into a {@link JsonObject}. Using the
 * {@code hint} attribute, this converter supports String coercion to JSON-equivalent Java primitive types
 * and uses {@link java.util.Map} and {@link java.util.List} to bridge to {@link JsonObject} and {@link JsonArray}
 * respectively. Valid {@code hint} values are:
 * <dl>
 * <dt>{@code list}</dt>
 * <dd>Treat children element values as sequential elements of an array</dd>
 * <dt>{@code map}</dt>
 * <dd>For each child element, treat the element name as an object key, and the element value as the associated value.</dd>
 * <dt>{@code string}</dt>
 * <dd>return {@code String.valueOf(value)}</dd>
 * <dt>{@code number}</dt>
 * <dd>Parse as a Number or fail with {@link Double#NaN}.</dd>
 * <dt>{@code boolean}</dt>
 * <dd>Parse as a Boolean or return {@code false} as default.</dd>
 * <dt>{@code json}</dt>
 * <dd>Parse strictly as a javax.json.JsonValue literal, skipping {@link #stringToValue(String)} semantics, such that
 * strings are expected to be quoted, as in:
 * <pre>
 * &lt;foo hint="json"&gt;"bar"&lt;/foo&gt;
 * </pre>
 * Non-string json types are therefore treated more predictably without inference heuristics like looking for periods or
 * the letter "E". Booleans, numerics, and nulls are therefore easily distinguished without further hinting:
 * <pre>
 * &lt;foo hint="json"&gt;true&lt;/foo&gt;
 * </pre>
 * <pre>
 * &lt;foo hint="json"&gt;3.14E12&lt;/foo&gt;
 * </pre>
 * even objects and arrays are supported natively:
 * <pre>
 * &lt;foo hint="json"&gt;{"key":"value"}&lt;/foo&gt;
 * </pre>
 * <pre>
 * &lt;foo hint="json"&gt;[1,2,3]&lt;/foo&gt;
 * </pre>
 * </dd>
 * </dl>
 * <p>
 * If a {@code hint} attribute is not specified, the type of the constructed value is inferred according to the
 * following rules:
 * <ol>
 * <li>more than two child elements: if they have the same element name, the parent is inferred as a List, otherwise it
 * is inferred as a Map.</li>
 * <li>one child element: if the parent element name and the child element name stem to the same value using
 * {@link #plexusPluralStem(String)}, the parent is assumed to be a List. Otherwise, the parent is inferred to be a
 * Map.</li>
 * <li>no children: Assume a primitive or String, and return {@link #stringToValue(String)}.</li>
 * </ol>
 */
public final class JsonConverter extends AbstractConfigurationConverter {
    static final String HINT_LIST = "list";
    static final String HINT_MAP = "map";
    static final String HINT_STRING = "string";
    static final String HINT_NUMBER = "number";
    static final String HINT_BOOLEAN = "boolean";
    static final String HINT_VALUE_LITERAL = "json";
    static final String HINT_OBJECT_LITERAL = "jsonobject";
    static final String HINT_ARRAY_LITERAL = "jsonarray";

    private static final Pattern KEY_DELIM = Pattern.compile("(_|\\W|(?=[A-Z]))");
    private static final Pattern DOUBLE_ESS_E_ESS_TO_DOUBLE_ESS = Pattern.compile("sses$");
    private static final Pattern I_E_ESS_TO_I = Pattern.compile("(?<=[a-z]{2})ies$");
    private static final Pattern I_E_ESS_TO_IE = Pattern.compile("ies$");
    // abyss -> abyss and types -> type means that y should be considered a vowel here
    // whys -> why means that w should be considered a vowel here
    private static final Pattern PRECEDING_VOWEL_NOT_DOUBLE_ESS = Pattern.compile("^.*[aeiouwy].*[^s]s$");
    private static final Pattern ESS_SUFFIX_TO_DROP = Pattern.compile("s$");
    private static final Pattern REPLACEABLE_WHY_TO_I = Pattern.compile("(?<=[a-z][^aeiou])y$");

    /**
     * Perform plural stemming on the provided key to normalize parent and child config element names for the purpose of
     * identifying implicit list configurations. This is a simplified version of the Porter stem algorithm that avoids
     * treatment of verb conjugations.
     *
     * @param key the config element name to perform stemming on.
     * @return the stemmed key
     */
    public static String plexusPluralStem(final String key) {
        String[] camelWords = KEY_DELIM.split(key);
        String lastWord = camelWords[camelWords.length - 1].toLowerCase();

        lastWord = DOUBLE_ESS_E_ESS_TO_DOUBLE_ESS.matcher(lastWord).replaceAll("ss");
        lastWord = I_E_ESS_TO_I.matcher(lastWord).replaceAll("i");
        lastWord = I_E_ESS_TO_IE.matcher(lastWord).replaceAll("ie");
        lastWord = PRECEDING_VOWEL_NOT_DOUBLE_ESS.matcher(lastWord).matches()
                ? ESS_SUFFIX_TO_DROP.matcher(lastWord).replaceAll("")
                : lastWord;
        lastWord = REPLACEABLE_WHY_TO_I.matcher(lastWord).replaceAll("i");
        return lastWord;
    }

    @Override
    public boolean canConvert(final Class<?> type) {
        return JsonObject.class.equals(type);
    }

    @Override
    public Object fromConfiguration(final ConverterLookup lookup,
                                    final PlexusConfiguration configuration,
                                    final Class<?> type,
                                    final Class<?> enclosingType,
                                    final ClassLoader loader,
                                    final ExpressionEvaluator evaluator,
                                    final ConfigurationListener listener) throws ComponentConfigurationException {
        return readJsonObjectFromConfig(configuration, evaluator);
    }

    /**
     * Perform type inference and delegate conversion appropriately.
     *
     * @param child     the child config element to convert.
     * @param evaluator the ExpressionEvaluator to use for interpolation.
     * @return the converted config structure
     * @throws ComponentConfigurationException if property interpolation fails or if an invalid hint is requested.
     */
    public JsonValue convertChild(final @NotNull PlexusConfiguration child, final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final Object value = fromExpression(child, evaluator);
        final String hint = child.getAttribute("hint");
        if (hint != null) {
            switch (hint.toLowerCase()) {
                case HINT_MAP:
                    return readJsonObjectFromConfig(child, evaluator);
                case HINT_LIST:
                    return readJsonArrayFromConfig(child, evaluator);
                case HINT_STRING:
                    return JavaxJson.wrap(String.valueOf(value));
                case HINT_NUMBER:
                    return JavaxJson.wrap(readNumberFromValue(child.getName(), value));
                case HINT_BOOLEAN:
                    return JavaxJson.wrap(readBooleanFromValue(child.getName(), value));
                case HINT_VALUE_LITERAL:
                    return readJsonValueFromValue(child.getName(), value);
                case HINT_OBJECT_LITERAL:
                    return readJsonObjectFromValue(child.getName(), value);
                case HINT_ARRAY_LITERAL:
                    return readJsonArrayFromValue(child.getName(), value);
                default:
                    throw new ComponentConfigurationException("Unsupported JSON config hint type for key: " +
                            child.getName() + ", hint=" + hint);
            }
        } else if (child.getChildCount() > 0) {
            if (mightBeList(child)) {
                return readJsonArrayFromConfig(child, evaluator);
            } else {
                return readJsonObjectFromConfig(child, evaluator);
            }
        }
        if (value instanceof String) {
            return JavaxJson.val(stringToValue((String) value)).get();
        }
        return JavaxJson.val(value).get();
    }

    boolean mightBeList(final @NotNull PlexusConfiguration config) {
        if (config.getChildCount() >= 1) {
            final String parentStem = plexusPluralStem(config.getName());
            final String firstChild = config.getChild(0).getName();
            return !parentStem.isEmpty()
                    && parentStem.equals(plexusPluralStem(firstChild))
                    && Arrays.stream(config.getChildren())
                    .allMatch(child -> firstChild.equals(child.getName()));
        }
        return false;
    }

    JsonObject readJsonObjectFromConfig(final @NotNull PlexusConfiguration config,
                                        final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final JsonObjectBuilder retValue = Json.createObjectBuilder();
        for (PlexusConfiguration child : config.getChildren()) {
            final String key = child.getName();
            retValue.add(key, convertChild(child, evaluator));
        }
        return retValue.build();
    }

    JsonArray readJsonArrayFromConfig(final @NotNull PlexusConfiguration config,
                                      final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final JsonArrayBuilder retValue = Json.createArrayBuilder();
        for (PlexusConfiguration child : config.getChildren()) {
            retValue.add(convertChild(child, evaluator));
        }
        return retValue.build();
    }


    JsonValue readJsonObjectFromValue(final String key, final @Nullable Object value) throws ComponentConfigurationException {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        } else if (value instanceof String) {
            try (JsonReader reader = Json.createReader(new StringReader((String) value))) {
                return reader.readObject();
            } catch (JsonException e) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as JsonObject.", e);
            }
        }
        return JsonObject.NULL;
    }

    JsonValue readJsonArrayFromValue(final String key, final @Nullable Object value) throws ComponentConfigurationException {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        } else if (value instanceof String) {
            try (JsonReader reader = Json.createReader(new StringReader((String) value))) {
                return reader.readArray();
            } catch (JsonException e) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as JsonArray.", e);
            }
        }
        return JsonObject.NULL;
    }

    JsonValue readJsonValueFromValue(final String key, final @Nullable Object value) throws ComponentConfigurationException {
        if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value instanceof String) {
            try (JsonReader reader = Json.createReader(new StringReader((String) value))) {
                return reader.readValue();
            } catch (JsonException e) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as JsonValue.", e);
            }
        }
        return JsonObject.NULL;
    }

    Number readNumberFromValue(final String key, final @Nullable Object value) throws ComponentConfigurationException {
        if (value instanceof Number) {
            return (Number) value;
        } else if (value instanceof String) {
            Object readValue = stringToValue((String) value);
            if (readValue instanceof Number) {
                return (Number) readValue;
            } else if (!isBlankish(readValue)) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as number.");
            }
        }
        return null;
    }

    boolean isBlankish(final Object value) {
        return value == null || (value instanceof String && ((String) value).trim().isEmpty());
    }

    Boolean readBooleanFromValue(final String key, final @Nullable Object value) throws ComponentConfigurationException {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            Object readValue = stringToValue((String) value);
            if (readValue instanceof Boolean) {
                return (Boolean) readValue;
            } else if (!isBlankish(readValue)) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as boolean.");
            }
        }
        return false;
    }


    /**
     * Borrowed from org.json.JSONObject.
     * <p>
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     *
     * @param string A String.
     * @return A simple JSON-wrappable value.
     */
    public static Object stringToValue(String string) {
        if (string.equals("")) {
            return string;
        }
        if (string.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (string.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (string.equalsIgnoreCase("null")) {
            return null;
        }

        /*
         * If it might be a number, try converting it. If a number cannot be
         * produced, then the value will just be a string.
         */

        char initial = string.charAt(0);
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            try {
                if (string.indexOf('.') > -1 || string.indexOf('e') > -1
                        || "-NaN".equals(string) || "-Infinity".equals(string)
                        || string.indexOf('E') > -1
                        || "-0".equals(string)) {
                    Double d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    Long myLong = new Long(string);
                    if (string.equals(myLong.toString())) {
                        if (myLong.longValue() == myLong.intValue()) {
                            return myLong.intValue();
                        }
                        return myLong;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return string;
    }
}
