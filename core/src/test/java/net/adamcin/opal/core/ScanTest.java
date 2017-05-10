/*
 * Copyright 2017 Mark Adamcin
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

package net.adamcin.opal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.script.SimpleBindings;

import net.adamcin.commons.testing.junit.TestBody;
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
    public void testSimplePackage() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                File package10 = TestPackageUtil.prepareTestPackage("package_1.0.zip");

                OpalHandler listener = new DefaultOpalHandler() {

                    @Override
                    public void onOpen(PackageId packageId, PackageProperties packageProperties, MetaInf metaInf, List<PackageId> subpackages) {
                    }

                    @Override
                    public void onClose(PackageId packageId, Session inspectSession) {
                        LOGGER.info("My userId is {}", inspectSession.getUserID());
                    }
                };

                new OpalContext.Builder().withHandlers(listener).build().scanPackages(package10);
            }
        });
    }

    @Test
    public void testJCRListener() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                File fullcoverage = TestPackageUtil.prepareTestPackage("fullcoverage.zip");

                final List<String> importedPaths = new ArrayList<>();
                final List<String> queriedPaths = new ArrayList<>();

                OpalHandler handler = new DefaultOpalHandler() {

                    @Override
                    public void onImportPath(PackageId packageId, String path, Node node) throws RepositoryException {
                        importedPaths.add(node.getPath());
                        LOGGER.info("imported path: {}", node.getPath());
                    }

                    @Override
                    public void onClose(PackageId packageId, Session inspectSession) throws RepositoryException {
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

                new OpalContext.Builder().withHandlers(handler).build().scanPackages(fullcoverage);
            }
        });
    }

    @Test
    public void testScriptHandler() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                File fullcoverage = TestPackageUtil.prepareTestPackage("fullcoverage.zip");

                OpalHandler handler = OpalScriptHandler.createScriptHandler("nashorn",
                        getClass().getResource("/simpleHandler.js"));

                new OpalContext.Builder().withHandlers(handler)
                        .build().scanPackages(fullcoverage).stream()
                .forEach(violation -> LOGGER.info("[{} violation] {}", violation.getSeverity(), violation.getDescription()));
            }
        });
    }
}
