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

import net.adamcin.oakpal.api.JavaxJson;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.io.XmlPlexusConfigurationReader;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.uncheck0;
import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonConverterTest {

    private final File testInputBaseDir = new File("src/test/resources/JsonConverterTest");
    private final File testOutputBaseDir = new File("target/test-out/JsonConverterTest");

    @Before
    public void setUp() throws Exception {
        testOutputBaseDir.mkdirs();
    }

    @Test
    public void testPlexusPluralStem() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("meetings", "meeting");
        cases.put("caresses", "caress");
        cases.put("flatFiles", "file");
        cases.put("pinkUnicornPonies", "poni");
        cases.put("pony", "poni");
        cases.put("some_people_like_snakes", "snake");
        cases.put("snake", "snake");
        cases.put("some.people.like.dots", "dot");
        cases.put("denyNodeTypes", "type");
        cases.put("denyNodeType", "type");
        cases.put("abyss", "abyss");
        cases.put("why", "whi");
        cases.put("whys", "whi");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Assert.assertEquals("should stem to", entry.getValue(),
                    JsonConverter.plexusPluralStem(entry.getKey()));
        }

    }

    @Test
    public void testStringToValue() {
        final String empty = "";
        assertSame("same empty ", empty, JsonConverter.stringToValue(empty));
        assertSame("same true", Boolean.TRUE, JsonConverter.stringToValue("true"));
        assertSame("same false", Boolean.FALSE, JsonConverter.stringToValue("false"));
        assertNull("same null", JsonConverter.stringToValue("null"));
        final String nan = "-NaN";
        assertSame("same nan", nan, JsonConverter.stringToValue(nan));
        assertEquals("read int", 42, JsonConverter.stringToValue("42"));
        assertEquals("read long", ((long) Integer.MAX_VALUE) + 1,
                JsonConverter.stringToValue(Long.toString(((long) Integer.MAX_VALUE) + 1)));
        assertEquals("read double", 42.0D, (double) JsonConverter.stringToValue("42.0"), 1.0D);
        assertEquals("maybe number? on second thought...", "-0foo", JsonConverter.stringToValue("-0foo"));
    }

    static JsonConverter converter() {
        return new JsonConverter();
    }

    @Test
    public void testCanConvert() {
        assertTrue("can convert json objects", converter().canConvert(JsonObject.class));
        assertFalse("cannot convert objects", converter().canConvert(Object.class));
    }

    @Test
    public void testReadBooleanFromValue() throws Exception {
        assertSame("box true", Boolean.TRUE, converter().readBooleanFromValue("", Boolean.TRUE));
        assertSame("box false", Boolean.FALSE, converter().readBooleanFromValue("", Boolean.FALSE));
        assertTrue("string true", converter().readBooleanFromValue("", "true"));
        assertFalse("string false", converter().readBooleanFromValue("", "false"));
        assertFalse("null false", converter().readBooleanFromValue("", null));
        assertFalse("empty string false", converter().readBooleanFromValue("", ""));
        assertFalse("null string false", converter().readBooleanFromValue("", "null"));
        assertFalse("integer always false", converter().readBooleanFromValue("", 1));
        assertFalse("long always false", converter().readBooleanFromValue("", 1L));
        assertFalse("float always false", converter().readBooleanFromValue("", 1.0F));
        assertFalse("double always false", converter().readBooleanFromValue("", 1.0D));
    }


    @Test
    public void testReadNumberFromValue() throws Exception {
        Number[] numbers = Stream.of(1, 2.0, ((long) Integer.MAX_VALUE) + 4,
                ((Float) Float.MAX_VALUE).doubleValue() + 1.0D)
                .toArray(Number[]::new);
        for (Number number : numbers) {
            assertSame("same number", number, converter().readNumberFromValue("", number));
            assertEquals("toString equals", number, converter().readNumberFromValue("", number.toString()));
        }
        assertNull("should be null", converter().readNumberFromValue("", Boolean.TRUE));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testReadNumberFromValue_nonNumericString() throws Exception {
        converter().readNumberFromValue("", "foo");
    }

    @Test
    public void testReadJsonValueFromValue() throws Exception {
        assertSame("null value is JsonValue.NULL", JsonValue.NULL,
                converter().readJsonValueFromValue("key", null));
        assertSame("same json null value", JsonValue.NULL,
                converter().readJsonValueFromValue("key", JsonValue.NULL));
        assertSame("same json empty object", JsonValue.EMPTY_JSON_OBJECT,
                converter().readJsonValueFromValue("key", JsonValue.EMPTY_JSON_OBJECT));
        assertSame("same json empty array", JsonValue.EMPTY_JSON_ARRAY,
                converter().readJsonValueFromValue("key", JsonValue.EMPTY_JSON_ARRAY));
        assertEquals("\"some string\" is readable", JavaxJson.wrap("some string"),
                converter().readJsonValueFromValue("key", "\"some string\""));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testReadJsonValueFromValue_throws() throws Exception {
        converter().readJsonValueFromValue("key", "some string");
    }

    @Test
    public void testReadJsonArrayFromValue() throws Exception {
        final JsonArray expectSameArray = arr("one", "two", "three").get();
        assertSame("same json array", expectSameArray, converter().readJsonArrayFromValue("key", expectSameArray));
        assertSame("null value is JsonValue.NULL", JsonValue.NULL,
                converter().readJsonArrayFromValue("key", null));
        assertEquals("json array as string is equal", expectSameArray,
                converter().readJsonArrayFromValue("key", expectSameArray.toString()));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testReadJsonArrayFromValue_throws() throws Exception {
        converter().readJsonArrayFromValue("key", "some string");
    }

    @Test
    public void testReadJsonObjectFromValue() throws Exception {
        final JsonObject expectSameObject = key("foo", "one").key("bar", "two").get();
        assertSame("same json object", expectSameObject, converter().readJsonObjectFromValue("key", expectSameObject));
        assertSame("null value is JsonValue.NULL", JsonValue.NULL,
                converter().readJsonObjectFromValue("key", null));
        assertEquals("json object as string is equal", expectSameObject,
                converter().readJsonObjectFromValue("key", expectSameObject.toString()));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testReadJsonObjectFromValue_throws() throws Exception {
        converter().readJsonObjectFromValue("key", "some string");
    }

    private static DefaultPlexusConfiguration plex(final @NotNull String name) {
        return new DefaultPlexusConfiguration(name);
    }

    private static DefaultPlexusConfiguration plex(final @NotNull String name, final String value) {
        return new DefaultPlexusConfiguration(name);
    }

    private PlexusConfiguration xml(final @NotNull String filename) {
        return uncheck0(() -> {
            try (InputStreamReader reader =
                         new InputStreamReader(new FileInputStream(
                                 new File(testInputBaseDir, filename)), StandardCharsets.UTF_8)) {
                return new XmlPlexusConfigurationReader().read(reader);
            }
        }).get();
    }

    private PlexusConfiguration xml(final @NotNull String filename, final @NotNull String hint) {
        final PlexusConfiguration config = xml(filename);
        config.setAttribute("hint", hint);
        return config;
    }

    @Test
    public void testXml() {
        PlexusConfiguration config = xml("fooBar.xml");
        assertEquals("name is", "foo", config.getName());
        assertEquals("value is", "bar", config.getValue());
    }

    @Test
    public void testXmlHint() {
        PlexusConfiguration config = xml("fooBar.xml", "someHint");
        assertEquals("name is", "foo", config.getName());
        assertEquals("value is", "bar", config.getValue());
        assertEquals("hint is", "someHint", config.getAttribute("hint"));
    }

    @Test
    public void testConvertChild_stringToString() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("expect json string", JavaxJson.wrap("bar"),
                converter().convertChild(xml("fooBar.xml"), eval));
    }

    @Test
    public void testConvertChild_expectNull() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> null);
        assertSame("expect json null for null", JsonValue.NULL,
                converter().convertChild(xml("fooBar.xml"), eval));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testConvertChild_invalidHint() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        converter().convertChild(xml("invalidHint.xml"), eval);
    }

    @Test
    public void testConvertChild_wrapNonStringValue() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class).indexOf("t") == 0);
        assertEquals("expect true for empty string", JavaxJson.wrap(true),
                converter().convertChild(xml("topFoo.xml"), eval));
        assertEquals("expect false for non-empty string", JavaxJson.wrap(false),
                converter().convertChild(xml("fooFoo.xml"), eval));
    }

    @Test
    public void testMightBeList() {
        assertFalse("expect json string might not be list ",
                converter().mightBeList(xml("fooBar.xml")));
        assertTrue("expect json array might be list ",
                converter().mightBeList(xml("foosFoo.xml")));
        assertFalse("expect json object might not be list ",
                converter().mightBeList(xml("foosBar.xml")));
    }

    @Test
    public void testReadJsonObjectFromConfig() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("expect empty json object for empty value", JsonValue.EMPTY_JSON_OBJECT,
                converter().readJsonObjectFromConfig(xml("emptyFoo.xml"), eval));
        assertEquals("expect json object for value", key("foo", "one").get(),
                converter().readJsonObjectFromConfig(xml("foosFoo.xml"), eval));
        assertEquals("expect json object for value", key("bar", "one").get(),
                converter().readJsonObjectFromConfig(xml("foosBar.xml"), eval));
    }

    @Test
    public void testReadJsonArrayFromConfig() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("expect empty json array for empty value", JsonValue.EMPTY_JSON_ARRAY,
                converter().readJsonArrayFromConfig(xml("emptyFoo.xml"), eval));
        assertEquals("expect json array for value", arr("one").get(),
                converter().readJsonArrayFromConfig(xml("foosFoo.xml"), eval));
        assertEquals("expect json array for value", arr("one").get(),
                converter().readJsonArrayFromConfig(xml("foosBar.xml"), eval));
    }

    @Test
    public void testConvertChild_mightBeList() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("expect json array for value", arr("one").get(),
                converter().convertChild(xml("foosFoo.xml"), eval));
        assertEquals("expect json object for value", key("bar", "one").get(),
                converter().convertChild(xml("foosBar.xml"), eval));
    }

    @Test
    public void testConvertChild_hintMap() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("expect json object for value", key("foo", "one").get(),
                converter().convertChild(xml("foosFoo.xml", "map"), eval));
        assertEquals("expect json object for value", key("bar", "one").get(),
                converter().convertChild(xml("foosBar.xml", "map"), eval));
    }

    @Test
    public void testConvertChild_hintList() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("expect json array for value", arr("one").get(),
                converter().convertChild(xml("foosFoo.xml", "list"), eval));
        assertEquals("expect json array for value", arr("one").get(),
                converter().convertChild(xml("foosBar.xml", "list"), eval));
    }

    @Test
    public void testConvertChild_hintString() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertSame("expect json true for value", JsonValue.TRUE,
                converter().convertChild(xml("fooTrue.xml"), eval));
        assertEquals("expect json string 'true' for value", JavaxJson.wrap("true"),
                converter().convertChild(xml("fooTrue.xml", "string"), eval));
        assertEquals("expect json number for value", JavaxJson.wrap(-500),
                converter().convertChild(xml("fooNumber.xml"), eval));
        assertEquals("expect json string '-500' for value", JavaxJson.wrap("-500"),
                converter().convertChild(xml("fooNumber.xml", "string"), eval));
    }

    @Test
    public void testConvertChild_hintNumber() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertSame("expect json true for value", JsonValue.TRUE,
                converter().convertChild(xml("fooTrue.xml"), eval));
        assertEquals("expect json number for value", JavaxJson.wrap(-500),
                converter().convertChild(xml("fooNumber.xml"), eval));
        assertEquals("expect json string '-500' for value", JavaxJson.wrap(-500),
                converter().convertChild(xml("fooNumber.xml", "number"), eval));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testConvertChild_hintNumber_NaN() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        converter().convertChild(xml("fooTrue.xml", "number"), eval);
    }

    @Test
    public void testConvertChild_hintBoolean() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertSame("true is true", JsonValue.TRUE,
                converter().convertChild(xml("fooTrue.xml"), eval));
        assertSame("true is true", JsonValue.TRUE,
                converter().convertChild(xml("fooTrue.xml", "boolean"), eval));
        assertSame("non-boolean, empty string is false", JsonValue.FALSE,
                converter().convertChild(xml("emptyFoo.xml", "boolean"), eval));
        assertSame("non-boolean, number is true", JsonValue.FALSE,
                converter().convertChild(xml("emptyFoo.xml", "boolean"), eval));
    }

    @Test(expected = ComponentConfigurationException.class)
    public void testConvertChild_hintBoolean_throws() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        converter().convertChild(xml("fooBar.xml", "boolean"), eval);
    }

    @Test
    public void testConvertChild_jsonValueLiteral() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertSame("json true", JsonValue.TRUE, converter().convertChild(xml("fooTrue.xml", "json"), eval));
        assertSame("json false", JsonValue.FALSE, converter().convertChild(xml("fooFalse.xml", "json"), eval));
        assertSame("json null", JsonValue.NULL, converter().convertChild(xml("fooNull.xml", "json"), eval));
        assertSame("json empty string", JsonValue.NULL, converter().convertChild(xml("emptyFoo.xml", "json"), eval));
        assertSame("json blank string", JsonValue.NULL, converter().convertChild(xml("blankFoo.xml", "json"), eval));
        assertEquals("json object", key("key", "value").get(), converter().convertChild(xml("fooObject.xml", "json"), eval));
        assertEquals("json array", arr("one", "two").get(), converter().convertChild(xml("fooArray.xml", "json"), eval));
        assertEquals("json integer", wrap(-500), converter().convertChild(xml("fooNumber.xml", "json"), eval));
    }


    @Test(expected = ComponentConfigurationException.class)
    public void testConvertChild_jsonValueLiteral_stringToken() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        converter().convertChild(xml("fooBar.xml", "json"), eval);
    }

    @Test
    public void testConvertChild_objectLiteral() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("json object as object", key("key", "value").get(),
                converter().convertChild(xml("fooObject.xml", "jsonobject"), eval));
        assertEquals("json object as string", wrap("{\"key\": \"value\"}"),
                converter().convertChild(xml("fooObject.xml"), eval));
    }

    @Test
    public void testConvertChild_arrayLiteral() throws Exception {
        ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        assertEquals("json array as array", arr("one", "two").get(),
                converter().convertChild(xml("fooArray.xml", "jsonarray"), eval));
        assertEquals("json array as string", wrap("[\"one\", \"two\"]"),
                converter().convertChild(xml("fooArray.xml"), eval));
    }

    @Test
    public void testFromConfiguration() throws Exception {
        final ConverterLookup lookup = mock(ConverterLookup.class);
        final PlexusConfiguration config = xml("foosBar.xml");
        final Class<?> type = JsonObject.class;
        final Class<?> enclosingType = Object.class;
        final ClassLoader loader = getClass().getClassLoader();
        final ExpressionEvaluator eval = mock(ExpressionEvaluator.class);
        when(eval.evaluate(anyString())).thenAnswer(call -> call.getArgument(0, String.class));
        final ConfigurationListener listener = mock(ConfigurationListener.class);
        assertEquals("should convert config", key("bar", "one").get(),
                converter().fromConfiguration(lookup, config, type, enclosingType, loader, eval, listener));
    }
}
