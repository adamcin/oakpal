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
 * {@code implementation} attribute, this converter supports String coercion to JSON-equivalent Java primitive types
 * and uses {@link java.util.Map} and {@link java.util.List} to bridge to {@link JSONObject} and {@link JSONArray}
 * respectively.
 */
public class JSONObjectConverter extends AbstractConfigurationConverter {
    static final String IMPL_LIST = "java.util.List";
    static final String IMPL_MAP = "java.util.Map";
    static final String IMPL_NUMBER = "java.lang.Number";
    static final String IMPL_BOOLEAN = "java.lang.Boolean";
    static final String IMPL_OBJECT_LITERAL = "org.json.JSONObject";
    static final String IMPL_ARRAY_LITERAL = "org.json.JSONArray";

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
     * @throws ComponentConfigurationException if property interpolation fails or if an invalid implementation class is
     *                                         requested.
     */
    Object convertChild(final PlexusConfiguration child, final ExpressionEvaluator evaluator)
            throws ComponentConfigurationException {
        final String impl = child.getAttribute("implementation");
        Object value = fromExpression(child, evaluator);
        if (impl != null) {
            switch (impl) {
                case IMPL_MAP:
                    return readJSONObjectFromConfig(child, evaluator);
                case IMPL_LIST:
                    return readJSONArrayFromConfig(child, evaluator);
                case IMPL_NUMBER:
                    return readNumberFromValue(value);
                case IMPL_BOOLEAN:
                    return readBooleanFromValue(value);
                case IMPL_OBJECT_LITERAL:
                    return readJSONObjectFromValue(child.getName(), value);
                case IMPL_ARRAY_LITERAL:
                    return readJSONArrayFromValue(child.getName(), value);
                default:
                    throw new ComponentConfigurationException("Unsupported JSON config implementation type for key: " +
                            child.getName() + ", implementation=" + impl);
            }
        } else if (child.getChildCount() > 0) {
            if (mightBeList(child)) {
                return readJSONArrayFromConfig(child, evaluator);
            } else {
                return readJSONObjectFromConfig(child, evaluator);
            }
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
     * identifying implicit list configurations.
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

}
