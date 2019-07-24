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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.JcrPackageDefinitionImpl;
import org.apache.jackrabbit.vault.packaging.impl.JcrPackageManagerImpl;
import org.junit.Test;

public class DefaultPackagingServiceTest {

    @Test
    public void testGetPackageManagerNoSession() {
        assertNotNull("got package manager", new DefaultPackagingService().getPackageManager());
    }

    @Test
    public void testConstruct() throws Exception {
        DefaultPackagingService service = new DefaultPackagingService();
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
        });
    }

    @Test
    public void testConstructNoDelegation() throws Exception {
        DefaultPackagingService service = new DefaultPackagingService(new URLClassLoader(new URL[0], null));
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
        });
    }

    @Test
    public void testConstructWrongInterfaceClassLoader() throws Exception {
        final URLClassLoader classLoader = new URLClassLoader(
                new URL[]{JcrPackageManager.class.getProtectionDomain().getCodeSource().getLocation()}, null);
        DefaultPackagingService service = new DefaultPackagingService(classLoader);
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
        });
    }

    @Test
    public void testExceptionHandlingWithReflection() throws Exception {
        DefaultPackagingService service = new DefaultPackagingService();
        service.jcrPackageManagerClazz = FailingJcrPackageManager.class;
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
            assertTrue(manager instanceof JcrPackageManagerImpl);
        });
    }

    @Test
    public void testCreatePackageDefinition() throws Exception {
        final DefaultPackagingService service = new DefaultPackagingService();
        File testPack = TestPackageUtil.prepareTestPackage("test-package-with-etc.zip");
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
            JcrPackage jcrPackage = null;
            try {
                jcrPackage = manager.upload(testPack, false, true, null);
                JcrPackageDefinition def = service.createPackageDefinition(jcrPackage.getDefNode());
                assertTrue("should be real definition", def instanceof JcrPackageDefinitionImpl);
            } finally {
                if (jcrPackage != null) {
                    jcrPackage.close();
                }
            }
        });
    }

    @Test
    public void testOpen() throws Exception {
        final DefaultPackagingService service = new DefaultPackagingService();
        File testPack = TestPackageUtil.prepareTestPackage("test-package-with-etc.zip");
        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            JcrPackageManager manager = service.getPackageManager(session);
            JcrPackage uploaded = null;
            JcrPackage opened = null;
            try {
                uploaded = manager.upload(testPack, false, true, null);
                opened = service.open(uploaded.getNode(), false);
                assertTrue("should be valid package", opened.isValid());
            } finally {
                if (uploaded != null) {
                    uploaded.close();
                }
                if (opened != null) {
                    opened.close();
                }
            }
        });
    }

    /**
     * This implementation does not have a matching constructor for reflection,
     * so it should trigger an exception forcing the use of the default
     * {@link org.apache.jackrabbit.vault.packaging.PackagingService#getPackageManager(Session)}.
     */
    private static class FailingJcrPackageManager implements JcrPackageManager {
        @Override
        public JcrPackage open(final PackageId id) throws RepositoryException {
            return null;
        }

        @Override
        public JcrPackage open(final Node node) throws RepositoryException {
            return null;
        }

        @Override
        public JcrPackage open(final Node node, final boolean allowInvalid) throws RepositoryException {
            return null;
        }

        @Override
        public PackageId resolve(final Dependency dependency, final boolean onlyInstalled) throws RepositoryException {
            return null;
        }

        @Override
        public PackageId[] usage(final PackageId id) throws RepositoryException {
            return new PackageId[0];
        }

        @Override
        public JcrPackage upload(final File file, final boolean isTmpFile, final boolean replace, final String nameHint) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public JcrPackage upload(final File file, final boolean isTmpFile, final boolean replace, final String nameHint, final boolean strict) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public JcrPackage upload(final InputStream in, final boolean replace) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public JcrPackage upload(final InputStream in, final boolean replace, final boolean strict) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public JcrPackage create(final Node folder, final String name) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public JcrPackage create(final String group, final String name) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public JcrPackage create(final String group, final String name, final String version) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public PackageId[] extract(final Archive archive, final ImportOptions options, final boolean replace) throws RepositoryException, PackageException, IOException {
            return new PackageId[0];
        }

        @Override
        public void remove(final JcrPackage pack) throws RepositoryException {

        }

        @Override
        public JcrPackage rename(final JcrPackage pack, final String groupId, final String name) throws PackageException, RepositoryException {
            return null;
        }

        @Override
        public JcrPackage rename(final JcrPackage pack, final String groupId, final String name, final String version) throws PackageException, RepositoryException {
            return null;
        }

        @Override
        public void assemble(final JcrPackage pack, final ProgressTrackerListener listener) throws PackageException, RepositoryException, IOException {

        }

        @Override
        public void assemble(final Node packNode, final JcrPackageDefinition definition, final ProgressTrackerListener listener) throws PackageException, RepositoryException, IOException {

        }

        @Override
        public void assemble(final JcrPackageDefinition definition, final ProgressTrackerListener listener, final OutputStream out) throws IOException, RepositoryException, PackageException {

        }

        @Override
        public void rewrap(final JcrPackage pack, final ProgressTrackerListener listener) throws PackageException, RepositoryException, IOException {

        }

        @Override
        public Node getPackageRoot() throws RepositoryException {
            return null;
        }

        @Override
        public Node getPackageRoot(final boolean noCreate) throws RepositoryException {
            return null;
        }

        @Override
        public List<JcrPackage> listPackages() throws RepositoryException {
            return null;
        }

        @Override
        public List<JcrPackage> listPackages(final WorkspaceFilter filter) throws RepositoryException {
            return null;
        }

        @Override
        public List<JcrPackage> listPackages(final String group, final boolean built) throws RepositoryException {
            return null;
        }

        @Override
        public VaultPackage open(final File file) throws IOException {
            return null;
        }

        @Override
        public VaultPackage open(final File file, final boolean strict) throws IOException {
            return null;
        }

        @Override
        public VaultPackage assemble(final Session s, final ExportOptions opts, final File file) throws IOException, RepositoryException {
            return null;
        }

        @Override
        public void assemble(final Session s, final ExportOptions opts, final OutputStream out) throws IOException, RepositoryException {

        }

        @Override
        public VaultPackage rewrap(final ExportOptions opts, final VaultPackage src, final File file) throws IOException, RepositoryException {
            return null;
        }

        @Override
        public void rewrap(final ExportOptions opts, final VaultPackage src, final OutputStream out) throws IOException {

        }
    }
}