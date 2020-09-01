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

package net.adamcin.oakpal.core.sling;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.Nothing;
import net.adamcin.oakpal.api.OsgiConfigInstallable;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.SlingOpenable;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.installer.api.InstallableResource;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.core.OakpalPlan.keys;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultSlingSimulatorTest {
    private DefaultSlingSimulator slingSimulator = new DefaultSlingSimulator();

    @Test
    public void testReadInstallableResourceFromNode_noRepositoryException() throws Exception {

        OakpalPlan.fromJson(key(keys().repoInits(),
                arr().val("create path (nt:unstructured) /apps/config/Test")).get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            Result<Optional<SlingInstallableParams<?>>> result = slingSimulator
                    .readInstallableParamsFromNode(session.getNode("/apps/config/Test"));
            assertTrue("expect null config", result.isSuccess() && !result.getOrDefault(null).isPresent());
        });
    }


    @Test
    public void testReadInstallableResourceFromNode_slingOsgiConfig() throws Exception {
        OakpalPlan.fromJson(key(keys().repoInits(), arr()
                .val("register nodetypes")
                .val("<<===")
                .val("<'sling'='http://sling.apache.org/jcr/sling/1.0'>")
                .val("[sling:OsgiConfig] > nt:unstructured, nt:hierarchyNode")
                .val("===>>")
                .val("create path (nt:folder) /apps/config/Test(sling:OsgiConfig)"))
                .get()).toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            SlingInstallableParams<?> resource = slingSimulator
                    .readInstallableParamsFromNode(session.getNode("/apps/config/Test")).toOptional()
                    .flatMap(Function.identity())
                    .orElse(null);
            assertNotNull("expect not null resource", resource);
            assertTrue("expect instance of OsgiConfigInstallableParams",
                    resource instanceof OsgiConfigInstallableParams);
            OsgiConfigInstallableParams params = (OsgiConfigInstallableParams) resource;
            assertNotNull("expect not null properties", params.getProperties());
            assertEquals("expect servicePid is Test", "Test", params.getServicePid());

            PackageId base = new PackageId("com.test", "base", "1.0.0");
            OsgiConfigInstallable installable = params.createInstallable(base, "/apps/config/Test");

        });
    }

    @Test
    public void testSeparatorsToUnix() {
        assertNull("expect null path", DefaultSlingSimulator.separatorsToUnix(null));
        assertEquals("expect new path", "/some/path", DefaultSlingSimulator.separatorsToUnix("\\some\\path"));
        assertEquals("expect same path", "/some/path", DefaultSlingSimulator.separatorsToUnix("/some/path"));
    }

    @Test
    public void testGetResourceId() {
        final Map<String, String> expectResultsForIds = new LinkedHashMap<>();
        expectResultsForIds.put("/test.config", "test.config");
        expectResultsForIds.put("http:test.properties", "test.properties");
        expectResultsForIds.put("test.jar", "test.jar");
        expectResultsForIds.put("test.zip", "test.zip");
        for (Map.Entry<String, String> entry : expectResultsForIds.entrySet()) {
            assertEquals("expect result for id: " + entry.getKey(), entry.getValue(),
                    DefaultSlingSimulator.getResourceId(entry.getKey()));
        }
    }

    @Test
    public void testRemoveConfigExtension() {
        final Map<String, String> expectResultsForIds = new LinkedHashMap<>();
        expectResultsForIds.put("test.config", "test");
        expectResultsForIds.put("test.properties", "test");
        expectResultsForIds.put("test.jar", "test.jar");
        expectResultsForIds.put("test.zip", "test.zip");
        for (Map.Entry<String, String> entry : expectResultsForIds.entrySet()) {
            assertEquals("expect result for id: " + entry.getKey(), entry.getValue(),
                    DefaultSlingSimulator.removeConfigExtension(entry.getKey()));
        }
    }

    @Test
    public void testIsConfigExtension() {
        final Map<String, Boolean> expectBooleansForIds = new LinkedHashMap<>();
        expectBooleansForIds.put("test.config", true);
        expectBooleansForIds.put("test.properties", true);
        expectBooleansForIds.put("test.jar", false);
        expectBooleansForIds.put("test.zip", false);
        for (Map.Entry<String, Boolean> entry : expectBooleansForIds.entrySet()) {
            assertEquals("expect boolean for id: " + entry.getKey(), entry.getValue(),
                    DefaultSlingSimulator.isConfigExtension(entry.getKey()));
        }
    }

    void checkExpectedProperties(final Map<String, Object> expectProps, final Map<String, Object> props) {
        assertEquals("expect same keys", expectProps.keySet(), props.keySet());
        for (Map.Entry<String, Object> entry : expectProps.entrySet()) {
            Object expectValue = entry.getValue();
            if (expectValue.getClass().isArray()) {
                assertArrayEquals("expect equal array for key " + entry.getKey(), (Object[]) expectValue,
                        (Object[]) props.get(entry.getKey()));
            } else {
                assertEquals("expect equal value for key " + entry.getKey(), expectValue,
                        props.get(entry.getKey()));
            }
        }
    }

    @Test(expected = IOException.class)
    public void testReadDictionary_onlyCommentLineThrows() throws Exception {
        final String simpleConfigWithComment = "# some comment";
        DefaultSlingSimulator.readDictionary(
                new ByteArrayInputStream(simpleConfigWithComment.getBytes(StandardCharsets.UTF_8)), "simple.config");
    }

    @Test
    public void testReadDictionary() throws Exception {
        final Map<String, Object> expectConfig = new HashMap<>();
        expectConfig.put("foo", "bar");
        expectConfig.put("foos", Stream.of("bar", "bar", "bar").toArray(String[]::new));
        expectConfig.put("ones", Stream.of(1L, 1L, 1L).toArray(Long[]::new));
        expectConfig.put("nothing", new String[0]);

        final String dotConfig = "foo=\"bar\"\n" +
                "foos=[\"bar\",\"bar\",\"bar\"]\n" +
                "ones=L[\"1\",\"1\",\"1\"]\n" +
                "nothing=[]";

        final Map<String, Object> simpleConfig = DefaultSlingSimulator.readDictionary(
                new ByteArrayInputStream(dotConfig.getBytes(StandardCharsets.UTF_8)), "simple.config");
        checkExpectedProperties(expectConfig, simpleConfig);

        final String dotConfigWithComment = "# some comment\n" + dotConfig;
        final Map<String, Object> simpleConfigWithComment = DefaultSlingSimulator.readDictionary(
                new ByteArrayInputStream(dotConfigWithComment.getBytes(StandardCharsets.UTF_8)), "simple.config");
        checkExpectedProperties(expectConfig, simpleConfigWithComment);

        final Map<String, Object> expectJsonConfig = new HashMap<>();
        expectJsonConfig.put("foo", "bar");
        expectJsonConfig.put("foos", Stream.of("bar", "bar", "bar").toArray(String[]::new));
        expectJsonConfig.put("ones", Stream.of(1L, 1L, 1L).toArray(Long[]::new));

        final String cfgJson = "{\"foo\":\"bar\",\"foos\":[\"bar\",\"bar\",\"bar\"],\"ones\":[1,1,1]}";
        final Map<String, Object> cfgJsonProperties = DefaultSlingSimulator.readDictionary(
            new ByteArrayInputStream(cfgJson.getBytes(StandardCharsets.UTF_8)), "simple.cfg.json");
        checkExpectedProperties(expectJsonConfig, cfgJsonProperties);

        final Map<String, Object> cfgJsonPropertiesFactory = DefaultSlingSimulator.readDictionary(
            new ByteArrayInputStream(cfgJson.getBytes(StandardCharsets.UTF_8)), "simple-test.cfg.json");
        checkExpectedProperties(expectJsonConfig, cfgJsonPropertiesFactory);

        final Map<String, Object> expectProperties = new HashMap<>();
        expectProperties.put("foo", "bar");
        expectProperties.put("foos", "bar,bar,bar");
        expectProperties.put("ones", "1,1,1");
        expectProperties.put("nothing", "");

        final String dotProperties = "foo=bar\nfoos=bar,bar,bar\nones=1,1,1\nnothing=";
        final Map<String, Object> simpleProperties = DefaultSlingSimulator.readDictionary(
                new ByteArrayInputStream(dotProperties.getBytes(StandardCharsets.UTF_8)), "simple.properties");
        checkExpectedProperties(expectProperties, simpleProperties);

        final Map<String, Object> cfgSimpleProperties = DefaultSlingSimulator.readDictionary(
            new ByteArrayInputStream(dotProperties.getBytes(StandardCharsets.UTF_8)), "simple.cfg");
        checkExpectedProperties(expectProperties, cfgSimpleProperties);

        ByteArrayOutputStream propsXmlBytes = new ByteArrayOutputStream();
        Properties srcPropertiesXml = new Properties();
        srcPropertiesXml.putAll(expectProperties);
        srcPropertiesXml.storeToXML(propsXmlBytes, "generated by test", "UTF-8");
        final String dotPropertiesXml = new String(propsXmlBytes.toByteArray(), StandardCharsets.UTF_8);
        final Map<String, Object> simplePropertiesXml = DefaultSlingSimulator.readDictionary(
                new ByteArrayInputStream(dotPropertiesXml.getBytes(StandardCharsets.UTF_8)), "simple.xml");
        checkExpectedProperties(expectProperties, simplePropertiesXml);

    }

    @Test
    public void testLoadJcrProperties() throws Exception {
        final Map<String, Object> props = new HashMap<>();
        OakpalPlan.fromJson(key(keys().repoInits(), arr()
                .val("register nodetypes")
                .val("<<===")
                .val("<'sling'='http://sling.apache.org/jcr/sling/1.0'>")
                .val("[sling:OsgiConfig] > nt:unstructured, nt:hierarchyNode")
                .val("===>>")
                .val("create path (nt:folder) /apps/config/Test(sling:OsgiConfig)")
                .val("set properties on /apps/config/Test")
                .val("  set sling:ResourceType{String} to /x/y/z")
                .val("  set foo{String} to bar")
                .val("  set foos to bar, bar, bar")
                .val("  set ones{Long} to 1, 1, 1")
                .val("end")
        ).get()).toOakMachineBuilder(null, getClass().getClassLoader())
                .build().adminInitAndInspect(session -> {
            Node testNode = session.getNode("/apps/config/Test");
            testNode.setProperty("nothing", new String[0]);
            session.save();
            DefaultSlingSimulator.loadJcrProperties(props, testNode);
        });

        final Map<String, Object> expectProps = new HashMap<>();
        expectProps.put("foo", "bar");
        expectProps.put("foos", Stream.of("bar", "bar", "bar").toArray(String[]::new));
        expectProps.put("ones", Stream.of(1L, 1L, 1L).toArray(Long[]::new));
        expectProps.put("nothing", new String[0]);

        assertEquals("expect same keys", expectProps.keySet(), props.keySet());
        for (Map.Entry<String, Object> entry : expectProps.entrySet()) {
            Object expectValue = entry.getValue();
            if (expectValue.getClass().isArray()) {
                assertArrayEquals("expect equal array for key " + entry.getKey(), (Object[]) expectValue,
                        (Object[]) props.get(entry.getKey()));
            } else {
                assertEquals("expect equal value for key " + entry.getKey(), expectValue,
                        props.get(entry.getKey()));
            }
        }
    }

    @Test
    public void testConvertJcrValue() throws Exception {
        ValueFactory vf = ValueFactoryImpl.getInstance();
        final String expectString = "hey I'm a string";
        assertEquals("expect equal string", expectString,
                DefaultSlingSimulator.convertJcrValue(vf.createValue(expectString)));
        final Calendar expectDate = Calendar.getInstance();
        assertEquals("expect equal date", expectDate,
                DefaultSlingSimulator.convertJcrValue(vf.createValue(expectDate)));
        final double expectDouble = 42.0D;
        assertEquals("expect equal double", expectDouble,
                (Double) DefaultSlingSimulator.convertJcrValue(vf.createValue(expectDouble)), 1.0D);
        final long expectLong = 404L;
        assertEquals("expect equal long", expectLong,
                DefaultSlingSimulator.convertJcrValue(vf.createValue(expectLong)));
        final Boolean expectBoolean = Boolean.TRUE;
        assertEquals("expect equal boolean", expectBoolean,
                DefaultSlingSimulator.convertJcrValue(vf.createValue(expectBoolean)));
        assertNull("expect null for name",
                DefaultSlingSimulator.convertJcrValue(vf.createValue("aName", PropertyType.NAME)));
    }


    @Test
    public void testMaybeConfigResource_factoryHyphen() throws Exception {
        String configPath = "/apps/install/com.Test-pid";
        OakpalPlan.fromJson(key(keys().repoInits(),
                arr().val(String.format("create path (nt:unstructured) %s", configPath))).get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {
            DefaultSlingSimulator.NodeRes nodeRes = new DefaultSlingSimulator.NodeRes(
                    session.getNode(configPath), configPath);
            OsgiConfigInstallableParams params = DefaultSlingSimulator.maybeConfigResource(nodeRes);
            assertNotNull("expect nonnull config params", params);

            assertEquals("expect servicePid", "pid", params.getServicePid());
            assertEquals("expect factoryPid", "com.Test", params.getFactoryPid());
        });
    }

    @Test
    public void testMaybeConfigResource_factoryTilde() throws Exception {
        String configPath = "/apps/install/com.Test~pid";
        OakpalPlan.fromJson(obj().get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().adminInitAndInspect(session -> {
            DefaultSlingSimulator.NodeRes nodeRes = new DefaultSlingSimulator.NodeRes(
                    JcrUtils.getOrCreateByPath(configPath, "nt:unstructured", session), configPath);
            OsgiConfigInstallableParams params = DefaultSlingSimulator.maybeConfigResource(nodeRes);
            assertNotNull("expect nonnull config params", params);

            assertEquals("expect servicePid", "pid", params.getServicePid());
            assertEquals("expect factoryPid", "com.Test", params.getFactoryPid());
        });
    }

    @Test
    public void testMaybeConfigResource_singleton() throws Exception {
        String configPath = "/apps/install/com.Test";
        OakpalPlan.fromJson(key(keys().repoInits(),
                arr().val(String.format("create path (nt:unstructured) %s", configPath))).get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {
            DefaultSlingSimulator.NodeRes nodeRes = new DefaultSlingSimulator.NodeRes(
                    session.getNode(configPath), configPath);
            OsgiConfigInstallableParams params = DefaultSlingSimulator.maybeConfigResource(nodeRes);
            assertNotNull("expect nonnull config params", params);

            assertEquals("expect servicePid", "com.Test", params.getServicePid());
            assertNull("expect null factoryPid", params.getFactoryPid());
        });
    }

    @Test
    public void testMaybePackageResource_noNullPointerException() throws Exception {
        String packagePath = "/apps/install/Test";
        slingSimulator.setPackageManager(null);
        OakpalPlan.fromJson(key(keys().repoInits(),
                arr().val(String.format("create path (nt:unstructured) %s", packagePath))).get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            DefaultSlingSimulator.NodeRes nodeRes = new DefaultSlingSimulator.NodeRes(
                    session.getNode(packagePath), packagePath);
            EmbeddedPackageInstallableParams result = slingSimulator
                    .maybePackageResource(nodeRes);

            assertNull("expect null result (not NullPointerException)", result);
        });
    }


    @Test
    public void testMaybePackageResource_noRepositoryException() throws Exception {
        String packagePath = "/apps/install/Test.zip";
        JcrPackageManager packageManager = mock(JcrPackageManager.class);
        when(packageManager.open(argThat(nodeWithPath(packagePath)), eq(true)))
                .thenThrow(RepositoryException.class);
        slingSimulator.setPackageManager(packageManager);
        OakpalPlan.fromJson(key(keys().repoInits(),
                arr().val(String.format("create path (nt:unstructured) %s", packagePath))).get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            DefaultSlingSimulator.NodeRes nodeRes = new DefaultSlingSimulator.NodeRes(
                    session.getNode(packagePath), packagePath);
            EmbeddedPackageInstallableParams result = slingSimulator
                    .maybePackageResource(nodeRes);

            assertNull("expect null result (not RepositoryException)", result);
        });
    }

    @Test
    public void testReadInstallableResourceFromNode_fileConfig() throws Exception {
        final String packagePath = "/apps/with-embedded/config/com.TestFactory-strawberry.config";

        TestPackageUtil.deleteTestPackage("with-embedded-config.zip");
        final File withEmbeddedConfig = TestPackageUtil.prepareTestPackageFromFolder("with-embedded-config.zip",
                new File("target/test-classes/with-embedded-package"));

        JcrPackageManager packageManager = mock(JcrPackageManager.class);
        slingSimulator.setPackageManager(packageManager);

        // can't use OakpalPlan.fromJson here because pre install urls only work if there's a base URL
        new OakpalPlan.Builder(new URL("https://github.com/adamcin/oakpal"), null)
                .withPreInstallUrls(Collections.singletonList(withEmbeddedConfig.toURI().toURL()))
                .build().toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            slingSimulator.setSession(session);

            SlingInstallableParams<?> resource = slingSimulator
                    .readInstallableParamsFromNode(session.getNode(packagePath)).toOptional()
                    .flatMap(Function.identity())
                    .orElse(null);
            assertNotNull("expect not null resource", resource);
            assertTrue("expect instance of OsgiConfigInstallableParams",
                    resource instanceof OsgiConfigInstallableParams);
            OsgiConfigInstallableParams params = (OsgiConfigInstallableParams) resource;

            PackageId base = new PackageId("com.test", "base", "1.0.0");
            OsgiConfigInstallable installable = params.createInstallable(base, packagePath);

            assertNotNull("expect not null installable", installable);
            assertEquals("expect base package Id", base, installable.getParentId());
            assertEquals("expect installable path", packagePath, installable.getJcrPath());
            assertEquals("expect servicePid", "strawberry", installable.getServicePid());
            assertEquals("expect factoryPid", "com.TestFactory", installable.getFactoryPid());


            final Map<String, Object> expectProps = new HashMap<>();
            expectProps.put(InstallableResource.INSTALLATION_HINT, "config");
            expectProps.put("foo", "bar");
            expectProps.put("foos", Stream.of("bar", "bar", "bar").toArray(String[]::new));
            expectProps.put("ones", Stream.of(1L, 1L, 1L).toArray(Long[]::new));
            expectProps.put("nothing", new String[0]);

            checkExpectedProperties(expectProps, installable.getProperties());
        });
    }

    @Test
    public void testReadInstallableResourceFromNode_package() throws Exception {
        // first prepare the embedded file, which is copied to the test packages root directory with the given filename
        final File embeddedPackageFile = TestPackageUtil.prepareTestPackage("package_1.0.zip");
        // declare the path inside the embedding package
        final String packagePath = "/apps/with-embedded/install/package_1.0.zip";
        // prepare the outer package, passing the embedded package zip entry name and prepared File location as a map
        // of additional entries.
        final File withEmbeddedPackage = TestPackageUtil.prepareTestPackageFromFolder("with-embedded-package.zip",
                new File("target/test-classes/with-embedded-package"),
                Collections.singletonMap("jcr_root" + packagePath, embeddedPackageFile));

        VaultPackage vaultPackage = mock(VaultPackage.class);
        PackageId embeddedId = new PackageId("com.test", "embedded", "1.0");
        when(vaultPackage.getId()).thenReturn(embeddedId);

        JcrPackage jcrPackageFromOpen = mock(JcrPackage.class);
        when(jcrPackageFromOpen.getPackage()).thenReturn(vaultPackage);
        JcrPackage jcrPackageFromUpload = mock(JcrPackage.class);
        when(jcrPackageFromUpload.getPackage()).thenReturn(vaultPackage);

        JcrPackageManager packageManager = mock(JcrPackageManager.class);
        when(packageManager.open(argThat(nodeWithPath(packagePath)), eq(true))).thenReturn(jcrPackageFromOpen);
        when(packageManager.upload(any(InputStream.class), eq(true), eq(true))).thenReturn(jcrPackageFromUpload);

        slingSimulator.setPackageManager(packageManager);

        // can't use OakpalPlan.fromJson here because pre install urls only work if there's a base URL
        new OakpalPlan.Builder(new URL("https://github.com/adamcin/oakpal"), null)
                .withPreInstallUrls(Collections.singletonList(withEmbeddedPackage.toURI().toURL()))
                .build().toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            slingSimulator.setSession(session);

            SlingInstallableParams<?> resource = slingSimulator
                    .readInstallableParamsFromNode(session.getNode(packagePath)).toOptional()
                    .flatMap(Function.identity())
                    .orElse(null);
            assertNotNull("expect not null resource", resource);
            assertTrue("expect instance of EmbeddedPackageInstallableParams",
                    resource instanceof EmbeddedPackageInstallableParams);
            EmbeddedPackageInstallableParams params = (EmbeddedPackageInstallableParams) resource;

            PackageId base = new PackageId("com.test", "base", "1.0.0");
            EmbeddedPackageInstallable installable = params.createInstallable(base, packagePath);

            assertNotNull("expect not null installable", installable);
            assertEquals("expect base package Id", base, installable.getParentId());
            assertEquals("expect installable path", packagePath, installable.getJcrPath());
            assertEquals("expect installable id", embeddedId, installable.getEmbeddedId());

            Fun.ThrowingSupplier<JcrPackage> opened = slingSimulator.open(installable);
            assertNotNull("expect not null function", opened);
            JcrPackage openedPackage = opened.tryGet();
            assertEquals("open returned correct package", jcrPackageFromUpload, openedPackage);

            verify(packageManager, times(1)).open(argThat(nodeWithPath(packagePath)), eq(true));
            verify(packageManager, times(1)).upload(any(InputStream.class), eq(true), eq(true));
            verifyNoMoreInteractions(packageManager);
        });
    }

    static class UnsupportedSlingOpenable implements SlingOpenable<Nothing> {
        private final PackageId parentId;
        private final String jcrPath;

        public UnsupportedSlingOpenable(final PackageId parentId, final String jcrPath) {
            this.parentId = parentId;
            this.jcrPath = jcrPath;
        }

        @Override
        public @NotNull PackageId getParentId() {
            return parentId;
        }

        @Override
        public @NotNull String getJcrPath() {
            return jcrPath;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOpen_unsupported() throws Exception {
        slingSimulator.open(new UnsupportedSlingOpenable(PackageId.fromString("test"), "/test/installable")).tryGet();
    }

    private static ArgumentMatcher<Node> nodeWithPath(String path) {
        return new ArgumentMatcher<Node>() {
            @Override
            public boolean matches(Node node) {
                try {
                    return path.equals(node.getPath());
                } catch (RepositoryException e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return path;
            }
        };
    }

}