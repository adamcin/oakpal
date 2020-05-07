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

package net.adamcin.oakpal.webster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.Blob;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.junit.Test;

public class JcrFactoryTest {
    final File testBaseDir = new File("target/repos/JcrFactoryTest");

    @Test
    public void testGetJcr() throws Exception {
        final File seedDir = new File(testBaseDir, "testGetJcr/seedRepo/segmentstore");
        final File globalDir = new File(testBaseDir, "testGetJcr/globalRepo/segmentstore");

        final String superPath = "/apps/myapp/components/sup1";
        final String resultPath = "/apps/myapp/components/res1";

        TestUtil.prepareRepo(seedDir, session -> {
            final URL slingNodetypes = getClass().getResource("/sling_nodetypes.cnd");
            TestUtil.installCndFromURL(session, slingNodetypes);

            Node sup1 = JcrUtils.getOrCreateByPath(superPath, "sling:Folder", session);
            Node res1 = JcrUtils.getOrCreateByPath(resultPath, "sling:Folder", session);
            res1.addMixin("sling:ResourceSuperType");
            res1.setProperty("sling:resourceSuperType", superPath);
            session.save();
        });

        TestUtil.withReadOnlyFixture(seedDir, session -> {
            QueryManager qm = session.getWorkspace().getQueryManager();

            final String stmt = "select * from [sling:ResourceSuperType] as a OPTION(TRAVERSAL FAIL, INDEX NAME nodetype)";
            final Query query = qm.createQuery(stmt, Query.JCR_SQL2);
            QueryResult result = query.execute();
            final NodeIterator it = result.getNodes();
            assertTrue("Result hasNext()", it.hasNext());
            final Node next = it.nextNode();
            assertFalse("Result has no more", it.hasNext());
            assertEquals("Path is correct", resultPath, next.getPath());
            assertEquals("sling:resourceSuperType value is correct", superPath,
                    next.getProperty("sling:resourceSuperType").getString());
        });
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetReadOnlyFixture() throws Exception {
        final File seedDir = new File(testBaseDir, "testGetReadOnlyFixture/seedRepo/segmentstore");
        TestUtil.prepareRepo(seedDir, session -> {
            session.getRootNode().addNode("foo", "nt:unstructured");
            session.save();
        });
        final File globalDir = new File(testBaseDir, "testGetReadOnlyFixture/globalRepo/segmentstore");
        TestUtil.prepareRepo(globalDir, session -> {});

        try (NodeStoreFixture fixture = JcrFactory.getReadOnlyFixture(seedDir);
             NodeStoreFixture globalFixture = JcrFactory.getReadOnlyFixture(globalDir)) {
            TestUtil.compositeWithFixtures(fixture, globalFixture, session -> {
                session.getWorkspace().getNamespaceRegistry()
                        .registerNamespace("foo", "http://foo.com");
            });
        }
    }

    @Test
    public void testGetReadWriteFixture() throws Exception {
        final File seedDir = new File(testBaseDir, "testGetReadWriteFixture/seedRepo/segmentstore");
        TestUtil.prepareRepo(seedDir, session -> {
            session.getRootNode().addNode("foo", "nt:unstructured");
            session.save();
        });
        final File globalDir = new File(testBaseDir, "testGetReadWriteFixture/globalRepo/segmentstore");
        TestUtil.prepareRepo(globalDir, session -> {});

        try (NodeStoreFixture fixture = JcrFactory.getReadWriteFixture(seedDir);
             NodeStoreFixture globalFixture = JcrFactory.getReadWriteFixture(globalDir)) {
            TestUtil.compositeWithFixtures(fixture, globalFixture, session -> {
                session.getWorkspace().getNamespaceRegistry()
                        .registerNamespace("foo", "http://foo.com");
            });
        }
    }

    private static void recursiveDeleteWithRetry(final File toDelete) throws IOException {
        try {
            FileUtils.deleteDirectory(toDelete);
        } catch (IOException e) {
            // retry if failed.
            if (toDelete.exists()) {
                FileUtils.deleteDirectory(toDelete);
            }
        }
    }

    @Test
    public void testGetNodeStoreFixture_withWithoutFDS() throws Exception {
        final File seedRepoHome = new File(testBaseDir, "testGetNodeStoreFixture_withFDS/seedRepo");
        recursiveDeleteWithRetry(seedRepoHome);
        final File seedDir = new File(seedRepoHome, "segmentstore");
        final File fdsDir = new File(seedRepoHome, "datastore");

        assertFalse("fds dir should not exist @ " + fdsDir.getAbsolutePath(), fdsDir.exists());
        try (NodeStoreFixture fixture = JcrFactory.getNodeStoreFixture(false, seedDir)) {
            assertNull("blob store should be null without datastore dir", fixture.getBlobStore());
        }

        recursiveDeleteWithRetry(seedRepoHome);
        fdsDir.mkdirs();
        assertTrue("fds dir should exist @ " + fdsDir.getAbsolutePath(), fdsDir.exists());
        try (NodeStoreFixture fixture = JcrFactory.getNodeStoreFixture(false, seedDir)) {
            assertNotNull("blob store should not be null", fixture.getBlobStore());
        }
    }
}
