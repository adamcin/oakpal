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

package net.adamcin.oakpal.testing.oakpaltest;

import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class HandlerTest {

    @Before
    public void setUp() throws Exception {
        Handler.register();
    }

    @Test
    public void testRegister() {
        final String orig = System.getProperty(Handler.PROP, "");
        final String withoutPackage = orig.replaceFirst(Handler.HANDLER_PKG + "\\|?", "");
        System.setProperty(Handler.PROP, withoutPackage);
        assertEquals("expect without", withoutPackage, System.getProperty(Handler.PROP, ""));
        Handler.register();
        assertTrue("expect with",
                System.getProperty(Handler.PROP, "").matches(Pattern.quote(Handler.HANDLER_PKG) + "($|\\|.+)"));
    }

    @Test
    public void testOpenConnection() throws Exception {
        final File testFile = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final URL expectUrl = testFile.toURI().toURL();
        final URL schemeUrl = new URL(Handler.PROTO + ":/target/test-packages/tmp_foo_bar.zip");
        final URLConnection schemeConn = schemeUrl.openConnection();

        assertNotNull("has connect", schemeConn);
        assertEquals("same url", expectUrl, schemeConn.getURL());

        Handler handler = new Handler();
        assertNull("return null for other schemes", handler.openConnection(expectUrl));
    }


}