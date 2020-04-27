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

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Result;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NodeTypeManager;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.api.JavaxJson.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OakpalPlanTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OakpalPlanTest.class);

    private final String BASE_PATH = "src/test/resources/OakpalPlanTest";
    private final String CHECK_NOTHING = "OakpalPlanTest/checkNothing.js";
    private final File base = new File(BASE_PATH);
    private URL baseUrl;

    @Before
    public void setUp() throws Exception {
        baseUrl = base.toURI().toURL();
    }

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

    private OakpalPlan.Builder builder() {
        return new OakpalPlan.Builder(null, null);
    }

    private OakpalPlan.Builder builder(final @NotNull String name) {
        return new OakpalPlan.Builder(null, name);
    }

    private OakpalPlan.Builder builder(final @NotNull URL base) {
        return new OakpalPlan.Builder(base, null);
    }

    private OakpalPlan.Builder builder(final @Nullable URL base, final @Nullable String name) {
        return new OakpalPlan.Builder(base, name);
    }

    @Test
    public void testBuilder_constructor() throws Exception {
        assertNull("null base is null", builder().build().getBase());
        assertEquals("null name is default", OakpalPlan.DEFAULT_PLAN_NAME, builder().build().getName());
        assertEquals("name from url", "foo.json",
                builder(new URL("http://foo.com/foo.json")).build().getName());
        assertEquals("name from name", "foo.json", builder("foo.json").build().getName());
    }

    @Test
    public void testBuilder_withChecklists() {
        final String checklist1 = "foo1";
        final String checklist2 = "foo2";
        assertEquals("checklists", Arrays.asList(checklist1, checklist2),
                builder().withChecklists(Arrays.asList(checklist1, checklist2))
                        .build().getChecklists());
    }

    @Test
    public void testBuilder_withPreInstallUrls() throws Exception {
        final URL url1 = new URL("http://foo.com/foo1.zip");
        final URL url2 = new URL("http://foo.com/foo2.zip");
        assertEquals("urls should be", Arrays.asList(url1, url2), builder()
                .withPreInstallUrls(Arrays.asList(url1, url2)).build().getPreInstallUrls());
    }

    @Test
    public void testBuilder_withJcrNamespaces() {
        final JcrNs ns1 = JcrNs.create("foo", "http://foo.com");
        final JcrNs ns2 = JcrNs.create("bar", "http://bar.com");
        assertEquals("urls should be", Arrays.asList(ns1, ns2), builder()
                .withJcrNamespaces(Arrays.asList(ns1, ns2)).build().getJcrNamespaces());
    }

    @Test
    public void testBuilder_withJcrNodetypes() {
        final JsonObject json = obj()
                .key("nt:foo1", key("@", arr("mixin")))
                .key("nt:foo2", key("@", arr("mixin")))
                .get();
        final List<QNodeTypeDefinition> expectTypes = JsonCnd.getQTypesFromJson(json, getMapping());
        assertEquals("nodetypes should be", expectTypes, builder()
                .withJcrNodetypes(expectTypes).build().getJcrNodetypes());
    }

    @Test
    public void testBuilder_withJcrPrivileges() {
        final String privilege1 = "foo:priv1";
        final String privilege2 = "foo:priv2";
        final List<PrivilegeDefinition> expectPrivileges =
                JsonCnd.getPrivilegesFromJson(arr(privilege1, privilege2).get(), getMapping());
        assertEquals("expect privileges", expectPrivileges,
                builder().withJcrPrivileges(expectPrivileges)
                        .build().getJcrPrivileges());
    }

    @Test
    public void testBuilder_withForcedRoots() {
        final ForcedRoot root1 = new ForcedRoot().withPath("/root1");
        final ForcedRoot root2 = new ForcedRoot().withPath("/root2");
        final List<ForcedRoot> expectRoots = Arrays.asList(root1, root2);
        assertEquals("expect roots", expectRoots,
                builder().withForcedRoots(expectRoots).build().getForcedRoots());
    }

    @Test
    public void testBuilder_withChecks() {
        final CheckSpec check1 = CheckSpec.fromJson(key("name", "check1").key("impl", CHECK_NOTHING).get());
        final CheckSpec check2 = CheckSpec.fromJson(key("name", "check2").key("impl", CHECK_NOTHING).get());
        final List<CheckSpec> expectChecks = Arrays.asList(check1, check2);
        assertEquals("expect checks", expectChecks,
                builder().withChecks(expectChecks).build().getChecks());
    }

    @Test
    public void testBuilder_withEnablePreInstallHooks() {
        assertTrue("enablePreInstallHooks true",
                builder().withEnablePreInstallHooks(true).build()
                        .isEnablePreInstallHooks());
        assertFalse("enablePreInstallHooks false",
                builder().withEnablePreInstallHooks(false).build()
                        .isEnablePreInstallHooks());
    }

    @Test
    public void testBuilder_withInstallHookPolicy() {
        assertNull("implicit null", builder().build().getInstallHookPolicy());
        assertNull("explicit null", builder().withInstallHookPolicy(null).build().getInstallHookPolicy());
        for (InstallHookPolicy policy : InstallHookPolicy.values()) {
            assertSame("same policy", policy,
                    builder().withInstallHookPolicy(policy).build().getInstallHookPolicy());
        }
    }

    @Test
    public void testBuilder_startingWithPlan_defaults() {
        final OakpalPlan derived = builder().startingWithPlan(builder().build()).build();
        assertEquals("default checklists", Collections.emptyList(), derived.getChecklists());
        assertEquals("default checks", Collections.emptyList(), derived.getChecks());
        assertEquals("default forcedRoots", Collections.emptyList(), derived.getForcedRoots());
        assertEquals("default namespaces", Collections.emptyList(), derived.getJcrNamespaces());
        assertEquals("default nodetypes", Collections.emptyList(), derived.getJcrNodetypes());
        assertEquals("default privileges", Collections.emptyList(), derived.getJcrPrivileges());
        assertFalse("default enablePreInstallHook", derived.isEnablePreInstallHooks());
        assertNull("default installHooksPolicy", derived.getInstallHookPolicy());
        assertEquals("default preInstallUrls", Collections.emptyList(), derived.getPreInstallUrls());
        assertNull("default builder originalJson is null", derived.getOriginalJson());
        assertEquals("default builder toJson is empty",
                JsonValue.EMPTY_JSON_OBJECT, derived.toJson());
    }

    @Test
    public void testBuilder_startingWithPlan_nonDefaults() throws Exception {
        final List<String> expectChecklists = Arrays.asList("checklist1", "checklist2");
        final CheckSpec check1 = CheckSpec.fromJson(key("name", "check1").key("impl", CHECK_NOTHING).get());
        final CheckSpec check2 = CheckSpec.fromJson(key("name", "check2").key("impl", CHECK_NOTHING).get());
        final List<CheckSpec> expectChecks = Arrays.asList(check1, check2);
        final ForcedRoot root1 = new ForcedRoot().withPath("/root1");
        final ForcedRoot root2 = new ForcedRoot().withPath("/root2");
        final List<ForcedRoot> expectRoots = Arrays.asList(root1, root2);
        final JcrNs ns1 = JcrNs.create("foo", "http://foo.com");
        final JcrNs ns2 = JcrNs.create("bar", "http://bar.com");
        final List<JcrNs> expectNamespaces = Arrays.asList(ns1, ns2);
        final JsonObject json = obj()
                .key("nt:foo1", key("@", arr("mixin")))
                .key("nt:foo2", key("@", arr("mixin")))
                .get();
        final List<QNodeTypeDefinition> expectTypes = JsonCnd.getQTypesFromJson(json, getMapping());
        final String privilege1 = "foo:priv1";
        final String privilege2 = "foo:priv2";
        final List<PrivilegeDefinition> expectPrivileges =
                JsonCnd.getPrivilegesFromJson(arr(privilege1, privilege2).get(), getMapping());
        final InstallHookPolicy expectPolicy = InstallHookPolicy.PROHIBIT;
        final URL url1 = new URL("http://foo.com/foo1.zip");
        final URL url2 = new URL("http://foo.com/foo2.zip");
        final List<URL> expectPreInstallUrls = Arrays.asList(url1, url2);

        final OakpalPlan base = builder()
                .withChecklists(expectChecklists)
                .withChecks(expectChecks)
                .withForcedRoots(expectRoots)
                .withJcrNamespaces(expectNamespaces)
                .withJcrNodetypes(expectTypes)
                .withJcrPrivileges(expectPrivileges)
                .withEnablePreInstallHooks(true)
                .withInstallHookPolicy(expectPolicy)
                .withPreInstallUrls(expectPreInstallUrls)
                .build();

        final OakpalPlan derived = builder().startingWithPlan(base).build();
        assertEquals("expect checklists", expectChecklists, derived.getChecklists());
        assertEquals("expect checks", expectChecks, derived.getChecks());
        assertEquals("expect forcedRoots", expectRoots, derived.getForcedRoots());
        assertEquals("expect namespaces", expectNamespaces, derived.getJcrNamespaces());
        assertEquals("expect nodetypes", expectTypes, derived.getJcrNodetypes());
        assertEquals("expect privileges", expectPrivileges, derived.getJcrPrivileges());
        assertTrue("expect enablePreInstallHook", derived.isEnablePreInstallHooks());
        assertSame("expect installHooksPolicy", expectPolicy, derived.getInstallHookPolicy());
        assertEquals("expect preInstallUrls", expectPreInstallUrls, derived.getPreInstallUrls());
        assertNull("expect builder originalJson is null", derived.getOriginalJson());

        final URL expectComparisonUrl = new URL(baseUrl, "fullPlan.json");
        try (InputStream input = expectComparisonUrl.openStream();
             JsonReader reader = Json.createReader(input)) {
            final JsonObject expectJson = reader.readObject();
            assertEquals("expect fromJson originalJson is", expectJson, derived.toJson());
        }
    }

    @Test
    public void testBuilder_toJson() throws Exception {
        final List<String> expectChecklists = Arrays.asList("checklist1", "checklist2");
        final CheckSpec check1 = CheckSpec.fromJson(key("name", "check1").key("impl", CHECK_NOTHING).get());
        final CheckSpec check2 = CheckSpec.fromJson(key("name", "check2").key("impl", CHECK_NOTHING).get());
        final List<CheckSpec> expectChecks = Arrays.asList(check1, check2);
        final ForcedRoot root1 = new ForcedRoot().withPath("/root1");
        final ForcedRoot root2 = new ForcedRoot().withPath("/root2");
        final List<ForcedRoot> expectRoots = Arrays.asList(root1, root2);
        final JcrNs ns1 = JcrNs.create("foo", "http://foo.com");
        final JcrNs ns2 = JcrNs.create("bar", "http://bar.com");
        final List<JcrNs> expectNamespaces = Arrays.asList(ns1, ns2);
        final JsonObject json = obj()
                .key("nt:foo1", key("@", arr("mixin")))
                .key("nt:foo2", key("@", arr("mixin")))
                .get();
        final List<QNodeTypeDefinition> expectTypes = JsonCnd.getQTypesFromJson(json, getMapping());
        final String privilege1 = "foo:priv1";
        final String privilege2 = "foo:priv2";
        final List<PrivilegeDefinition> expectPrivileges =
                JsonCnd.getPrivilegesFromJson(arr(privilege1, privilege2).get(), getMapping());
        final InstallHookPolicy expectPolicy = InstallHookPolicy.PROHIBIT;
        final URL url1 = new URL("http://foo.com/foo1.zip");
        final URL url2 = new URL("http://foo.com/foo2.zip");
        final List<URL> expectPreInstallUrls = Arrays.asList(url1, url2);

        final URL expectBaseUrl = new URL(baseUrl, "fullPlan.json");
        final OakpalPlan derived = builder(expectBaseUrl)
                .withChecklists(expectChecklists)
                .withChecks(expectChecks)
                .withForcedRoots(expectRoots)
                .withJcrNamespaces(expectNamespaces)
                .withJcrNodetypes(expectTypes)
                .withJcrPrivileges(expectPrivileges)
                .withEnablePreInstallHooks(true)
                .withInstallHookPolicy(expectPolicy)
                .withPreInstallUrls(expectPreInstallUrls)
                .build();
        try (InputStream input = expectBaseUrl.openStream();
             JsonReader reader = Json.createReader(input)) {
            final JsonObject expectJson = reader.readObject();
            assertEquals("expect fromJson originalJson is", expectJson, derived.toJson());
        }
    }

    @Test(expected = IOException.class)
    public void testFromJson_noPlan() throws Exception {
        final Result<OakpalPlan> noPlan = OakpalPlan.fromJson(new URL(baseUrl, "noPlan.json"));
        noPlan.throwCause(IOException.class);
    }

    @Test(expected = JsonException.class)
    public void testFromJson_badJson() throws Exception {
        final Result<OakpalPlan> badJson = OakpalPlan.fromJson(new URL(baseUrl, "badJson.json"));
        badJson.throwCause(JsonException.class);
    }

    @Test
    public void testFromJson() throws Exception {
        final Result<OakpalPlan> emptyPlan = OakpalPlan.fromJson(new URL(baseUrl, "emptyplan.json"));
        assertTrue("empty plan is successful", emptyPlan.isSuccess());
    }

    @Test
    public void testFromJson_emptyPlan() throws Exception {
        final URL expectBaseUrl = new URL(baseUrl, "emptyplan.json");
        final Result<OakpalPlan> emptyPlan = OakpalPlan.fromJson(expectBaseUrl);
        assertTrue("empty plan is successful", emptyPlan.isSuccess());
        final OakpalPlan derived = emptyPlan.getOrDefault(null);
        assertNotNull("not null empty plan", derived);
        assertEquals("empty plan has empty json",
                JsonValue.EMPTY_JSON_OBJECT,
                derived.toJson());
        assertEquals("base uri", expectBaseUrl, derived.getBase());
        assertEquals("default checklists", Collections.emptyList(), derived.getChecklists());
        assertEquals("default checks", Collections.emptyList(), derived.getChecks());
        assertEquals("default forcedRoots", Collections.emptyList(), derived.getForcedRoots());
        assertEquals("default namespaces", Collections.emptyList(), derived.getJcrNamespaces());
        assertEquals("default nodetypes", Collections.emptyList(), derived.getJcrNodetypes());
        assertEquals("default privileges", Collections.emptyList(), derived.getJcrPrivileges());
        assertFalse("default enablePreInstallHook", derived.isEnablePreInstallHooks());
        assertNull("default installHooksPolicy", derived.getInstallHookPolicy());
        assertEquals("default preInstallUrls", Collections.emptyList(), derived.getPreInstallUrls());
    }

    private void expectFullPlan(final @NotNull OakpalPlan derived) throws Exception {
        final List<String> expectChecklists = Arrays.asList("checklist1", "checklist2");
        final CheckSpec check1 = CheckSpec.fromJson(key("name", "check1").key("impl", CHECK_NOTHING).get());
        final CheckSpec check2 = CheckSpec.fromJson(key("name", "check2").key("impl", CHECK_NOTHING).get());
        final List<CheckSpec> expectChecks = Arrays.asList(check1, check2);
        final ForcedRoot root1 = new ForcedRoot().withPath("/root1");
        final ForcedRoot root2 = new ForcedRoot().withPath("/root2");
        final List<ForcedRoot> expectRoots = new ArrayList<>(Arrays.asList(root1, root2));
        final JcrNs ns1 = JcrNs.create("foo", "http://foo.com");
        final JcrNs ns2 = JcrNs.create("bar", "http://bar.com");
        final List<JcrNs> expectNamespaces = Arrays.asList(ns1, ns2);
        final JsonObject json = obj()
                .key("nt:foo1", key("@", arr("mixin")))
                .key("nt:foo2", key("@", arr("mixin")))
                .get();
        final List<QNodeTypeDefinition> expectTypes = JsonCnd.getQTypesFromJson(json, getMapping());
        final String privilege1 = "foo:priv1";
        final String privilege2 = "foo:priv2";
        final List<String> expectPrivilegeNames = Arrays.asList(privilege1, privilege2);
        final List<PrivilegeDefinition> expectPrivileges =
                JsonCnd.getPrivilegesFromJson(wrap(expectPrivilegeNames), getMapping());
        final InstallHookPolicy expectPolicy = InstallHookPolicy.PROHIBIT;

        assertEquals("expect checklists", expectChecklists, derived.getChecklists());
        assertEquals("expect checks", expectChecks, derived.getChecks());
        assertEquals("expect forcedRoots", expectRoots, derived.getForcedRoots());
        assertEquals("expect namespaces", expectNamespaces, derived.getJcrNamespaces());
        assertEquals("expect nodetypes", expectTypes, derived.getJcrNodetypes());
        assertEquals("expect privileges", expectPrivileges, derived.getJcrPrivileges());
        assertTrue("expect enablePreInstallHook", derived.isEnablePreInstallHooks());
        assertSame("expect installHooksPolicy", expectPolicy, derived.getInstallHookPolicy());
        assertNotNull("expect fromJson originalJson is not null", derived.getOriginalJson());
    }

    @Test
    public void testFromJson_fullPlan_justJson() throws Exception {
        final URL expectBaseUrl = new URL(baseUrl, "fullPlan.json");

        try (InputStream input = expectBaseUrl.openStream();
             JsonReader reader = Json.createReader(input)) {
            final JsonObject expectJson = reader.readObject();
            final OakpalPlan derived = OakpalPlan.fromJson(expectJson);
            assertNotNull("not null full plan", derived);

            expectFullPlan(derived);
            assertEquals("expect no preInstallUrls", Collections.emptyList(), derived.getPreInstallUrls());
            assertEquals("expect fromJson originalJson is", expectJson, derived.getOriginalJson());
            assertEquals("expect fromJson toJson is", expectJson, derived.toJson());
        }
    }

    @Test
    public void testFromJson_fullPlan_fromUrl() throws Exception {
        final URL expectBaseUrl = new URL(baseUrl, "fullPlan.json");
        final Result<OakpalPlan> fullPlan = OakpalPlan.fromJson(expectBaseUrl);
        assertTrue("full plan is successful", fullPlan.isSuccess());
        final OakpalPlan derived = fullPlan.getOrDefault(null);
        assertNotNull("not null full plan", derived);

        expectFullPlan(derived);
        final URL url1 = new URL("http://foo.com/foo1.zip");
        final URL url2 = new URL("http://foo.com/foo2.zip");
        final List<URL> expectPreInstallUrls = Arrays.asList(url1, url2);
        assertEquals("expect preInstallUrls", expectPreInstallUrls, derived.getPreInstallUrls());

        try (InputStream input = expectBaseUrl.openStream();
             JsonReader reader = Json.createReader(input)) {
            final JsonObject expectJson = reader.readObject();
            assertEquals("expect fromJson originalJson is", expectJson, derived.getOriginalJson());
            assertEquals("expect fromJson toJson is", expectJson, derived.toJson());
        }
    }

    @Test
    public void testToInitStage() throws Exception {
        final URL expectBaseUrl = new URL(baseUrl, "fullPlan.json");
        final Result<OakpalPlan> fullPlan = OakpalPlan.fromJson(expectBaseUrl);
        assertTrue("full plan is successful", fullPlan.isSuccess());
        final OakpalPlan derived = fullPlan.getOrDefault(null);
        assertNotNull("not null full plan", derived);

        final ErrorListener errorListener = mock(ErrorListener.class);
        doAnswer(call -> {
            LOGGER.error("call: " + call.getArgument(1, String.class),
                    call.getArgument(0, Throwable.class));
            return true;
        }).when(errorListener).onJcrPrivilegeRegistrationError(any(Throwable.class), anyString());
        new OakMachine.Builder()
                .withErrorListener(errorListener)
                .withInitStage(derived.toInitStage())
                .build()
                .adminInitAndInspect(session -> {
                    assertEquals("foo ns uri is defined",
                            NS_FOO_URI, session.getNamespaceURI(NS_FOO_PREFIX));
                    assertEquals("bar ns uri is defined",
                            NS_BAR_URI, session.getNamespaceURI(NS_BAR_PREFIX));

                    final NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
                    assertTrue("nt:foo1 is mixin",
                            manager.getNodeType("nt:foo1").isMixin());
                    assertTrue("nt:foo2 is mixin",
                            manager.getNodeType("nt:foo2").isMixin());

                    assertTrue("/root1 exists", session.nodeExists("/root1"));
                    assertTrue("/root2 exists", session.nodeExists("/root2"));
                    assertFalse("/root3 does not exist", session.nodeExists("/root3"));

                    final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) session.getWorkspace())
                            .getPrivilegeManager();
                    assertNotNull("privilege1 is defined",
                            privilegeManager.getPrivilege("foo:priv1"));
                    assertNotNull("privilege1 is defined",
                            privilegeManager.getPrivilege("foo:priv2"));
                });
    }

    @Test
    public void testToOakMachineBuilder() throws Exception {
        final URL expectBaseUrl = new URL(baseUrl, "fullPlan.json");
        final Result<OakpalPlan> fullPlan = OakpalPlan.fromJson(expectBaseUrl);
        assertTrue("full plan is successful", fullPlan.isSuccess());
        final OakpalPlan derived = fullPlan.getOrDefault(null);
        assertNotNull("not null full plan", derived);

        final ErrorListener errorListener = mock(ErrorListener.class);
        final OakMachine machine =
                derived.toOakMachineBuilder(errorListener, Util.getDefaultClassLoader()).build();

        final URL url1 = new URL("http://foo.com/foo1.zip");
        final URL url2 = new URL("http://foo.com/foo2.zip");
        final List<URL> expectPreInstallUrls = Arrays.asList(url1, url2);
        assertEquals("expect preInstallUrls", expectPreInstallUrls, machine.getPreInstallUrls());
    }

    @SuppressWarnings("WeakerAccess")
    static class NotACheck {

    }

    @Test(expected = Exception.class)
    public void testToOakMachineBuilder_throws() throws Exception {
        builder().withChecks(Collections
                .singletonList(CheckSpec.fromJson(key("impl", NotACheck.class.getName()).get())))
                .build().toOakMachineBuilder(null, Util.getDefaultClassLoader());
    }

    @Test
    public void testRelativizeToBaseParent() throws Exception {
        final URI fooOpaque = new URI("mailto:foo@bar.com");
        final URI barOpaque = new URL("http://bar.com").toURI();
        assertSame("return uri when opaque",
                barOpaque, OakpalPlan.relativizeToBaseParent(fooOpaque, barOpaque));

        final URI barOneJson = new URL("http://bar.com/one.json").toURI();
        final URI barTwoJson = new URL("http://bar.com/two.json").toURI();
        final URI barTwoRel = new URI("two.json");
        assertEquals("return relative uri when base is not json",
                barTwoRel, OakpalPlan.relativizeToBaseParent(barOneJson, barTwoJson));

        final URI fooRoot = new URL("http://foo.com/").toURI();
        final URI fooAbs = new URL("http://foo.com/file.json").toURI();
        final URI fooRel = new URI("file.json");
        assertEquals("return relative uri when base is not json",
                fooRel, OakpalPlan.relativizeToBaseParent(fooRoot, fooAbs));
    }

    @Test
    public void testInitResourceBundle() throws Exception {
        CompletableFuture<ResourceBundle> callback = new CompletableFuture<>();
        OakpalPlan plan = builder().build();
        ProgressCheck check = mock(ProgressCheck.class);
        doAnswer(call -> callback.complete(call.getArgument(0)))
                .when(check).setResourceBundle(nullable(ResourceBundle.class));
        when(check.getResourceBundleBaseName()).thenReturn(null);
        plan.initResourceBundle(check, Locale.getDefault(), getClass().getClassLoader());
        assertFalse("expect callback not done", callback.isDone());
        when(check.getResourceBundleBaseName()).thenReturn(getClass().getName());
        plan.initResourceBundle(check, Locale.getDefault(), getClass().getClassLoader());
        assertSame("expect callback complete with", ResourceBundle.getBundle(getClass().getName()),
                callback.getNow(null));

    }
}