/*
 * Copyright 2018 Mark Adamcin
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.json.JsonValue;

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.SimpleProgressCheck;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by madamcin on 5/8/17.
 */
public class ScanTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanTest.class);

    @Test
    public void testSimplePackage() throws Exception {
        File package10 = TestPackageUtil.prepareTestPackage("package_1.0.zip");

        ProgressCheck listener = new SimpleProgressCheck() {

            @Override
            public void beforeExtract(PackageId packageId, Session inspectSession,
                                      PackageProperties packageProperties, MetaInf metaInf,
                                      List<PackageId> subpackages) {
            }

            @Override
            public void afterExtract(PackageId packageId, Session inspectSession) {
                LOGGER.info("My userId is {}", inspectSession.getUserID());
            }
        };

        new OakMachine.Builder().withProgressChecks(listener).build().scanPackage(package10);
    }

    @Test
    public void testJCRListener() throws Exception {
        File fullcoverage = TestPackageUtil.prepareTestPackage("fullcoverage.zip");

        final List<String> importedPaths = new ArrayList<>();
        final List<String> queriedPaths = new ArrayList<>();

        ProgressCheck handler = new SimpleProgressCheck() {

            @Override
            public void importedPath(PackageId packageId, String path, Node node) throws RepositoryException {
                importedPaths.add(node.getPath());
                LOGGER.info("imported path: {}", node.getPath());
            }

            @Override
            public void afterExtract(PackageId packageId, Session inspectSession) throws RepositoryException {
                QueryManager qm = inspectSession.getWorkspace().getQueryManager();
                Query q = qm.createQuery("//element(*, vlt:FullCoverage)", "xpath");
                QueryResult r = q.execute();
                NodeIterator nodes = r.getNodes();
                while (nodes.hasNext()) {
                    Node nextNode = nodes.nextNode();
                    queriedPaths.add(nextNode.getPath());
                    LOGGER.info("queried path: {}", nextNode.getPath());
                }

            }
        };

        new OakMachine.Builder().withProgressChecks(handler).build().scanPackage(fullcoverage);
    }

    @Test
    public void testScriptHandler() throws Exception {
        File fullcoverage = TestPackageUtil.prepareTestPackage("fullcoverage.zip");

        ProgressCheck handler = ScriptProgressCheck.createScriptCheckFactory(
                getClass().getResource("/simpleHandler.js")).newInstance(JsonValue.EMPTY_JSON_OBJECT);

        new OakMachine.Builder().withProgressChecks(handler)
                .build().scanPackage(fullcoverage).stream()
                .flatMap(r -> r.getViolations().stream())
                .forEach(violation -> LOGGER.info("[{} violation] {}", violation.getSeverity(),
                        violation.getDescription()));
    }

    @Test
    public void testConstraintViolation() throws Exception {
        File cvp = TestPackageUtil.prepareTestPackageFromFolder("cvp.zip",
                new File("src/test/resources/constraint_violator_package"));

        ProgressCheck check = new SimpleProgressCheck() {
            @Override
            public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
                assertEquals("title should be persisted.", "Doc",
                        inspectSession.getNode("/apps/acme/docs").getProperty("jcr:title").getString());
            }
        };

        new OakMachine.Builder().withInitStage(
                new InitStage.Builder().withForcedRoot("/apps/acme/docs").build())
                .withProgressChecks(check).build()
                .scanPackage(cvp).stream()
                .flatMap(r -> r.getViolations().stream())
                .forEach(violation -> LOGGER.info("[{} violation] {}", violation.getSeverity(),
                        violation.getDescription()));
    }

    @Test
    public void testNewUserPackage() throws Exception {
        File nup = TestPackageUtil.prepareTestPackageFromFolder("nup.zip",
                new File("src/test/resources/new_user_package"));

        ProgressCheck check = new SimpleProgressCheck() {
            @Override
            public void importedPath(final PackageId packageId, final String path, final Node node)
                    throws RepositoryException {
                if (path.equals("/home/users/acme")) {
                    UserManager manager = ((JackrabbitSession) node.getSession()).getUserManager();
                    User user = (User) manager.getAuthorizableByPath(path);
                    assertEquals("acme is acme", "acme", user.getID());
                }
            }
        };

        new OakMachine.Builder()
                .withProgressChecks(check).build()
                .scanPackage(nup).stream()
                .flatMap(r -> r.getViolations().stream())
                .forEach(violation -> LOGGER.info("[{} violation] {}", violation.getSeverity(),
                        violation.getDescription()));
    }
}
