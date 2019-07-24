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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import javax.jcr.nodetype.NodeTypeManager;

import org.junit.Before;
import org.junit.Test;

public class CNDURLInstallerTest {

    private URL cndAUrl;
    private URL cndBUrl;
    private URL cndCUrl;
    private URL cndDUrl;
    private URL cndEUrl;
    private URL cndFUrl;
    private URL cndYUrl;
    private URL cndZUrl;

    @Before
    public void setUp() throws Exception {
        File cndAFile = new File("src/test/resources/CNDURLInstallerTest/a.cnd");
        cndAUrl = cndAFile.toURI().toURL();
        File cndBFile = new File("src/test/resources/CNDURLInstallerTest/b.cnd");
        cndBUrl = cndBFile.toURI().toURL();
        File cndCFile = new File("src/test/resources/CNDURLInstallerTest/c.cnd");
        cndCUrl = cndCFile.toURI().toURL();
        File cndDFile = new File("src/test/resources/CNDURLInstallerTest/d.cnd");
        cndDUrl = cndDFile.toURI().toURL();
        File cndEFile = new File("src/test/resources/CNDURLInstallerTest/e.cnd");
        cndEUrl = cndEFile.toURI().toURL();
        File cndFFile = new File("src/test/resources/CNDURLInstallerTest/f.cnd");
        cndFUrl = cndFFile.toURI().toURL();
        File cndYFile = new File("src/test/resources/CNDURLInstallerTest/y.cnd");
        cndYUrl = cndYFile.toURI().toURL();
        File cndZFile = new File("src/test/resources/CNDURLInstallerTest/z.cnd");
        cndZUrl = cndZFile.toURI().toURL();
    }

    @Test
    public void testConstructor() {
        final CNDURLInstaller installerNulls = new CNDURLInstaller(null,
                Collections.emptyList(), null);
    }

    @Test
    public void testRegister() throws Exception {
        final DefaultErrorListener errorListener = new DefaultErrorListener();
        final CNDURLInstaller installer = new CNDURLInstaller(errorListener,
                Arrays.asList(cndFUrl, cndEUrl, cndDUrl, cndCUrl, cndBUrl, cndAUrl),
                Arrays.asList(cndYUrl, cndZUrl));

        new OakMachine.Builder().build().adminInitAndInspect(session -> {
            installer.register(session);
            NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();
            assertTrue("has a:primaryType", ntManager.hasNodeType("a:primaryType"));
            assertTrue("has b:primaryType", ntManager.hasNodeType("b:primaryType"));
            assertTrue("has c:primaryType", ntManager.hasNodeType("c:primaryType"));
            assertTrue("has d:primaryType", ntManager.hasNodeType("d:primaryType"));
            assertTrue("has e:primaryType", ntManager.hasNodeType("e:primaryType"));
            assertFalse("does not have f:primaryType", ntManager.hasNodeType("f:primaryType"));
            assertFalse("does not have y:primaryType", ntManager.hasNodeType("y:primaryType"));
            assertTrue("has z:primaryType", ntManager.hasNodeType("z:primaryType"));
        });
        assertEquals("f and y should report errors: " + errorListener.getReportedViolations(), 2,
                errorListener.getReportedViolations().size());
    }
}