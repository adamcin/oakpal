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

package net.adamcin.oakpal.maven.component;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This implementation converts a deeply-nested {@link PlexusConfiguration} graph into a {@link JSONObject}. Using the
 * {@code hint} attribute, this converter supports String coercion to JSON-equivalent Java primitive types
 * and uses {@link java.util.Map} and {@link java.util.List} to bridge to {@link JSONObject} and {@link JSONArray}
 * respectively. Valid {@code hint} values are:
 * <dl>
 * <dt>{@code List}</dt>
 * <dd>Treat children element values as sequential elements of an array</dd>
 * <dt>{@code Map}</dt>
 * <dd>For each child element, treat the element name as an object key, and the element value as the associated value.</dd>
 * <dt>{@code String}</dt>
 * <dd>return {@code String.valueOf(value)}</dd>
 * <dt>{@code Number}</dt>
 * <dd>Parse as a Number or fail with {@link Double#NaN}.</dd>
 * <dt>{@code Boolean}</dt>
 * <dd>Parse as a Boolean or return {@code false} as default.</dd>
 * <dt>{@code JSONObject}</dt>
 * <dd>Parse as a JSONObject literal, as in
 * <pre>
 * &lt;foo&gt;{"bar":true}&lt;/foo&gt;
 * </pre>
 * </dd>
 * <dt>{@code JSONArray}</dt>
 * <dd>Parse as a JSONArray literal, as in
 * <pre>
 * &lt;foo&gt;["bar","alice"]&lt;/foo&gt;
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
 * <li>no children: Assume a primitive or String, and return {@link JSONObject#stringToValue(String)}.</li>
 * </ol>
 */
public final class JSONObjectConverter extends AbstractConfigurationConverter {
    static final String HINT_LIST = "List";
    static final String HINT_MAP = "Map";
    static final String HINT_STRING = "String";
    static final String HINT_NUMBER = "Number";
    static final String HINT_BOOLEAN = "Boolean";
    static final String HINT_OBJECT_LITERAL = "JSONObject";
    static final String HINT_ARRAY_LITERAL = "JSONArray";

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
        return JSONObject.class.equals(type);
    }

    @Override
    public Object fromConfiguration(final ConverterLookup lookup,
                                    final PlexusConfiguration configuration,
                                    final Class<?> type,
                                    final Class<?> enclosingType,
                                    final ClassLoader loader,
                                    final ExpressionEvaluator evaluator,
                                    final ConfigurationListener listener) throws ComponentConfigurationException {
        return readJSONObjectFromConfig(configuration, evaluator);
    }

    /**
     * Perform type inference and delegate conversion appropriately.
     *
     * @param child     the child config element to convert.
     * @param evaluator the ExpressionEvaluator to use for interpolation.
     * @return the converted config structure
     * @throws ComponentConfigurationException if property interpolation fails or if an invalid hint is requested.
     */
    public Object convertChild(final PlexusConfiguration child, final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final Object value = fromExpression(child, evaluator);
        final String hint = child.getAttribute("hint");
        if (hint != null) {
            switch (hint) {
                case HINT_MAP:
                    return readJSONObjectFromConfig(child, evaluator);
                case HINT_LIST:
                    return readJSONArrayFromConfig(child, evaluator);
                case HINT_STRING:
                    return String.valueOf(value);
                case HINT_NUMBER:
                    return readNumberFromValue(value);
                case HINT_BOOLEAN:
                    return readBooleanFromValue(value);
                case HINT_OBJECT_LITERAL:
                    return readJSONObjectFromValue(child.getName(), value);
                case HINT_ARRAY_LITERAL:
                    return readJSONArrayFromValue(child.getName(), value);
                default:
                    throw new ComponentConfigurationException("Unsupported JSON config hint type for key: " +
                            child.getName() + ", hint=" + hint);
            }
        } else if (child.getChildCount() > 0) {
            if (mightBeList(child)) {
                return readJSONArrayFromConfig(child, evaluator);
            } else {
                return readJSONObjectFromConfig(child, evaluator);
            }
        }
        if (value instanceof String) {
            return JSONObject.stringToValue((String) value);
        }
        return value;
    }

    boolean mightBeList(final PlexusConfiguration config) {
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

    Object readJSONObjectFromValue(final String key, final Object value) throws ComponentConfigurationException {
        if (value instanceof JSONObject) {
            return value;
        } else if (value instanceof String) {
            try {
                return new JSONObject((String) value);
            } catch (JSONException e) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as JSONObject.", e);
            }
        } else {
            return JSONObject.NULL;
        }
    }

    Object readJSONArrayFromValue(final String key, final Object value) throws ComponentConfigurationException {
        if (value instanceof JSONArray) {
            return value;
        } else if (value instanceof String) {
            try {
                return new JSONArray((String) value);
            } catch (JSONException e) {
                throw new ComponentConfigurationException("Failed to parse value for key " + key + " as JSONArray.", e);
            }
        } else {
            return JSONObject.NULL;
        }
    }

    Number readNumberFromValue(final Object value) {
        if (value instanceof Number) {
            return (Number) value;
        } else if (value instanceof String) {
            Object jsonValue = JSONObject.stringToValue((String) value);
            if (jsonValue instanceof Number) {
                return (Number) jsonValue;
            } else {
                return Double.NaN;
            }
        } else {
            return Double.NaN;
        }
    }

    Boolean readBooleanFromValue(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            Object jsonValue = JSONObject.stringToValue((String) value);
            if (jsonValue instanceof Boolean) {
                return (Boolean) jsonValue;
            } else {
                return !((String) value).isEmpty() && jsonValue != JSONObject.NULL;
            }
        } else {
            return false;
        }
    }

    JSONObject readJSONObjectFromConfig(final PlexusConfiguration config, final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final JSONObject retValue = new JSONObject();
        for (PlexusConfiguration child : config.getChildren()) {
            final String key = child.getName();
            retValue.put(key, convertChild(child, evaluator));
        }
        return retValue;
    }

    JSONArray readJSONArrayFromConfig(final PlexusConfiguration config, final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final JSONArray retValue = new JSONArray();
        for (PlexusConfiguration child : config.getChildren()) {
            retValue.put(convertChild(child, evaluator));
        }
        return retValue;
    }

}
