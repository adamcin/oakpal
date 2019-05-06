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

package net.adamcin.oakpal.toolslib;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.junit.Test;

public class PrivilegeXmlExporterTest {

    final File testBaseDir = new File("target/repos/PrivilegeXmlExporterTest");

    @Test
    public void testWritePrivileges() throws Exception {
                final File tempDir = new File(testBaseDir, "testWritePrivileges");
        final File fromRepoDir = new File(tempDir, "fromRepo/segmentstore");
        final File toRepoDir = new File(tempDir, "toRepo/segmentstore");
        final File exportedXml = new File(tempDir, "exported.xml");

        TestUtil.prepareRepo(fromRepoDir, session -> {
            final URL aemPrivileges = getClass().getResource("/aem_privileges.xml");
            TestUtil.installPrivilegesFromURL(session, aemPrivileges);
        });

        if (exportedXml.exists()) {
            exportedXml.delete();
        }

        try (NodeStoreFixture fixture = TestUtil.getReadOnlyFixture(fromRepoDir, null)) {
            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture);
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));

                final Workspace workspace = session.getWorkspace();
                final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) workspace).getPrivilegeManager();

                assertNotNull("crx:replicate should be imported",
                        privilegeManager.getPrivilege("crx:replicate"));
                assertNotNull("cq:storeUGC should be imported",
                        privilegeManager.getPrivilege("cq:storeUGC"));

                try (Writer writer = new OutputStreamWriter(new FileOutputStream(exportedXml))) {
                    PrivilegeXmlExporter.writePrivileges(writer, session, Arrays.asList("crx:replicate"), false);
                }

            } finally {
                if (session != null) {
                    session.logout();
                }
                TestUtil.closeRepo(repo);
            }
        }

        TestUtil.prepareRepo(toRepoDir, session -> {
            final URL exportedUrl = exportedXml.toURL();
            TestUtil.installPrivilegesFromURL(session, exportedUrl);
        });

        try (NodeStoreFixture fixture = TestUtil.getReadOnlyFixture(toRepoDir, null)) {

            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture);
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));

                final Workspace workspace = session.getWorkspace();
                final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) workspace).getPrivilegeManager();

                assertNotNull("crx:replicate should be imported",
                        privilegeManager.getPrivilege("crx:replicate"));
                boolean thrown = false;
                try {
                    privilegeManager.getPrivilege("cq:storeUGC");
                } catch (Exception e) {
                    thrown = true;
                }
                assertTrue("cq:storeUGC should NOT be imported", thrown);
            } finally {
                if (session != null) {
                    session.logout();
                }
                TestUtil.closeRepo(repo);
            }
        }
    }
}
