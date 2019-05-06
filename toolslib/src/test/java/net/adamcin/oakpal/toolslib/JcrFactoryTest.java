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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.junit.Test;

public class JcrFactoryTest {
    final File testBaseDir = new File("target/repos/JcrFactoryTest");

    @Test
    public void testGetJcr() throws Exception {
        final File repoDir = new File(testBaseDir, "testGetJcr/segmentstore");

        final String superPath = "/apps/myapp/components/sup1";
        final String resultPath = "/apps/myapp/components/res1";
        TestUtil.prepareRepo(repoDir, session -> {
            final URL slingNodetypes = getClass().getResource("/sling_nodetypes.cnd");
            TestUtil.installCndFromURL(session, slingNodetypes);

            Node sup1 = JcrUtils.getOrCreateByPath(superPath, "sling:Folder", session);
            Node res1 = JcrUtils.getOrCreateByPath(resultPath, "sling:Folder", session);
            res1.addMixin("sling:ResourceSuperType");
            res1.setProperty("sling:resourceSuperType", superPath);
            session.save();
        });

        try (NodeStoreFixture fixture = TestUtil.getReadOnlyFixture(repoDir, null)) {
            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture);
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));

                QueryManager qm = session.getWorkspace().getQueryManager();
                ValueFactory vf = session.getValueFactory();

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
            } finally {
                if (session != null) {
                    session.logout();
                }
                TestUtil.closeRepo(repo);
            }
        }
    }


}
