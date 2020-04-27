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

import static net.adamcin.oakpal.api.Fun.*;
import static net.adamcin.oakpal.api.JavaxJson.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.*;
import javax.jcr.version.OnParentVersionAction;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonCollectors;

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.Result;
import org.apache.jackrabbit.commons.cnd.Lexer;
import org.apache.jackrabbit.spi.*;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QItemDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class JsonCndTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCndTest.class);

    private NamePathResolver resolver = new DefaultNamePathResolver(JsonCnd.BUILTIN_MAPPINGS);
    private static final String NS_FOO_PREFIX = "foo";
    private static final String NS_BAR_PREFIX = "bar";
    private static final String NS_FOO_URI = "http://foo.com";
    private static final String NS_BAR_URI = "http://bar.com";

    private static final List<JcrNs> ns = Arrays.asList(
            JcrNs.create(NS_BAR_PREFIX, NS_BAR_URI),
            JcrNs.create(NS_FOO_PREFIX, NS_FOO_URI)
    );

    private static NamespaceMapping getMapping() {
        return JsonCnd.toNamespaceMapping(ns);
    }

    private static final File baseDir = new File("src/test/resources/JsonCndTest");

    @Test
    public void testGetQNodeTypesFromJson() {
        List<QNodeTypeDefinition> nts = JsonCnd.getQTypesFromJson(
                key("nt:foo", key("attributes", arr("mixin"))).get(),
                JsonCnd.BUILTIN_MAPPINGS);

        assertEquals("size is ", 1, nts.size());
        assertEquals("name is", "foo", nts.get(0).getName().getLocalName());
        assertEquals("uri is", Name.NS_NT_URI, nts.get(0).getName().getNamespaceURI());
    }

    @Test
    public void testGetPrivilegesFromJson() {
        List<PrivilegeDefinition> oneDef = JsonCnd.getPrivilegesFromJson(key("nt:canDo", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true)
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS, arr("jcr:read", "jcr:write"))).get(),
                JsonCnd.BUILTIN_MAPPINGS);

        assertEquals("size is ", 1, oneDef.size());
        assertEquals("name is", "canDo", oneDef.get(0).getName().getLocalName());
        assertEquals("uri is", Name.NS_NT_URI, oneDef.get(0).getName().getNamespaceURI());

        List<PrivilegeDefinition> oneName = JsonCnd.getPrivilegesFromJson(arr("nt:canDo").get(),
                JsonCnd.BUILTIN_MAPPINGS);

        assertEquals("size is ", 1, oneName.size());
        assertEquals("name is", "canDo", oneName.get(0).getName().getLocalName());
        assertEquals("uri is", Name.NS_NT_URI, oneName.get(0).getName().getNamespaceURI());

        List<PrivilegeDefinition> jsonNull = JsonCnd.getPrivilegesFromJson(wrap(null), JsonCnd.BUILTIN_MAPPINGS);

        assertEquals("size is ", 0, jsonNull.size());

        List<PrivilegeDefinition> defs = JsonCnd.getPrivilegesFromJson(obj()
                .key("jcr:canDo", obj())
                .key("jcr:willDo", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, false))
                .key("jcr:wouldDoBut", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true))
                .key("jcr:wontDo", null)
                .key("jcr:none", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS, arr()))
                .key("jcr:rw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, false)
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:addChildNodes")))
                .key("jcr:mightRw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true)
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:removeNode")))
                .get(), getMapping());

        final Map<String, PrivilegeDefinition> defMap = defs.stream()
                .collect(Collectors.toMap(compose1(PrivilegeDefinition::getName, uncheck1(resolver::getJCRName)),
                        def -> def));
        final PrivilegeDefinition canDo = defMap.get("jcr:canDo");
        assertNotNull("canDo not null", canDo);
        assertFalse("canDo not abstract", canDo.isAbstract());
        assertEquals("canDo contains nothing", Collections.emptySet(), canDo.getDeclaredAggregateNames());
        final PrivilegeDefinition willDo = defMap.get("jcr:willDo");
        assertNotNull("willDo not null", willDo);
        assertFalse("willDo not abstract", willDo.isAbstract());
        assertEquals("willDo contains nothing", Collections.emptySet(), willDo.getDeclaredAggregateNames());
        final PrivilegeDefinition wouldDoBut = defMap.get("jcr:wouldDoBut");
        assertNotNull("wouldDoBut not null", wouldDoBut);
        assertTrue("wouldDoBut is abstract", wouldDoBut.isAbstract());
        assertEquals("wouldDoBut contains nothing", Collections.emptySet(), wouldDoBut.getDeclaredAggregateNames());
        final PrivilegeDefinition wontDo = defMap.get("jcr:wontDo");
        assertNull("wontDo is null", wontDo);
        final PrivilegeDefinition none = defMap.get("jcr:none");
        assertNotNull("none not null", none);
        assertFalse("none not abstract", none.isAbstract());
        assertEquals("none contains nothing", Collections.emptySet(), none.getDeclaredAggregateNames());
        final PrivilegeDefinition rw = defMap.get("jcr:rw");
        assertNotNull("rw not null", rw);
        assertFalse("rw not abstract", rw.isAbstract());
        assertEquals("rw contains nothing", Stream.of("jcr:read", "jcr:write", "jcr:addChildNodes")
                        .map(uncheck1(resolver::getQName)).collect(Collectors.toSet()),
                rw.getDeclaredAggregateNames());
        final PrivilegeDefinition mightRw = defMap.get("jcr:mightRw");
        assertNotNull("mightRw not null", mightRw);
        assertTrue("mightRw is abstract", mightRw.isAbstract());
        assertEquals("mightRw contains nothing", Stream.of("jcr:read", "jcr:write", "jcr:removeNode")
                .map(uncheck1(resolver::getQName)).collect(Collectors.toSet()), mightRw.getDeclaredAggregateNames());
    }

    @Test
    public void testToJson() {
        assertEquals("empty list to empty object", 0,
                JsonCnd.toJson(Collections.emptyList(), getMapping()).size());
        JsonObject qJson = key("nt:foo", key("@", arr("mixin"))).get();
        List<QNodeTypeDefinition> nts = JsonCnd.getQTypesFromJson(qJson, JsonCnd.BUILTIN_MAPPINGS);

        JsonObject toJson = JsonCnd.toJson(nts, getMapping());
        assertEquals("json meets json", qJson, toJson);
    }

    @Test
    public void testPrivilegesToJson() {
        List<PrivilegeDefinition> arrDefs =
                JsonCnd.getPrivilegesFromJson(arr("jcr:read", "jcr:write").get(), getMapping());
        assertEquals("arr defs are", arr("jcr:read", "jcr:write").get(),
                JsonCnd.privilegesToJson(arrDefs, getMapping()));

        List<PrivilegeDefinition> objDefs = JsonCnd.getPrivilegesFromJson(obj()
                .key("jcr:canDo", obj())
                .key("jcr:willDo", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, false))
                .key("jcr:wouldDoBut", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true))
                .key("jcr:wontDo", null)
                .key("jcr:none", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS, arr()))
                .key("jcr:rw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:addChildNodes"))
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, false))
                .key("jcr:mightRw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:removeNode"))
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true))
                .get(), getMapping());

        JsonObject expectJson = obj()
                .key("jcr:canDo", obj())
                .key("jcr:mightRw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true)
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:removeNode")))
                .key("jcr:none", obj())
                .key("jcr:rw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:addChildNodes")))
                .key("jcr:willDo", obj())
                .key("jcr:wouldDoBut", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true))
                .get();

        assertEquals("obj defs are", expectJson, JsonCnd.privilegesToJson(objDefs, getMapping()));

    }

    @Test
    public void testToJcrNsList() {
        List<JcrNs> fromCnd = JsonCnd.toJcrNsList(getMapping(), new NamespaceMappingRequest.Builder()
                .withRetainPrefix(NS_BAR_PREFIX)
                .withRetainPrefix(NS_FOO_PREFIX)
                .build());
        assertEquals("ns meet ns", ns, fromCnd);
    }

    @Test
    public void testFunction_adaptToQ() throws Exception {
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            final NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            final Function<NodeTypeDefinition, QNodeTypeDefinition> fAdapter = JsonCnd.adaptToQ(session);
            final NodeType folderType = manager.getNodeType("nt:folder");
            final QNodeTypeDefinition folderQType = fAdapter.apply(folderType);
            assertEquals("name is", "folder", folderQType.getName().getLocalName());
            assertEquals("uri is", Name.NS_NT_URI, folderQType.getName().getNamespaceURI());
        });
    }

    @Test
    public void testNamedBy_qNodeTypes() throws Exception {
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            final NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            final Function<NodeTypeDefinition, QNodeTypeDefinition> fAdapter = JsonCnd.adaptToQ(session);
            final NodeType folderType = manager.getNodeType("nt:folder");
            final QNodeTypeDefinition folderQType = fAdapter.apply(folderType);
            List<Name> deps = JsonCnd.namedBy(folderQType).collect(Collectors.toList());
            final Name folderName = deps.get(0);
            assertEquals("name is", "folder", folderName.getLocalName());
            assertEquals("uri is", Name.NS_NT_URI, folderName.getNamespaceURI());
            final Name hierName = deps.get(1);
            assertEquals("name is", "hierarchyNode", hierName.getLocalName());
            assertEquals("uri is", Name.NS_NT_URI, hierName.getNamespaceURI());
        });
    }

    @Test
    public void testNamedBy_privileges() {
        List<PrivilegeDefinition> defs = JsonCnd.getPrivilegesFromJson(obj()
                .key("jcr:canDo", obj())
                .key("jcr:willDo", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, false))
                .key("jcr:wouldDoBut", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true))
                .key("jcr:none", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS, arr()))
                .key("jcr:rw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:addChildNodes")))
                .key("jcr:mightRw", obj()
                        .key(JsonCnd.PRIVILEGE_KEY_ABSTRACT, true)
                        .key(JsonCnd.PRIVILEGE_KEY_CONTAINS,
                                arr("jcr:read", "jcr:write", "jcr:removeNode")))
                .get(), getMapping());

        final List<String> jcrNames = Arrays.asList("jcr:canDo", "jcr:willDo", "jcr:wouldDoBut", "jcr:none", "jcr:rw",
                "jcr:mightRw", "jcr:read", "jcr:write", "jcr:addChildNodes", "jcr:removeNode");

        final Set<Name> expectNamedBy = jcrNames.stream().map(uncheck1(resolver::getQName)).collect(Collectors.toSet());
        final Set<Name> actualNamedBy = defs.stream().flatMap(JsonCnd::namedBy).collect(Collectors.toSet());
        assertEquals("expect namedBy", expectNamedBy, actualNamedBy);
    }

    @Test
    public void testJcrNameOrResidual() throws Exception {
        final Fun.ThrowingFunction<Name, String> func = JsonCnd.jcrNameOrResidual(resolver);
        assertEquals("* equals *", JsonCnd.TOKEN_RESIDUAL, func.tryApply(JsonCnd.QNAME_RESIDUAL));
        final Name ntFolder = NameFactoryImpl.getInstance().create(Name.NS_NT_URI, "folder");
        assertEquals("nt:folder equals nt:folder", "nt:folder", func.tryApply(ntFolder));
    }

    @Test
    public void testQNameOrResidual() throws Exception {
        final Fun.ThrowingFunction<String, Name> func = JsonCnd.qNameOrResidual(resolver);
        assertEquals("* equals *", JsonCnd.QNAME_RESIDUAL, func.tryApply(JsonCnd.TOKEN_RESIDUAL));
        final Name ntFolder = NameFactoryImpl.getInstance().create(Name.NS_NT_URI, "folder");
        assertEquals("nt:folder equals nt:folder", ntFolder, func.tryApply("nt:folder"));
    }

    @Test
    public void testComparatorPushResiduals() {
        final String[] names = new String[]{"c", "*", "b", "a"};
        final String[] expect = new String[]{"a", "b", "c", "*"};
        Arrays.sort(names, JsonCnd.COMPARATOR_PUSH_RESIDUALS);
        assertArrayEquals("push residuals", expect, names);
    }

    @Test
    public void testQValueString() throws Exception {
        final Name ntFoo = NameFactoryImpl.getInstance().create(Name.NS_NT_URI, "foo");
        final QValue fooValue = QValueFactoryImpl.getInstance().create(ntFoo);
        assertEquals("fooValue name is name", "nt:foo", JsonCnd.qValueString(fooValue, resolver));
        final Path pathFoo = PathFactoryImpl.getInstance().create(new Path.Element[]{
                PathFactoryImpl.getInstance().getRootElement(),
                PathFactoryImpl.getInstance().createElement(ntFoo),
                PathFactoryImpl.getInstance().createElement(ntFoo)
        });

        final QValue fooPathValue = QValueFactoryImpl.getInstance().create(pathFoo);
        assertEquals("path equals path", "/nt:foo/nt:foo", JsonCnd.qValueString(fooPathValue, resolver));

        final QValue stringValue = QValueFactoryImpl.getInstance().create("some string", PropertyType.STRING);
        assertEquals("string equals string", "some string", JsonCnd.qValueString(stringValue, resolver));
    }

    @Test
    public void testStreamNsPrefix() {
        assertEquals("empty string means empty stream",
                0, JsonCnd.streamNsPrefix("").count());
        assertEquals("colonless string means empty stream",
                0, JsonCnd.streamNsPrefix("foo").count());
        assertEquals("colon string means single prefix",
                1, JsonCnd.streamNsPrefix("nt:foo").count());
        assertEquals("colon string means captured prefix",
                "nt", JsonCnd.streamNsPrefix("nt:foo").findFirst().orElse(""));
        assertEquals("double-colon string still means single prefix",
                1, JsonCnd.streamNsPrefix("nt:foo:bar").count());
        assertEquals("double-colon string still means captured prefix",
                "nt", JsonCnd.streamNsPrefix("nt:foo:bar").findFirst().orElse(""));
    }

    @Test
    public void testReadNodeTypes() {
        List<URL> cnds = Stream.of("a", "b", "c", "d", "e", "f")
                .map(letter -> new File(baseDir, letter + ".cnd"))
                .map(compose1(File::toURI, uncheck1(URI::toURL)))
                .collect(Collectors.toList());

        final List<Result<NodeTypeSet>> sets = JsonCnd.readNodeTypes(getMapping(), cnds);
        assertEquals("sets should have n sets", cnds.size(), sets.size());
        assertTrue("all are successful", sets.stream().allMatch(Result::isSuccess));
        assertTrue("all are successful", sets.stream()
                .map(result -> result
                        .map(compose1(NodeTypeSet::getNodeTypes, Map::size))
                        .getOrDefault(0))
                .allMatch(size -> size == 2));

        List<URL> cndsWithError = Stream.of("a", "b", "c", "error", "d", "e", "f")
                .map(letter -> new File(baseDir, letter + ".cnd"))
                .map(compose1(File::toURI, uncheck1(URI::toURL)))
                .collect(Collectors.toList());

        final List<Result<NodeTypeSet>> setsWithError = JsonCnd.readNodeTypes(getMapping(), cndsWithError);
        assertEquals("setsWithError should have n sets", cndsWithError.size(), setsWithError.size());
        assertFalse("not all are successful", setsWithError.stream().allMatch(Result::isSuccess));
        assertEquals("setsWithError should have n successful sets", cndsWithError.size() - 1,
                setsWithError.stream().filter(Result::isSuccess).count());
    }

    @Test
    public void testAggregateNodeTypes() {
        final List<URL> cnds = Stream.of("a", "bb", "b", "c", "d", "e", "f")
                .map(letter -> new File(baseDir, letter + ".cnd"))
                .map(compose1(File::toURI, uncheck1(URI::toURL)))
                .collect(Collectors.toList());

        final List<Result<NodeTypeSet>> setsWithError = JsonCnd.readNodeTypes(getMapping(), cnds);
        final NodeTypeSet aggregate = JsonCnd.aggregateNodeTypes(getMapping(), setsWithError.stream()
                .collect(Result.tryCollect(Collectors.toList()))
                .getOrDefault(Collections.emptyList()));

        final Name bPrimaryType = NameFactoryImpl.getInstance()
                .create("http://b.com/1.0", "primaryType");
        final Name bProperty = NameFactoryImpl.getInstance()
                .create("http://b.com/1.0", "property");
        assertTrue("expect b:primaryType",
                aggregate.getNodeTypes().containsKey(bPrimaryType));
        assertTrue("b:primaryType should define b:property",
                Stream.of(aggregate.getNodeTypes().get(bPrimaryType).getPropertyDefs())
                        .anyMatch(def -> bProperty.equals(def.getName())));
    }

    interface SomeDefinition {
        List<String> getAttributeValues();
    }

    interface WhateverKeyDefToken extends JsonCnd.KeyDefinitionToken<Map<String, String>, SomeDefinition> {

        @Override
        default String getToken() {
            return getLexTokens()[0];
        }

        @Override
        default void readTo(NamePathResolver resolver, Map<String, String> builder, JsonValue value) {
            builder.put(JSON_VALUE_STRING.apply(value), getToken());
        }

        @Override
        default JsonValue writeJson(SomeDefinition def, NamePathResolver resolver) {
            if (def.getAttributeValues().contains(getToken())) {
                return JavaxJson.wrap(getLexTokens());
            } else {
                return JsonValue.EMPTY_JSON_ARRAY;
            }
        }
    }

    @Test
    public void testInternalReadAllTo() {
        final JsonObject parentValue = obj()
                .key("a", "alphaVal")
                .key("bravo", "bravoVal")
                .key("?", "unknownVal")
                .get();
        final Map<String, String> builder = new HashMap<>();
        final WhateverKeyDefToken alphaToken = mock(WhateverKeyDefToken.class);
        when(alphaToken.getLexTokens()).thenReturn(new String[]{"a"});
        final WhateverKeyDefToken bravoToken = mock(WhateverKeyDefToken.class);
        when(bravoToken.getLexTokens()).thenReturn(new String[]{"b", "bravo"});
        final WhateverKeyDefToken unknownToken = mock(WhateverKeyDefToken.class);
        when(unknownToken.getLexTokens()).thenReturn(new String[]{"?"});
        final WhateverKeyDefToken[] tokens = new WhateverKeyDefToken[]{alphaToken, bravoToken, unknownToken};

        for (WhateverKeyDefToken token : tokens) {
            doCallRealMethod().when(token).nonUnknown();
            doCallRealMethod().when(token).getToken();
            doCallRealMethod().when(token).readTo(any(NamePathResolver.class), any(Map.class), any(JsonValue.class));
        }

        JsonCnd.internalReadAllTo(resolver, builder, parentValue, tokens);

        assertTrue("contains alphaVal key", builder.containsKey("alphaVal"));
        assertEquals("associated alpha token", "a", builder.get("alphaVal"));
        assertTrue("contains bravoVal key", builder.containsKey("bravoVal"));
        assertEquals("associated bravo token", "b", builder.get("bravoVal"));
        assertFalse("not contains unknownVal key", builder.containsKey("unknownVal"));
    }

    @Test
    public void testInternalWriteAllJson() {
        final SomeDefinition def = mock(SomeDefinition.class);
        when(def.getAttributeValues()).thenReturn(Arrays.asList("a", "b", "c", "?"));
        final WhateverKeyDefToken alphaToken = mock(WhateverKeyDefToken.class);
        when(alphaToken.getLexTokens()).thenReturn(new String[]{"a", "alpha"});
        final WhateverKeyDefToken bravoToken = mock(WhateverKeyDefToken.class);
        when(bravoToken.getLexTokens()).thenReturn(new String[]{"b", "bravo"});
        final WhateverKeyDefToken unknownToken = mock(WhateverKeyDefToken.class);
        when(unknownToken.getLexTokens()).thenReturn(new String[]{"?", "unknown"});

        final WhateverKeyDefToken[] tokens = new WhateverKeyDefToken[]{alphaToken, bravoToken, unknownToken};

        for (WhateverKeyDefToken token : tokens) {
            doCallRealMethod().when(token).nonUnknown();
            doCallRealMethod().when(token).getToken();
            doCallRealMethod().when(token).writeJson(any(SomeDefinition.class), any(NamePathResolver.class));
        }

        final JsonValue parentValue = JsonCnd.internalWriteAllJson(def, resolver, tokens);
        assertSame("parentValue is JsonObject", parentValue.getValueType(), JsonValue.ValueType.OBJECT);

        final JsonObject parentObject = parentValue.asJsonObject();
        assertEquals("alpha key is present and is equal to [a, alpha]",
                Json.createArrayBuilder().add("a").add("alpha").build(), parentObject.get("a"));
        assertEquals("bravo key is present and is equal to [b, bravo]",
                Json.createArrayBuilder().add("b").add("bravo").build(), parentObject.get("b"));
        assertFalse("charlie key is not present", parentObject.containsKey("c"));
        assertFalse("unknown key is not present", parentObject.containsKey("?"));
    }

    @Test
    public void test_NodeTypeDefinitionKey_EXTENDS() {
        final JsonCnd.NodeTypeDefinitionKey key = JsonCnd.NodeTypeDefinitionKey.EXTENDS;

        assertEquals("getToken should return", ">", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{">", "extends"}, key.getLexTokens());

        final QNodeTypeDefinitionBuilder builder = mock(QNodeTypeDefinitionBuilder.class);
        final QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);

        final Name[] expect = Stream.of("nt:base", "nt:folder")
                .map(uncheck1(resolver::getQName)).toArray(Name[]::new);
        when(def.getSupertypes()).thenReturn(expect);

        assertEquals("supertypes should be",
                arr("nt:base", "nt:folder").get(), key.writeJson(def, resolver));

        final CompletableFuture<Name[]> result = new CompletableFuture<>();
        doAnswer(call -> result.complete(call.getArgument(0))).when(builder).setSupertypes(nullable(Name[].class));

        final JsonArray input = arr("nt:base", "nt:folder").get();
        key.readTo(resolver, builder, input);
        assertArrayEquals("expect supertypes", expect, result.getNow(new Name[0]));
    }

    @Test
    public void test_NodeTypeDefinitionKey_ATTRIBUTES() {
        final JsonCnd.NodeTypeDefinitionKey key = JsonCnd.NodeTypeDefinitionKey.ATTRIBUTES;

        assertEquals("getToken should return", "@", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"@", "attributes"}, key.getLexTokens());

        final QNodeTypeDefinitionBuilder builder = mock(QNodeTypeDefinitionBuilder.class);
        final QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);

        when(def.isAbstract()).thenReturn(false);
        when(def.isMixin()).thenReturn(true);
        when(def.hasOrderableChildNodes()).thenReturn(false);
        when(def.isQueryable()).thenReturn(false);

        final JsonArray expect = arr("mixin", "noquery").get();
        assertEquals("attributes should be", expect, key.writeJson(def, resolver));

        final CompletableFuture<Boolean> prefMixin = new CompletableFuture<>();
        final CompletableFuture<Boolean> prefQueryable = new CompletableFuture<>();

        doAnswer(call -> prefMixin.complete(call.getArgument(0))).when(builder).setMixin(anyBoolean());
        doAnswer(call -> prefQueryable.complete(call.getArgument(0))).when(builder).setQueryable(anyBoolean());

        key.readTo(resolver, builder, expect);
        assertTrue("is mixin", prefMixin.getNow(Boolean.FALSE));
        assertFalse("not queryable", prefQueryable.getNow(Boolean.TRUE));
    }

    @Test
    public void test_NodeTypeDefinitionKey_PRIMARYITEM() throws Exception {
        final JsonCnd.NodeTypeDefinitionKey key = JsonCnd.NodeTypeDefinitionKey.PRIMARYITEM;

        assertEquals("getToken should return", "primaryitem", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"primaryitem", "!"}, key.getLexTokens());

        final QNodeTypeDefinitionBuilder builder = mock(QNodeTypeDefinitionBuilder.class);
        final QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);

        final Name expect = resolver.getQName("jcr:content");
        when(def.getPrimaryItemName()).thenReturn(expect);

        assertEquals("primary item should be",
                wrap("jcr:content"), key.writeJson(def, resolver));

        final CompletableFuture<Name> result = new CompletableFuture<>();
        doAnswer(call -> result.complete(call.getArgument(0))).when(builder).setPrimaryItemName(nullable(Name.class));

        final JsonValue input = JavaxJson.val("jcr:content").get();
        key.readTo(resolver, builder, input);
        assertEquals("expect primary item name", expect, result.getNow(null));
    }

    @Test
    public void test_NodeTypeDefinitionKey_PROPERTY_DEFINITION() throws Exception {
        final JsonCnd.NodeTypeDefinitionKey key = JsonCnd.NodeTypeDefinitionKey.PROPERTY_DEFINITION;

        assertEquals("getToken should return", "-", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"-", "properties"}, key.getLexTokens());

        final Name parentName = resolver.getQName("nt:FooBar");
        final QNodeTypeDefinitionBuilder builder = mock(QNodeTypeDefinitionBuilder.class);
        when(builder.getName()).thenReturn(parentName);
        final QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);

        final Name fooName = resolver.getQName("nt:foo");
        final Name barName = resolver.getQName("nt:bar");

        final QPropertyDefinitionBuilder bPropFoo = new QPropertyDefinitionBuilder();
        bPropFoo.setName(fooName);
        bPropFoo.setDeclaringNodeType(parentName);
        final QPropertyDefinition propFoo = bPropFoo.build();

        final QPropertyDefinitionBuilder bPropBar = new QPropertyDefinitionBuilder();
        bPropBar.setName(barName);
        bPropBar.setDeclaringNodeType(parentName);
        final QPropertyDefinition propBar = bPropBar.build();

        final QPropertyDefinitionBuilder bPropAny = new QPropertyDefinitionBuilder();
        bPropAny.setName(JsonCnd.QNAME_RESIDUAL);
        bPropAny.setDeclaringNodeType(parentName);
        final QPropertyDefinition propAny = bPropAny.build();

        final QPropertyDefinitionBuilder bPropAnyMulti = new QPropertyDefinitionBuilder();
        bPropAnyMulti.setName(JsonCnd.QNAME_RESIDUAL);
        bPropAnyMulti.setMultiple(true);
        bPropAnyMulti.setDeclaringNodeType(parentName);
        final QPropertyDefinition propAnyMulti = bPropAnyMulti.build();

        final QPropertyDefinition[] expectUnsorted = new QPropertyDefinition[]{propFoo, propBar, propAnyMulti, propAny};
        when(def.getPropertyDefs()).thenReturn(expectUnsorted);

        final JsonArray expectJson = Stream.of(propBar, propFoo, propAny, propAnyMulti)
                .map(propDef -> JsonCnd.PropertyDefinitionKey.writeAllJson(propDef, resolver))
                .collect(JsonCollectors.toJsonArray());

        assertEquals("property defs should be", expectJson, key.writeJson(def, resolver));

        final CompletableFuture<QPropertyDefinition[]> result = new CompletableFuture<>();
        doAnswer(call -> result.complete(call.getArgument(0))).when(builder)
                .setPropertyDefs(nullable(QPropertyDefinition[].class));

        key.readTo(resolver, builder, expectJson);
        final QPropertyDefinition[] expect = new QPropertyDefinition[]{propBar, propFoo, propAny, propAnyMulti};
        final QPropertyDefinition[] actual = result.getNow(null);
        assertArrayEquals("expect property defs", expect, actual);

    }

    @Test
    public void test_NodeTypeDefinitionKey_CHILD_NODE_DEFINITION() throws Exception {
        final JsonCnd.NodeTypeDefinitionKey key = JsonCnd.NodeTypeDefinitionKey.CHILD_NODE_DEFINITION;

        assertEquals("getToken should return", "+", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"+", "childNodes"}, key.getLexTokens());

        final Name parentName = resolver.getQName("nt:FooBar");
        final QNodeTypeDefinitionBuilder builder = mock(QNodeTypeDefinitionBuilder.class);
        when(builder.getName()).thenReturn(parentName);
        final QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);

        final Name fooName = resolver.getQName("nt:foo");
        final Name barName = resolver.getQName("nt:bar");

        final QNodeDefinitionBuilder bPropFoo = new QNodeDefinitionBuilder();
        bPropFoo.setName(fooName);
        bPropFoo.setDeclaringNodeType(parentName);
        final QNodeDefinition propFoo = bPropFoo.build();

        final QNodeDefinitionBuilder bPropAny = new QNodeDefinitionBuilder();
        bPropAny.setName(JsonCnd.QNAME_RESIDUAL);
        bPropAny.setDeclaringNodeType(parentName);
        final QNodeDefinition propAny = bPropAny.build();

        final QNodeDefinitionBuilder bPropBar = new QNodeDefinitionBuilder();
        bPropBar.setName(barName);
        bPropBar.setDeclaringNodeType(parentName);
        final QNodeDefinition propBar = bPropBar.build();

        final QNodeDefinition[] expectUnsorted = new QNodeDefinition[]{propFoo, propAny, propBar};
        when(def.getChildNodeDefs()).thenReturn(expectUnsorted);

        final JsonArray expectJson = Stream.of(propBar, propFoo, propAny)
                .map(propDef -> JsonCnd.ChildNodeDefinitionKey.writeAllJson(propDef, resolver))
                .collect(JsonCollectors.toJsonArray());

        assertEquals("child node defs should be", expectJson, key.writeJson(def, resolver));

        final CompletableFuture<QNodeDefinition[]> result = new CompletableFuture<>();
        doAnswer(call -> result.complete(call.getArgument(0))).when(builder)
                .setChildNodeDefs(nullable(QNodeDefinition[].class));

        key.readTo(resolver, builder, expectJson);
        final QNodeDefinition[] expect = new QNodeDefinition[]{propBar, propFoo, propAny};
        final QNodeDefinition[] actual = result.getNow(null);
        assertArrayEquals("expect property defs", expect, actual);
    }

    @Test
    public void test_NodeTypeDefinitionKey_readAllTo() throws Exception {
        final JsonObject value = obj()
                .key("extends", arr("nt:base", "nt:folder"))
                .key("attributes", arr("ord", "a"))
                .key("primaryitem", "jcr:content")
                .key("properties", arr()
                        .val(obj().key("name", "nt:foo")))
                .key("childNodes", arr()
                        .val(obj().key("name", "jcr:content")))
                .get();

        final Name parentName = resolver.getQName("nt:FooBar");
        final QNodeTypeDefinitionBuilder builder = new QNodeTypeDefinitionBuilder();
        builder.setName(parentName);

        JsonCnd.NodeTypeDefinitionKey.readAllTo(resolver, builder, value);

        assertEquals("primaryitem should be", resolver.getQName("jcr:content"), builder.getPrimaryItemName());
        assertArrayEquals("supertypes should be",
                new Name[]{resolver.getQName("nt:base"), resolver.getQName("nt:folder")},
                builder.getSuperTypes());
        assertTrue("is orderable", builder.hasOrderableChildNodes());
        assertTrue("is abstract", builder.isAbstract());
        assertEquals("builder property defs has", 1, builder.getPropertyDefs().length);
        assertEquals("property def name is", resolver.getQName("nt:foo"), builder.getPropertyDefs()[0].getName());
        assertEquals("property def declaring type is", parentName, builder.getPropertyDefs()[0].getDeclaringNodeType());
        assertEquals("builder ChildNode defs has", 1, builder.getChildNodeDefs().length);
        assertEquals("ChildNode def name is", resolver.getQName("jcr:content"), builder.getChildNodeDefs()[0].getName());
        assertEquals("ChildNode def declaring type is", parentName, builder.getChildNodeDefs()[0].getDeclaringNodeType());
    }

    @Test
    public void test_NodeTypeDefinitionKey_writeAllJson() throws Exception {

        final JsonObject expected = obj()
                .key(">", arr("nt:base", "nt:folder"))
                .key("@", arr("abstract", "orderable"))
                .key("primaryitem", "jcr:content")
                .key("-", arr()
                        .val(obj().key("name", "nt:foo").key("type", PropertyType.TYPENAME_UNDEFINED)))
                .key("+", arr()
                        .val(obj().key("name", "jcr:content").key("types", arr("nt:base"))))
                .get();

        final Name parentName = resolver.getQName("nt:FooBar");
        final QNodeTypeDefinitionBuilder builder = new QNodeTypeDefinitionBuilder();
        builder.setName(parentName);
        builder.setSupertypes(new Name[]{resolver.getQName("nt:base"), resolver.getQName("nt:folder")});
        builder.setOrderableChildNodes(true);
        builder.setAbstract(true);
        builder.setPrimaryItemName(resolver.getQName("jcr:content"));

        final QPropertyDefinitionBuilder pBuilder = new QPropertyDefinitionBuilder();
        pBuilder.setName(resolver.getQName("nt:foo"));
        pBuilder.setDeclaringNodeType(parentName);
        builder.setPropertyDefs(new QPropertyDefinition[]{pBuilder.build()});

        final QNodeDefinitionBuilder nBuilder = new QNodeDefinitionBuilder();
        nBuilder.setName(resolver.getQName("jcr:content"));
        nBuilder.setDeclaringNodeType(parentName);
        builder.setChildNodeDefs(new QNodeDefinition[]{nBuilder.build()});

        assertEquals("should be same json value", expected,
                JsonCnd.NodeTypeDefinitionKey.writeAllJson(builder.build(), resolver));
    }


    static class AttributeTester<B, D, A extends JsonCnd.AttributeDefinitionToken<B, D>> {
        final @NotNull Supplier<? extends B> builderSupplier;
        final @NotNull Function<? super B, ? extends D> builderFinisher;
        final @NotNull Function<String, A> tokenLookup;
        final @NotNull A attr;
        final @NotNull String[] expectTokens;
        final @NotNull BiConsumer<? super B, Boolean> setter;
        final @NotNull Predicate<? super D> getter;
        final boolean inverseValue;

        private AttributeTester(@NotNull Supplier<? extends B> builderSupplier,
                                @NotNull Function<? super B, ? extends D> builderFinisher,
                                @NotNull Function<String, A> tokenLookup,
                                @NotNull A attr,
                                @NotNull String[] expectTokens,
                                @NotNull BiConsumer<? super B, Boolean> setter,
                                @NotNull Predicate<? super D> getter,
                                boolean inverseValue) {
            this.builderSupplier = builderSupplier;
            this.builderFinisher = builderFinisher;
            this.tokenLookup = tokenLookup;
            this.attr = attr;
            this.expectTokens = expectTokens;
            this.setter = setter;
            this.getter = getter;
            this.inverseValue = inverseValue;
        }

        void test() {
            assertEquals("attr token is", expectTokens[0], attr.getToken());
            assertArrayEquals("attr lex tokens are, for " + attr.getToken(), expectTokens, attr.getLexTokens());
            for (String token : expectTokens) {
                assertSame("expect accessible by token: " + token,
                        attr, tokenLookup.apply(token));
            }

            Function<Boolean, ? extends D> generator = value -> {
                final B builder = builderSupplier.get();
                setter.accept(builder, value);
                return builderFinisher.apply(builder);
            };

            final B flipBuilder = builderSupplier.get();
            final Predicate<? super B> finishAndGet = composeTest1(builderFinisher, getter);
            if (inverseValue) {
                assertFalse("should inverse generator true, for: " + attr.getToken(),
                        attr.isWritable(generator.apply(true)));
                assertTrue("should inverse generator false, for: " + attr.getToken(),
                        attr.isWritable(generator.apply(false)));
                assertTrue("default should be true, for: " + attr.getToken(), finishAndGet.test(flipBuilder));
                attr.readTo(flipBuilder);
                assertFalse("readTo reverses to false, for: " + attr.getToken(), finishAndGet.test(flipBuilder));
            } else {
                assertTrue("should match generator true, for: " + attr.getToken(),
                        attr.isWritable(generator.apply(true)));
                assertFalse("should match generator false, for: " + attr.getToken(),
                        attr.isWritable(generator.apply(false)));
                assertFalse("default should be false, for: " + attr.getToken(), finishAndGet.test(flipBuilder));
                attr.readTo(flipBuilder);
                assertTrue("readTo reverses to true, for: " + attr.getToken(), finishAndGet.test(flipBuilder));
            }
        }

        static class Builder<B, D, A extends JsonCnd.AttributeDefinitionToken<B, D>> {
            final @NotNull Supplier<B> builderSupplier;
            final @NotNull Function<B, D> builderFinisher;
            final @NotNull Function<String, A> tokenLookup;

            private Builder(@NotNull Supplier<B> builderSupplier,
                            @NotNull Function<B, D> builderFinisher,
                            @NotNull Function<String, A> tokenLookup) {
                this.builderSupplier = builderSupplier;
                this.builderFinisher = builderFinisher;
                this.tokenLookup = tokenLookup;
            }

            AttributeTester<B, D, A>
            testerFor(@NotNull A attr,
                      @NotNull String[] expectTokens,
                      @NotNull BiConsumer<? super B, Boolean> setter,
                      @NotNull Predicate<? super D> getter,
                      boolean invertValue) {
                return new AttributeTester<>(builderSupplier, builderFinisher, tokenLookup,
                        attr, expectTokens, setter, getter, invertValue);
            }
        }

        static <B, D, A extends JsonCnd.AttributeDefinitionToken<B, D>> AttributeTester.Builder<B, D, A>
        builder(final @NotNull Supplier<B> supplier,
                final @NotNull Function<B, D> finisher,
                final @NotNull Function<String, A> tokenLookup) {
            return new Builder<>(supplier, finisher, tokenLookup);
        }
    }

    @Test
    public void test_TypeDefinitionAttribute() throws Exception {
        AttributeTester.Builder<QNodeTypeDefinitionBuilder, QNodeTypeDefinition, JsonCnd.TypeDefinitionAttribute>
                testBuilder =
                AttributeTester.builder(QNodeTypeDefinitionBuilder::new,
                        QNodeTypeDefinitionBuilder::build,
                        JsonCnd.TypeDefinitionAttribute::forToken);

        testBuilder.testerFor(JsonCnd.TypeDefinitionAttribute.ABSTRACT, new String[]{"abstract", "abs", "a"},
                QNodeTypeDefinitionBuilder::setAbstract,
                QNodeTypeDefinition::isAbstract, false).test();

        testBuilder.testerFor(JsonCnd.TypeDefinitionAttribute.MIXIN, new String[]{"mixin", "mix", "m"},
                QNodeTypeDefinitionBuilder::setMixin,
                QNodeTypeDefinition::isMixin, false).test();

        testBuilder.testerFor(JsonCnd.TypeDefinitionAttribute.ORDERABLE, new String[]{"orderable", "ord", "o"},
                QNodeTypeDefinitionBuilder::setOrderableChildNodes,
                QNodeTypeDefinition::hasOrderableChildNodes, false).test();

        testBuilder.testerFor(JsonCnd.TypeDefinitionAttribute.NOQUERY, new String[]{"noquery", "nq"},
                QNodeTypeDefinitionBuilder::setQueryable,
                QNodeTypeDefinition::isQueryable, true).test();

        final JsonCnd.TypeDefinitionAttribute unknown = JsonCnd.TypeDefinitionAttribute.forToken("");
        assertSame("empty token should return unknown", JsonCnd.TypeDefinitionAttribute.UNKNOWN, unknown);

        // assume null safe
        unknown.readToBuilder.accept(null);
        assertFalse("never writable", unknown.checkWritable.test(null));

        assertEquals("unknown token is", "?", JsonCnd.TypeDefinitionAttribute.UNKNOWN.getToken());
        assertArrayEquals("unknown tokens are", new String[]{"?"}, JsonCnd.TypeDefinitionAttribute.UNKNOWN.getLexTokens());

        final QNodeTypeDefinitionBuilder builder = new QNodeTypeDefinitionBuilder();
        final JsonArray attrArray = arr("?", "nq", "m", "a", "o").get();
        JsonCnd.TypeDefinitionAttribute.readAttributes(builder, attrArray);
        final QNodeTypeDefinition def = builder.build();
        final List<String> writable = JsonCnd.TypeDefinitionAttribute.getAttributeTokens(def);
        assertEquals("writable attributes list", Stream.of(JsonCnd.TypeDefinitionAttribute.values())
                .filter(JsonCnd.TypeDefinitionAttribute::nonUnknown)
                .map(JsonCnd.TypeDefinitionAttribute::getToken)
                .collect(Collectors.toList()), writable);
    }

    static class ItemDefinitionAttributeTester<
            C extends QItemDefinitionBuilder,
            E extends QItemDefinition,
            A extends JsonCnd.AttributeDefinitionToken<C, E>> {

        final @NotNull Supplier<C> concreteSupplier;
        final @NotNull Function<C, E> concreteFinisher;
        final @NotNull Function<String, A> tokenLookup;
        final @NotNull A[] tokenValues;

        ItemDefinitionAttributeTester(@NotNull Supplier<C> concreteSupplier,
                                      @NotNull Function<C, E> concreteFinisher,
                                      @NotNull Function<String, A> tokenLookup,
                                      @NotNull A[] tokenValues) {
            this.concreteSupplier = concreteSupplier;
            this.concreteFinisher = concreteFinisher;
            this.tokenLookup = tokenLookup;
            this.tokenValues = tokenValues;
        }

        void test() throws Exception {
            AttributeTester.Builder<QItemDefinitionBuilder, QItemDefinition, JsonCnd.ItemDefinitionAttribute> testBuilder =
                    AttributeTester.builder(infer0(concreteSupplier::get),
                            compose1(builder -> (C) builder, concreteFinisher),
                            JsonCnd.ItemDefinitionAttribute::forToken);

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.MANDATORY, new String[]{"mandatory", "man", "m"},
                    QItemDefinitionBuilder::setMandatory,
                    QItemDefinition::isMandatory, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.AUTOCREATED, new String[]{"autocreated", "aut", "a"},
                    QItemDefinitionBuilder::setAutoCreated,
                    QItemDefinition::isAutoCreated, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.PROTECTED, new String[]{"protected", "pro", "p"},
                    QItemDefinitionBuilder::setProtected,
                    QItemDefinition::isProtected, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.VERSION, new String[]{"version"},
                    (builder, opv) -> {
                        if (opv) {
                            builder.setOnParentVersion(OnParentVersionAction.VERSION);
                        }
                    },
                    def -> def.getOnParentVersion() == OnParentVersionAction.VERSION, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.INITIALIZE, new String[]{"initialize"},
                    (builder, opv) -> {
                        if (opv) {
                            builder.setOnParentVersion(OnParentVersionAction.INITIALIZE);
                        }
                    },
                    def -> def.getOnParentVersion() == OnParentVersionAction.INITIALIZE, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.COMPUTE, new String[]{"compute"},
                    (builder, opv) -> {
                        if (opv) {
                            builder.setOnParentVersion(OnParentVersionAction.COMPUTE);
                        }
                    },
                    def -> def.getOnParentVersion() == OnParentVersionAction.COMPUTE, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.IGNORE, new String[]{"ignore"},
                    (builder, opv) -> {
                        if (opv) {
                            builder.setOnParentVersion(OnParentVersionAction.IGNORE);
                        }
                    },
                    def -> def.getOnParentVersion() == OnParentVersionAction.IGNORE, false).test();

            testBuilder.testerFor(JsonCnd.ItemDefinitionAttribute.ABORT, new String[]{"abort"},
                    (builder, opv) -> {
                        if (opv) {
                            builder.setOnParentVersion(OnParentVersionAction.ABORT);
                        }
                    },
                    def -> def.getOnParentVersion() == OnParentVersionAction.ABORT, false).test();


            final JsonCnd.ItemDefinitionAttribute unknown = JsonCnd.ItemDefinitionAttribute.forToken("");
            assertSame("empty token should return unknown", JsonCnd.ItemDefinitionAttribute.UNKNOWN, unknown);

            // assume null safe
            unknown.readToBuilder.accept(null);
            assertFalse("never writable", unknown.checkWritable.test(null));

            assertEquals("unknown token is", "?", JsonCnd.ItemDefinitionAttribute.UNKNOWN.getToken());
            assertArrayEquals("unknown tokens are", new String[]{"?"}, JsonCnd.ItemDefinitionAttribute.UNKNOWN.getLexTokens());

            final C builder = concreteSupplier.get();
            final JsonArray attrArray = arr("abort", "?", "initialize", "a", "version", "p", "ignore", "m", "compute").get();
            JsonCnd.ItemDefinitionAttribute.readAttributes(builder, attrArray, tokenLookup);
            final E def = concreteFinisher.apply(builder);
            final List<String> writable = JsonCnd.ItemDefinitionAttribute.getAttributeTokens(def);
            assertEquals("writable attributes list",
                    Arrays.asList("mandatory", "autocreated", "protected", "compute"), writable);

            assertEquals("base writable attributes for empty subtoken array", writable,
                    JsonCnd.ItemDefinitionAttribute.getMoreItemDefAttributeTokens(def, tokenValues));
        }
    }

    @Test
    public void test_ItemDefinitionAttribute_usingNodeDefBuilder() throws Exception {
        ItemDefinitionAttributeTester<QNodeDefinitionBuilder, QNodeDefinition, JsonCnd.ChildNodeDefinitionAttribute> tester =
                new ItemDefinitionAttributeTester<>(
                        QNodeDefinitionBuilder::new,
                        QNodeDefinitionBuilder::build,
                        JsonCnd.ChildNodeDefinitionAttribute::forToken,
                        JsonCnd.ChildNodeDefinitionAttribute.values());

        tester.test();
    }

    @Test
    public void test_ItemDefinitionAttribute_usingPropertyDefBuilder() throws Exception {
        ItemDefinitionAttributeTester<QPropertyDefinitionBuilder, QPropertyDefinition, JsonCnd.PropertyDefinitionAttribute> tester =
                new ItemDefinitionAttributeTester<>(
                        QPropertyDefinitionBuilder::new,
                        QPropertyDefinitionBuilder::build,
                        JsonCnd.PropertyDefinitionAttribute::forToken,
                        JsonCnd.PropertyDefinitionAttribute.values());
        tester.test();
    }

    @Test
    public void test_PropertyDefinitionKey_NAME() throws Exception {
        final JsonCnd.PropertyDefinitionKey key = JsonCnd.PropertyDefinitionKey.NAME;
        assertEquals("getToken should return", "name", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"name"}, key.getLexTokens());
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        key.readTo(resolver, builder, val("nt:foo").get());
        assertEquals("builder getName() foo", resolver.getQName("nt:foo"),
                builder.getName());
        assertEquals("def getName() toJson", val("nt:foo").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_PropertyDefinitionKey_REQUIREDTYPE() {
        final JsonCnd.PropertyDefinitionKey key = JsonCnd.PropertyDefinitionKey.REQUIREDTYPE;
        assertEquals("getToken should return", "type", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"type"}, key.getLexTokens());
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        key.readTo(resolver, builder, val(PropertyType.TYPENAME_LONG).get());
        assertEquals("builder getRequiredType() Long", PropertyType.LONG,
                builder.getRequiredType());
        assertEquals("def getRequiredType() Long toJson", val(PropertyType.TYPENAME_LONG).get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_PropertyDefinitionKey_ATTRIBUTES() {
        final JsonCnd.PropertyDefinitionKey key = JsonCnd.PropertyDefinitionKey.ATTRIBUTES;
        assertEquals("getToken should return", "@", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"@", "attributes"}, key.getLexTokens());
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        key.readTo(resolver, builder, arr("mandatory", "version", "noqueryorder", "multiple").get());
        assertFalse("builder getProtected()", builder.getAutoCreated());
        assertTrue("builder getMandatory()", builder.getMandatory());
        assertFalse("builder getQueryOrderable()", builder.getQueryOrderable());
        assertFalse("builder getProtected()", builder.getProtected());
        assertEquals("builder opv version", OnParentVersionAction.VERSION,
                builder.getOnParentVersion());
        assertEquals("def getRequiredType() toJson",
                arr("mandatory", "version", "multiple", "noqueryorder").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_PropertyDefinitionKey_QUERYOPS() {
        final JsonCnd.PropertyDefinitionKey key = JsonCnd.PropertyDefinitionKey.QUERYOPS;
        assertEquals("getToken should return", "queryops", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"queryops", "qop"}, key.getLexTokens());
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        final String[] expectOps = new String[]{
                Lexer.QUEROPS_EQUAL,
                Lexer.QUEROPS_GREATERTHAN,
                Lexer.QUEROPS_LIKE};
        key.readTo(resolver, builder, arr(
                Lexer.QUEROPS_LIKE,
                Lexer.QUEROPS_GREATERTHAN,
                Lexer.QUEROPS_LIKE,
                Lexer.QUEROPS_EQUAL).get());
        assertArrayEquals("builder getAvailableQueryOperators()", expectOps,
                builder.getAvailableQueryOperators());
        assertEquals("def getAvailableQueryOperators() toJson", val(expectOps).get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_PropertyDefinitionKey_DEFAULT() throws Exception {
        final JsonCnd.PropertyDefinitionKey key = JsonCnd.PropertyDefinitionKey.DEFAULT;
        assertEquals("getToken should return", "=", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"=", "default"}, key.getLexTokens());
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        builder.setRequiredType(PropertyType.STRING);
        key.readTo(resolver, builder, arr("one", "two", "three").get());
        assertArrayEquals("builder getDefaultValues() one two three", new QValue[]{
                        QValueFactoryImpl.getInstance().create("one", PropertyType.STRING),
                        QValueFactoryImpl.getInstance().create("two", PropertyType.STRING),
                        QValueFactoryImpl.getInstance().create("three", PropertyType.STRING)
                },
                builder.getDefaultValues());
        assertEquals("def getDefaultValues() String toJson",
                arr("one", "two", "three").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_PropertyDefinitionKey_CONSTRAINTS() {
        final JsonCnd.PropertyDefinitionKey key = JsonCnd.PropertyDefinitionKey.CONSTRAINTS;
        assertEquals("getToken should return", "<", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"<", "constraints"}, key.getLexTokens());
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        builder.setRequiredType(PropertyType.STRING);
        key.readTo(resolver, builder, arr("^.*$", "^[0-9]*$").get());
    }

    @Test
    public void test_PropertyDefinitionKey_roundTrip() {
        final JsonObject input = obj()
                .key("name", "nt:foo")
                .key("type", "String")
                .key("attributes", arr("a", "*", "nof"))
                .key("queryops", arr("LIKE", "=", ">", "="))
                .key("default", arr("one", "two", "three"))
                .key("constraints", arr("^.*$", "^[0-9]*$"))
                .get();
        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        JsonCnd.PropertyDefinitionKey.readAllTo(resolver, builder, input);

        final JsonObject expectJson = obj()
                .key("name", "nt:foo")
                .key("type", "String")
                .key("@", arr("autocreated", "multiple", "nofulltext"))
                .key("queryops", arr("=", ">", "LIKE"))
                .key("=", arr("one", "two", "three"))
                .key("<", arr("^.*$", "^[0-9]*$"))
                .get();

        assertEquals("expect json", expectJson,
                JsonCnd.PropertyDefinitionKey.writeAllJson(builder.build(), resolver));
    }

    @Test
    public void test_PropertyDefinitionAttribute() throws Exception {
        AttributeTester.Builder<QPropertyDefinitionBuilder, QPropertyDefinition, JsonCnd.PropertyDefinitionAttribute>
                testBuilder =
                AttributeTester.builder(QPropertyDefinitionBuilder::new,
                        QPropertyDefinitionBuilder::build,
                        JsonCnd.PropertyDefinitionAttribute::forToken);

        testBuilder.testerFor(JsonCnd.PropertyDefinitionAttribute.MULTIPLE, new String[]{"multiple", "mul", "*"},
                QPropertyDefinitionBuilder::setMultiple,
                QPropertyDefinition::isMultiple, false).test();

        testBuilder.testerFor(JsonCnd.PropertyDefinitionAttribute.NOFULLTEXT, new String[]{"nofulltext", "nof"},
                QPropertyDefinitionBuilder::setFullTextSearchable,
                QPropertyDefinition::isFullTextSearchable, true).test();

        testBuilder.testerFor(JsonCnd.PropertyDefinitionAttribute.NOQUERYORDER, new String[]{"noqueryorder", "nqord"},
                QPropertyDefinitionBuilder::setQueryOrderable,
                QPropertyDefinition::isQueryOrderable, true).test();

        final JsonCnd.PropertyDefinitionAttribute unknown = JsonCnd.PropertyDefinitionAttribute.forToken("");
        assertSame("empty token should return unknown", JsonCnd.PropertyDefinitionAttribute.UNKNOWN, unknown);

        // assume null safe
        unknown.readToBuilder.accept(null);
        assertFalse("never writable", unknown.checkWritable.test(null));

        assertEquals("unknown token is", "?", JsonCnd.PropertyDefinitionAttribute.UNKNOWN.getToken());
        assertArrayEquals("unknown tokens are", new String[]{"?"},
                JsonCnd.PropertyDefinitionAttribute.UNKNOWN.getLexTokens());

        final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();
        final JsonArray attrArray = arr("?", "nqord", "*", "nof").get();
        JsonCnd.PropertyDefinitionAttribute.readAttributes(builder, attrArray);
        final QPropertyDefinition def = builder.build();
        final List<String> writable = JsonCnd.PropertyDefinitionAttribute.getAttributeTokens(def);
        assertEquals("writable attributes list", Stream.of(JsonCnd.PropertyDefinitionAttribute.values())
                .filter(JsonCnd.PropertyDefinitionAttribute::nonUnknown)
                .map(JsonCnd.PropertyDefinitionAttribute::getToken)
                .collect(Collectors.toList()), writable);
    }

    @Test
    public void test_ChildNodeDefinitionKey_NAME() throws Exception {
        final JsonCnd.ChildNodeDefinitionKey key = JsonCnd.ChildNodeDefinitionKey.NAME;
        assertEquals("getToken should return", "name", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"name"}, key.getLexTokens());
        final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();
        key.readTo(resolver, builder, val("jcr:content").get());
        assertEquals("builder getName() foo", resolver.getQName("jcr:content"),
                builder.getName());
        assertEquals("def getName() toJson", val("jcr:content").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_ChildNodeDefinitionKey_REQUIREDTYPES() throws Exception {
        final JsonCnd.ChildNodeDefinitionKey key = JsonCnd.ChildNodeDefinitionKey.REQUIREDTYPES;
        assertEquals("getToken should return", "types", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"types"}, key.getLexTokens());
        final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();
        key.readTo(resolver, builder, arr("nt:base", "nt:folder").get());
        assertArrayEquals("builder getRequiredTypes() foo", new Name[]{
                        resolver.getQName("nt:folder"), resolver.getQName("nt:base")},
                builder.getRequiredPrimaryTypes());
        assertEquals("def getRequiredTypes() toJson", arr("nt:base", "nt:folder").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_ChildNodeDefinitionKey_DEFAULTTYPE() throws Exception {
        final JsonCnd.ChildNodeDefinitionKey key = JsonCnd.ChildNodeDefinitionKey.DEFAULTTYPE;
        assertEquals("getToken should return", "=", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"=", "defaultType"}, key.getLexTokens());
        final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();
        key.readTo(resolver, builder, val("nt:folder").get());
        assertEquals("builder getDefaultType() nt:folder",
                resolver.getQName("nt:folder"),
                builder.getDefaultPrimaryType());
        assertEquals("def getDefaultType() toJson", val("nt:folder").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_ChildNodeDefinitionKey_ATTRIBUTES() {
        final JsonCnd.ChildNodeDefinitionKey key = JsonCnd.ChildNodeDefinitionKey.ATTRIBUTES;
        assertEquals("getToken should return", "@", key.getToken());
        assertArrayEquals("getLexTokens should return", new String[]{"@", "attributes"}, key.getLexTokens());
        final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();
        key.readTo(resolver, builder, arr("mandatory", "version", "sns").get());
        assertFalse("builder getProtected()", builder.getAutoCreated());
        assertTrue("builder getMandatory()", builder.getMandatory());
        assertFalse("builder getProtected()", builder.getProtected());
        assertTrue("builder allowsSameNameSiblings", builder.getAllowsSameNameSiblings());
        assertEquals("builder opv version", OnParentVersionAction.VERSION,
                builder.getOnParentVersion());
        assertEquals("def attributes toJson",
                arr("mandatory", "version", "sns").get(),
                key.writeJson(builder.build(), resolver));
    }

    @Test
    public void test_ChildNodeDefinitionKey_roundTrip() {
        final JsonObject input = obj()
                .key("name", "jcr:content")
                .key("types", arr("nt:base", "nt:folder"))
                .key("defaultType", "nt:folder")
                .key("attributes", arr("a", "*", "m"))
                .get();

        final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();
        JsonCnd.ChildNodeDefinitionKey.readAllTo(resolver, builder, input);

        final JsonObject expectJson = obj()
                .key("name", "jcr:content")
                .key("types", arr("nt:base", "nt:folder"))
                .key("=", "nt:folder")
                .key("@", arr("mandatory", "autocreated", "sns"))
                .get();

        assertEquals("expect json", expectJson,
                JsonCnd.ChildNodeDefinitionKey.writeAllJson(builder.build(), resolver));
    }

    @Test
    public void test_ChildNodeDefinitionAttribute() throws Exception {
        AttributeTester.Builder<QNodeDefinitionBuilder, QNodeDefinition, JsonCnd.ChildNodeDefinitionAttribute>
                testBuilder =
                AttributeTester.builder(QNodeDefinitionBuilder::new,
                        QNodeDefinitionBuilder::build,
                        JsonCnd.ChildNodeDefinitionAttribute::forToken);

        testBuilder.testerFor(JsonCnd.ChildNodeDefinitionAttribute.SNS, new String[]{"sns", "*", "multiple"},
                QNodeDefinitionBuilder::setAllowsSameNameSiblings,
                QNodeDefinition::allowsSameNameSiblings, false).test();

        final JsonCnd.ChildNodeDefinitionAttribute unknown = JsonCnd.ChildNodeDefinitionAttribute.forToken("");
        assertSame("empty token should return unknown", JsonCnd.ChildNodeDefinitionAttribute.UNKNOWN, unknown);

        // assume null safe
        unknown.readToBuilder.accept(null);
        assertFalse("never writable", unknown.checkWritable.test(null));

        assertEquals("unknown token is", "?", JsonCnd.ChildNodeDefinitionAttribute.UNKNOWN.getToken());
        assertArrayEquals("unknown tokens are", new String[]{"?"},
                JsonCnd.ChildNodeDefinitionAttribute.UNKNOWN.getLexTokens());

        final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();
        final JsonArray attrArray = arr("?", "sns").get();
        JsonCnd.ChildNodeDefinitionAttribute.readAttributes(builder, attrArray);
        final QNodeDefinition def = builder.build();
        final List<String> writable = JsonCnd.ChildNodeDefinitionAttribute.getAttributeTokens(def);
        assertEquals("writable attributes list", Stream.of(JsonCnd.ChildNodeDefinitionAttribute.values())
                .filter(JsonCnd.ChildNodeDefinitionAttribute::nonUnknown)
                .map(JsonCnd.ChildNodeDefinitionAttribute::getToken)
                .collect(Collectors.toList()), writable);
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getName() throws Exception {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
        when(delegate.getName()).thenReturn("nt:foo");
        assertEquals("getName()", resolver.getQName("nt:foo"), adapter.getName());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getSupertypes() throws Exception {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
        assertArrayEquals("empty should match", new Name[0], adapter.getSupertypes());
        when(delegate.getDeclaredSupertypeNames()).thenReturn(new String[]{"nt:base", "nt:folder"});
        assertArrayEquals("populated should match",
                new Name[]{resolver.getQName("nt:base"), resolver.getQName("nt:folder")}, adapter.getSupertypes());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getSupportedMixinTypes() {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
        assertNull("should be null", adapter.getSupportedMixinTypes());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_isMixin() {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.isMixin());
        when(delegate.isMixin()).thenReturn(true);
        assertTrue("expect true", adapter.isMixin());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_isAbstract() {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.isAbstract());
        when(delegate.isAbstract()).thenReturn(true);
        assertTrue("expect true", adapter.isAbstract());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_isQueryable() {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.isQueryable());
        when(delegate.isQueryable()).thenReturn(true);
        assertTrue("expect true", adapter.isQueryable());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_hasOrderableChildNodes() {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.hasOrderableChildNodes());
        when(delegate.hasOrderableChildNodes()).thenReturn(true);
        assertTrue("expect true", adapter.hasOrderableChildNodes());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getPrimaryItemName() throws Exception {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
        assertNull("null to start", adapter.getPrimaryItemName());
        when(delegate.getPrimaryItemName()).thenReturn("jcr:content");
        assertEquals("now should match", resolver.getQName("jcr:content"), adapter.getPrimaryItemName());
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getPropertyDefs() throws Exception {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
        assertNotNull("expect nonnull to start", adapter.getPropertyDefs());
        assertEquals("expect empty to start", 0, adapter.getPropertyDefs().length);
        final PropertyDefinition property1 = mock(PropertyDefinition.class);
        when(property1.getName()).thenReturn("jcr:created");
        final PropertyDefinition property2 = mock(PropertyDefinition.class);
        when(property2.getName()).thenReturn("jcr:createdBy");
        when(delegate.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[]{property1, property2});

        final Name[] expectNames = new Name[]{resolver.getQName("jcr:created"), resolver.getQName("jcr:createdBy")};
        assertArrayEquals("expect names", expectNames,
                Stream.of(adapt(delegate).getPropertyDefs()).map(QPropertyDefinition::getName).toArray(Name[]::new));
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getChildNodeDefs() throws Exception {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final JsonCnd.NodeTypeDefinitionQAdapter adapter =
                new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
        assertNotNull("expect nonnull to start", adapter.getChildNodeDefs());
        assertEquals("expect empty to start", 0, adapter.getChildNodeDefs().length);

        final NodeDefinition childNode1 = mock(NodeDefinition.class);
        when(childNode1.getName()).thenReturn("jcr:content");
        final NodeDefinition childNode2 = mock(NodeDefinition.class);
        when(childNode2.getName()).thenReturn("jcr:activities");
        when(delegate.getDeclaredChildNodeDefinitions()).thenReturn(new NodeDefinition[]{childNode1, childNode2});

        final Name[] expectNames = new Name[]{resolver.getQName("jcr:content"), resolver.getQName("jcr:activities")};
        assertArrayEquals("expect names", expectNames,
                Stream.of(adapt(delegate).getChildNodeDefs()).map(QNodeDefinition::getName).toArray(Name[]::new));
    }

    private JsonCnd.NodeTypeDefinitionQAdapter adapt(final @NotNull NodeTypeDefinition delegate) {
        return new JsonCnd.NodeTypeDefinitionQAdapter(delegate, resolver);
    }

    @Test
    public void test_NodeTypeDefinitionQAdapter_getDependencies() throws Exception {
        final NodeTypeDefinition delegate = mock(NodeTypeDefinition.class);
        final Set<Name> dependencies = new LinkedHashSet<>();
        assertNotNull("not null to start", adapt(delegate).getDependencies());
        assertEquals("empty to start",
                dependencies, new LinkedHashSet<>(adapt(delegate).getDependencies()));
        final String[] expectSupertypes = new String[]{"nt:base", "nt:folder"};
        when(delegate.getName()).thenReturn("nt:foo");
        when(delegate.getDeclaredSupertypeNames()).thenReturn(expectSupertypes);
        dependencies.addAll(Stream.of(expectSupertypes)
                .map(uncheck1(resolver::getQName))
                .collect(Collectors.toSet()));
        assertEquals("expect with supertypes",
                dependencies, new LinkedHashSet<>(adapt(delegate).getDependencies()));


        final NodeDefinition childNode1 = mock(NodeDefinition.class);
        when(childNode1.getDefaultPrimaryTypeName()).thenReturn("nt:folder2");
        when(childNode1.getRequiredPrimaryTypeNames()).thenReturn(new String[]{"nt:base", "nt:folder"});
        final NodeDefinition childNode2 = mock(NodeDefinition.class);
        when(childNode2.getDefaultPrimaryTypeName()).thenReturn("nt:foo");
        when(childNode2.getRequiredPrimaryTypeNames()).thenReturn(new String[]{"nt:base", "nt:folder", "nt:foo"});
        when(delegate.getDeclaredChildNodeDefinitions()).thenReturn(new NodeDefinition[]{childNode1, childNode2});

        final String[] expectChildNodeDefNames = new String[]{"nt:folder2", "nt:base", "nt:folder"};
        dependencies.addAll(Stream.of(expectChildNodeDefNames)
                .map(uncheck1(resolver::getQName))
                .collect(Collectors.toSet()));
        assertEquals("expect with childNodeDefs",
                dependencies, new LinkedHashSet<>(adapt(delegate).getDependencies()));

        final String jcrRef = "jcr:referenced";
        final String jcrWeakRef = "jcr:weakReferenced";
        final PropertyDefinition property1 = mock(PropertyDefinition.class);
        when(property1.getRequiredType()).thenReturn(PropertyType.REFERENCE);
        when(property1.getValueConstraints()).thenReturn(new String[]{jcrRef});
        dependencies.add(resolver.getQName(jcrRef));
        final PropertyDefinition property2 = mock(PropertyDefinition.class);
        when(property2.getRequiredType()).thenReturn(PropertyType.WEAKREFERENCE);
        when(property2.getValueConstraints()).thenReturn(new String[]{jcrWeakRef});
        dependencies.add(resolver.getQName(jcrWeakRef));
        when(delegate.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[]{property1, property2});

        assertEquals("expect with propertyDefs",
                dependencies, new LinkedHashSet<>(adapt(delegate).getDependencies()));
    }

    @Test(expected = RuntimeException.class)
    public void test_ItemDefinitionQAdapter_getName_throws() {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);
        // expect throw
        adapter.getName();
    }

    @Test
    public void test_ItemDefinitionQAdapter_getName() throws Exception {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);
        when(delegate.getName()).thenReturn("nt:folder");
        assertEquals("name should be", resolver.getQName("nt:folder"), adapter.getName());
    }

    @Test
    public void test_ItemDefinitionQAdapter_getDeclaringNodeType() throws Exception {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);
        assertNull("null to start", adapter.getDeclaringNodeType());
        final NodeType delegateNodeType = mock(NodeType.class);
        when(delegateNodeType.getName()).thenReturn("nt:folder");
        when(delegate.getDeclaringNodeType()).thenReturn(delegateNodeType);
        assertEquals("name should be", resolver.getQName("nt:folder"), adapter.getDeclaringNodeType());
    }

    @Test
    public void test_ItemDefinitionQAdapter_isAutoCreated() {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);

        assertFalse("expect false to start", adapter.isAutoCreated());
        when(delegate.isAutoCreated()).thenReturn(true);
        assertTrue("expect true", adapter.isAutoCreated());
    }

    @Test
    public void test_ItemDefinitionQAdapter_getOnParentVersion() {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);

        assertEquals("zero to start", 0, adapter.getOnParentVersion());
        when(delegate.getOnParentVersion()).thenReturn(OnParentVersionAction.COPY);
        assertEquals("one for copy", OnParentVersionAction.COPY, adapter.getOnParentVersion());
    }

    @Test
    public void test_ItemDefinitionQAdapter_isProtected() {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);

        assertFalse("expect false to start", adapter.isProtected());
        when(delegate.isProtected()).thenReturn(true);
        assertTrue("expect true", adapter.isProtected());
    }

    @Test
    public void test_ItemDefinitionQAdapter_isMandatory() {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);

        assertFalse("expect false to start", adapter.isMandatory());
        when(delegate.isMandatory()).thenReturn(true);
        assertTrue("expect true", adapter.isMandatory());
    }

    @Test
    public void test_ItemDefinitionQAdapter_definesResidual() {
        final ItemDefinition delegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> adapter =
                new JsonCnd.ItemDefinitionQAdapter<>(delegate, resolver);

        when(delegate.getName()).thenReturn("jcr:content");
        assertFalse("expect false to start", adapter.definesResidual());
        when(delegate.getName()).thenReturn("*");
        assertTrue("expect true", adapter.definesResidual());
    }

    @Test
    public void test_ItemDefinitionQAdapter_definesNode() {
        final ItemDefinition itemDelegate = mock(ItemDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<ItemDefinition> itemAdapter =
                new JsonCnd.ItemDefinitionQAdapter<>(itemDelegate, resolver);
        assertFalse("expect false for item", itemAdapter.definesNode());

        final PropertyDefinition propertyDelegate = mock(PropertyDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<PropertyDefinition> propertyAdapter =
                new JsonCnd.ItemDefinitionQAdapter<>(propertyDelegate, resolver);
        assertFalse("expect false for property", propertyAdapter.definesNode());

        final NodeDefinition nodeDelegate = mock(NodeDefinition.class);
        final JsonCnd.ItemDefinitionQAdapter<NodeDefinition> nodeAdapter =
                new JsonCnd.ItemDefinitionQAdapter<>(nodeDelegate, resolver);
        assertTrue("expect true for node", nodeAdapter.definesNode());
    }

    @Test
    public void test_PropertyDefinitionQAdapter_getRequiredType() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);
        assertEquals("zero to start", 0, adapter.getRequiredType());
        when(delegate.getRequiredType()).thenReturn(PropertyType.BINARY);
        assertEquals("expect binary", PropertyType.BINARY, adapter.getRequiredType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_PropertyDefinitionQAdapter_getValueConstraints_throws() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);

        final String[] expectPatterns = new String[]{"^.*$", "[0-9]+"};
        when(delegate.getValueConstraints()).thenReturn(expectPatterns);
        // expect exception when getValueConstraints() is called with undefined property type
        Result<QValueConstraint[]> result = result1(JsonCnd.PropertyDefinitionQAdapter::getValueConstraints)
                .apply(adapter);
        // throw the expected IllegalArgumentException
        result.throwCause(IllegalArgumentException.class);
    }

    @Test
    public void test_PropertyDefinitionQAdapter_getValueConstraints() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);
        // set required type to compatible type
        when(delegate.getRequiredType()).thenReturn(PropertyType.STRING);
        assertNull("null to start", adapter.getValueConstraints());
        final String[] expectPatterns = new String[]{"^.*$", "[0-9]+"};
        when(delegate.getValueConstraints()).thenReturn(expectPatterns);
        assertArrayEquals("expect patterns", expectPatterns, Stream.of(adapter.getValueConstraints())
                .map(QValueConstraint::getString).toArray(String[]::new));
    }

    @Test
    public void test_PropertyDefinitionQAdapter_getDefaultValues() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);
        assertNull("null to start", adapter.getDefaultValues());

        final String[] expectStrings = new String[]{"one", "two", "three"};
        final javax.jcr.Value[] expectValues = Stream.of(expectStrings)
                .map(ValueFactoryImpl.getInstance()::createValue)
                .toArray(javax.jcr.Value[]::new);
        when(delegate.getDefaultValues()).thenReturn(expectValues);
        assertArrayEquals("expect values", expectStrings, Stream.of(adapter.getDefaultValues())
                .map(uncheck1(QValue::getString))
                .toArray(String[]::new));
    }

    @Test
    public void test_PropertyDefinitionQAdapter_definesNode() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);
        assertFalse("expect false", adapter.definesNode());
    }

    @Test
    public void test_PropertyDefinitionQAdapter_isMultiple() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.isMultiple());
        when(delegate.isMultiple()).thenReturn(true);
        assertTrue("expect true", adapter.isMultiple());
    }

    @Test
    public void test_PropertyDefinitionQAdapter_getAvailableQueryOperators() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);
        assertNull("null to start", adapter.getAvailableQueryOperators());
        final String[] expect = new String[]{Lexer.QUEROPS_EQUAL, Lexer.QUEROPS_LIKE};
        when(delegate.getAvailableQueryOperators()).thenReturn(expect);
        assertArrayEquals("expect ops", expect, adapter.getAvailableQueryOperators());
    }

    @Test
    public void test_PropertyDefinitionQAdapter_isFullTextSearchable() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.isFullTextSearchable());
        when(delegate.isFullTextSearchable()).thenReturn(true);
        assertTrue("expect true", adapter.isFullTextSearchable());
    }

    @Test
    public void test_PropertyDefinitionQAdapter_isQueryOrderable() {
        final PropertyDefinition delegate = mock(PropertyDefinition.class);
        final JsonCnd.PropertyDefinitionQAdapter adapter =
                new JsonCnd.PropertyDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.isQueryOrderable());
        when(delegate.isQueryOrderable()).thenReturn(true);
        assertTrue("expect true", adapter.isQueryOrderable());
    }

    @Test
    public void test_NodeDefinitionQAdapter_getDefaultPrimaryType() throws Exception {
        final NodeDefinition delegate = mock(NodeDefinition.class);
        final JsonCnd.NodeDefinitionQAdapter adapter =
                new JsonCnd.NodeDefinitionQAdapter(delegate, resolver);
        assertNull("null to start", adapter.getDefaultPrimaryType());
        when(delegate.getDefaultPrimaryTypeName()).thenReturn("nt:folder");
        assertEquals("expect name", resolver.getQName("nt:folder"), adapter.getDefaultPrimaryType());
    }

    @Test
    public void test_NodeDefinitionQAdapter_getRequiredPrimaryTypes() throws Exception {
        final NodeDefinition delegate = mock(NodeDefinition.class);
        final JsonCnd.NodeDefinitionQAdapter adapter =
                new JsonCnd.NodeDefinitionQAdapter(delegate, resolver);
        assertNull("null to start", adapter.getRequiredPrimaryTypes());
        when(delegate.getRequiredPrimaryTypeNames()).thenReturn(new String[]{"nt:base", "nt:folder"});
        assertArrayEquals("expect names", new Name[]{
                resolver.getQName("nt:base"), resolver.getQName("nt:folder")
        }, adapter.getRequiredPrimaryTypes());
    }

    @Test
    public void test_NodeDefinitionQAdapter_allowsSameNameSiblings() {
        final NodeDefinition delegate = mock(NodeDefinition.class);
        final JsonCnd.NodeDefinitionQAdapter adapter =
                new JsonCnd.NodeDefinitionQAdapter(delegate, resolver);

        assertFalse("expect false to start", adapter.allowsSameNameSiblings());
        when(delegate.allowsSameNameSiblings()).thenReturn(true);
        assertTrue("expect true", adapter.allowsSameNameSiblings());
    }

    @Test
    public void test_NodeDefinitionQAdapter_definesNode() {
        final NodeDefinition delegate = mock(NodeDefinition.class);
        final JsonCnd.NodeDefinitionQAdapter adapter =
                new JsonCnd.NodeDefinitionQAdapter(delegate, resolver);
        assertTrue("expect true", adapter.definesNode());
    }
}
