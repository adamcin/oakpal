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

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JsonCnd.NodeTypeDefinitionKey.PRIMARYITEM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class JsonCndTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCndTest.class);

    private NamePathResolver resolver = new DefaultNamePathResolver(QName.BUILTIN_MAPPINGS);
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
                QName.BUILTIN_MAPPINGS);

        assertEquals("size is ", 1, nts.size());
        assertEquals("name is", "foo", nts.get(0).getName().getLocalName());
        assertEquals("uri is", Name.NS_NT_URI, nts.get(0).getName().getNamespaceURI());
    }

    @Test
    public void testToJson() {
        assertEquals("empty list to empty object", 0,
                JsonCnd.toJson(Collections.emptyList(), getMapping()).size());
        JsonObject qJson = key("nt:foo", key("@", arr("mixin"))).get();
        List<QNodeTypeDefinition> nts = JsonCnd.getQTypesFromJson(qJson, QName.BUILTIN_MAPPINGS);

        JsonObject toJson = JsonCnd.toJson(nts, getMapping());
        assertEquals("json meets json", qJson, toJson);
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
    public void testNamedBy() throws Exception {
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
                .map(compose(File::toURI, uncheck1(URI::toURL)))
                .collect(Collectors.toList());

        final List<Result<NodeTypeSet>> sets = JsonCnd.readNodeTypes(getMapping(), cnds);
        assertEquals("sets should have n sets", cnds.size(), sets.size());
    }

    @Test
    public void test_PRIMARYITEM_writeJson() throws Exception {
        QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);
        when(def.getPrimaryItemName()).thenReturn(resolver.getQName("jcr:content"));
        JsonValue value = PRIMARYITEM.writeJson(def, resolver);
        LOGGER.info("[test_PRIMARYITEM_writeJson] value = {}", value);
        assertSame("value is a JsonString", JsonValue.ValueType.STRING, value.getValueType());
    }

}
