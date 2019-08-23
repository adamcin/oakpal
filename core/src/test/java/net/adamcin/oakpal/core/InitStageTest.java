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

import static net.adamcin.oakpal.core.Fun.toEntry;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.uncheck2;
import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.security.AccessControlException;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionImpl;
import org.junit.Before;
import org.junit.Test;

public class InitStageTest {

    private static final String NS_PREFIX = "foo";
    private static final String NS_URI = "http://foo.com";

    private static final String NT_NAME_PRIMARY = "foo:primaryType";
    private static final String NT_NAME_MIXIN = "foo:mixinType";

    private static final File cndAFile = new File("src/test/resources/InitStageTest/a.cnd");
    private static final File cndBFile = new File("src/test/resources/InitStageTest/b.cnd");

    private URL cndAUrl;
    private URL cndBUrl;

    @Before
    public void setUp() throws Exception {
        cndAUrl = cndAFile.toURI().toURL();
        cndBUrl = cndBFile.toURI().toURL();
    }

    public static List<JcrNs> getNs() {
        return Collections.singletonList(JcrNs.create(NS_PREFIX, NS_URI));
    }

    private static NamespaceMapping getMapping() {
        return JsonCnd.toNamespaceMapping(Collections
                .singletonList(JcrNs.create(NS_PREFIX, NS_URI)));
    }

    private static List<QNodeTypeDefinition> getNtDefs() {
        return JsonCnd.getQTypesFromJson(obj()
                .key(NT_NAME_PRIMARY, key("extends", arr("nt:base")))
                .key(NT_NAME_MIXIN, key("attributes", arr("mixin")))
                .get(), getMapping());
    }

    @Test
    public void testBuildWithNs() throws Exception {
        final InitStage stage = new InitStage.Builder().withNs(NS_PREFIX, NS_URI).build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            assertEquals("ns prefix should be", NS_PREFIX, session.getNamespacePrefix(NS_URI));
            assertEquals("ns uri should be", NS_URI, session.getNamespaceURI(NS_PREFIX));
        });
        final InitStage stage2 = new InitStage.Builder()
                .withNs(Collections.singletonList(JcrNs.create(NS_PREFIX, NS_URI))).build();
        new OakMachine.Builder().withInitStage(stage2).build().adminInitAndInspect(session -> {
            assertEquals("ns prefix should be", NS_PREFIX, session.getNamespacePrefix(NS_URI));
            assertEquals("ns uri should be", NS_URI, session.getNamespaceURI(NS_PREFIX));
        });

        final CompletableFuture<Map.Entry<String, String>> errorMapping = new CompletableFuture<>();
        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(invoked -> errorMapping.complete(toEntry(invoked.getArgument(1), invoked.getArgument(2))))
                .when(errorListener).onJcrNamespaceRegistrationError(any(Throwable.class), anyString(), anyString());
        final InitStage stageFoo2 = new InitStage.Builder()
                .withNs("foo2", NS_URI)
                .build();
        final InitStage stageXml = new InitStage.Builder()
                .withNs("xml", "http://notxml.com")
                .build();
        new OakMachine.Builder().withErrorListener(errorListener)
                .withInitStage(stage2, stageFoo2, stageXml).build().adminInitAndInspect(session -> {
            assertEquals("ns prefix should be", "foo2", session.getNamespacePrefix(NS_URI));
            assertEquals("ns uri should be", NS_URI, session.getNamespaceURI("foo2"));
        });

        assertEquals("bad namespace mapping is", toEntry("xml", "http://notxml.com"), errorMapping.getNow(null));
    }

    @Test
    public void testBuildWithPrivilege() throws Exception {
        final String privName = "foo:canDo";
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withPrivilege(privName)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            assertEquals("privilege should not throw", privName, manager.getPrivilege(privName).getName());
        });

        final CompletableFuture<String> errorPrivilege = new CompletableFuture<>();
        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(invoked -> errorPrivilege.complete(invoked.getArgument(1))).when(errorListener)
                .onJcrPrivilegeRegistrationError(any(Throwable.class), anyString());
        final InitStage stage2 = new InitStage.Builder().withNs(getNs())
                .withPrivileges(Arrays.asList(privName, "bad:privilege"))
                .build();
        new OakMachine.Builder().withErrorListener(errorListener)
                .withInitStage(stage2).build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            assertEquals("privilege should not throw", privName, manager.getPrivilege(privName).getName());
        });

        assertEquals("bad privilege is", "bad:privilege", errorPrivilege.getNow(""));

    }

    @Test
    public void testBuildWithPrivilegeDefinition() throws Exception {
        final String privName = "foo:canDo";

        final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(getNs());
        final PrivilegeDefinition privDef = uncheck2(JsonCnd.privDefinitionMapper(mapping))
                .apply(privName, JsonValue.EMPTY_JSON_OBJECT);
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withPrivilegeDefinition(privDef)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            assertEquals("privilege should not throw", privName, manager.getPrivilege(privName).getName());
        });

        final InitStage stageWithCollection = new InitStage.Builder().withNs(getNs())
                .withPrivilegeDefinitions(Collections.singletonList(privDef))
                .build();

        new OakMachine.Builder().withInitStage(stageWithCollection).build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            assertEquals("privilege should not throw", privName, manager.getPrivilege(privName).getName());
        });

        final NamespaceMapping mapping2 = JsonCnd.toNamespaceMapping(getNs());
        mapping2.setMapping("oof", "http://oof.com");

        final NamePathResolver resolver = new DefaultNamePathResolver(mapping2);
        final Name badPrivilege = resolver.getQName("oof:agg");
        final InitStage stageWithAggregate = new InitStage.Builder().withNs(getNs())
                .withPrivilegeDefinitions(Collections.singletonList(
                        new PrivilegeDefinitionImpl(badPrivilege, false,
                                Collections.singleton(resolver.getQName("oof:unknown")))))
                .build();

        final CompletableFuture<String> errorPrivilege = new CompletableFuture<>();
        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(invoked -> errorPrivilege.complete(invoked.getArgument(1))).when(errorListener)
                .onJcrPrivilegeRegistrationError(any(Throwable.class), anyString());
        new OakMachine.Builder()
                .withInitStage(stageWithAggregate)
                .withErrorListener(errorListener)
                .build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            assertEquals("privilege should not throw", "jcr:read", manager.getPrivilege("jcr:read").getName());
        });

        assertEquals("bad privilege is", badPrivilege.toString(), errorPrivilege.getNow(""));

    }

    @Test(expected = AccessControlException.class)
    public void testBuildWithNullPrivilege() throws Exception {
        final String privName = "foo:canDo";
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withPrivileges(null)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            manager.getPrivilege(privName);
        });
    }

    @Test(expected = AccessControlException.class)
    public void testBuildWithNullPrivilegeDefinition() throws Exception {
        final String privName = "foo:canDo";
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withPrivilegeDefinitions(null)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            PrivilegeManager manager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
            manager.getPrivilege(privName);
        });
    }

    @Test
    public void testBuildWithQNodeTypes() throws Exception {
        final List<QNodeTypeDefinition> ntDefs = getNtDefs();
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withQNodeTypes(ntDefs)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            assertTrue("manager should have type", manager.hasNodeType(NT_NAME_PRIMARY));
            assertTrue("manager should have type", manager.hasNodeType(NT_NAME_MIXIN));
        });

        final JsonObject badNtsJson = key("foo:badType", key("extends", arr("nt:basebase"))).get();
        final List<QNodeTypeDefinition> badNts = JsonCnd.getQTypesFromJson(badNtsJson, getMapping());
        final InitStage badNtStage = new InitStage.Builder().withNs(getNs()).withQNodeTypes(badNts).build();

        final DefaultErrorListener errorListener = new DefaultErrorListener();
        new OakMachine.Builder().withErrorListener(errorListener)
                .withInitStage(stage, badNtStage)
                .build().adminInitAndInspect(session -> {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            assertTrue("manager should have type", manager.hasNodeType(NT_NAME_PRIMARY));
            assertTrue("manager should have type", manager.hasNodeType(NT_NAME_MIXIN));
            assertFalse("manager should not have type", manager.hasNodeType("foo:badType"));
        });

        assertEquals("num of violations", 1, errorListener.getReportedViolations().size());
    }

    @Test
    public void testBuildWithForcedRoot() throws Exception {
        final String path = "/correct/path";
        final ForcedRoot root = new ForcedRoot()
                .withPath(path)
                .withPrimaryType(NT_NAME_PRIMARY)
                .withMixinTypes(NT_NAME_MIXIN);
        final InitStage stage = new InitStage.Builder()
                .withNs(getNs())
                .withQNodeTypes(getNtDefs())
                .withForcedRoot(root)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists(path));
            assertTrue("primary type is", session.getNode(path).isNodeType(NT_NAME_PRIMARY));
            assertTrue("mixin type is", session.getNode(path).isNodeType(NT_NAME_MIXIN));
        });

        final InitStage stage2 = new InitStage.Builder()
                .withNs(getNs())
                .withQNodeTypes(getNtDefs())
                .withForcedRoot(path, NT_NAME_PRIMARY, NT_NAME_MIXIN)
                .build();
        new OakMachine.Builder().withInitStage(stage2).build().adminInitAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists(path));
            assertTrue("primary type is", session.getNode(path).isNodeType(NT_NAME_PRIMARY));
            assertTrue("mixin type is", session.getNode(path).isNodeType(NT_NAME_MIXIN));
        });

        final InitStage stage3 = new InitStage.Builder()
                .withNs(getNs())
                .withQNodeTypes(getNtDefs())
                .withForcedRoots(Collections.singletonList(root))
                .build();
        new OakMachine.Builder()
                .withInitStage(stage3).build().adminInitAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists(path));
            assertTrue("primary type is", session.getNode(path).isNodeType(NT_NAME_PRIMARY));
            assertTrue("mixin type is", session.getNode(path).isNodeType(NT_NAME_MIXIN));
        });

        final CompletableFuture<String> errorRoot = new CompletableFuture<>();
        ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(invoked -> errorRoot.complete(((ForcedRoot) invoked.getArgument(1)).getPath()))
                .when(errorListener).onForcedRootCreationError(any(Throwable.class), any(ForcedRoot.class));

        final InitStage stage4 = new InitStage.Builder()
                .withNs(getNs())
                .withForcedRoot("/folder", "nt:folder")
                .withForcedRoot("/folder/unstructured", "nt:unstructured")
                .build();
        new OakMachine.Builder().withErrorListener(errorListener)
                .withInitStage(stage4).build().adminInitAndInspect(session -> {
            assertTrue("path should exist", session.nodeExists("/folder"));
        });

        assertEquals("bad forced root path is", "/folder/unstructured", errorRoot.getNow(""));
    }

    @Test
    public void testBuildWithUnorderedCndUrls() throws Exception {
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withUnorderedCndUrl(cndAUrl, cndBUrl)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            assertTrue("manager should have type", manager.hasNodeType("a:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("a:mixinType"));
            assertTrue("manager should have type", manager.hasNodeType("b:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("b:mixinType"));
        });

        final InitStage stage2 = new InitStage.Builder().withNs(getNs())
                .withUnorderedCndUrls(Arrays.asList(cndAUrl, cndBUrl))
                .build();
        new OakMachine.Builder().withInitStage(stage2).build().adminInitAndInspect(session -> {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            assertTrue("manager should have type", manager.hasNodeType("a:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("a:mixinType"));
            assertTrue("manager should have type", manager.hasNodeType("b:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("b:mixinType"));
        });
    }

    @Test
    public void testBuildWithOrderedCndUrls() throws Exception {
        final InitStage stage = new InitStage.Builder().withNs(getNs())
                .withOrderedCndUrl(cndAUrl, cndBUrl)
                .build();
        new OakMachine.Builder().withInitStage(stage).build().adminInitAndInspect(session -> {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            assertTrue("manager should have type", manager.hasNodeType("a:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("a:mixinType"));
            assertTrue("manager should have type", manager.hasNodeType("b:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("b:mixinType"));
        });

        final InitStage stage2 = new InitStage.Builder().withNs(getNs())
                .withOrderedCndUrls(Arrays.asList(cndAUrl, cndBUrl))
                .build();
        new OakMachine.Builder().withInitStage(stage2).build().adminInitAndInspect(session -> {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            assertTrue("manager should have type", manager.hasNodeType("a:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("a:mixinType"));
            assertTrue("manager should have type", manager.hasNodeType("b:primaryType"));
            assertTrue("manager should have type", manager.hasNodeType("b:mixinType"));
        });
    }
}