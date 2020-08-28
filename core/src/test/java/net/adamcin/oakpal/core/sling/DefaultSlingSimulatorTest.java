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
import net.adamcin.oakpal.api.OsgiConfigInstallable;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.core.OakpalPlan.keys;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

            Fun.ThrowingSupplier<Map<String, Object>> opened = slingSimulator.open(installable);
            assertNotNull("expect not null function", opened);

            boolean thrown = false;
            try {
                opened.tryGet();
            } catch (IllegalArgumentException e) {
                thrown = true;
            }
            assertTrue("expect exception opening OsgiConfigInstallable", thrown);
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