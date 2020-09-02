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

import net.adamcin.oakpal.api.OsgiConfigInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class OsgiConfigInstallableParamsTest {

    @Test
    public void testConstructor() {
        final Map<String, Object> expectProperties = new LinkedHashMap<>();
        expectProperties.put("foo", "bar");
        expectProperties.put("bar", "foo");
        final String expectServicePid = "myServicePid";
        final String expectFactoryPid = "myFactoryPid";
        final Exception expectParseError = new IOException("my parse error");
        OsgiConfigInstallableParams params = new OsgiConfigInstallableParams(expectProperties, expectServicePid,
                expectFactoryPid, expectParseError);

        assertEquals("expect same properties", expectProperties, params.getProperties());
        assertEquals("expect same servicePid", expectServicePid, params.getServicePid());
        assertEquals("expect same factoryPid", expectFactoryPid, params.getFactoryPid());
        assertSame("expect same parseError", expectParseError, params.getParseError());
        assertSame("expect correct installableType", OsgiConfigInstallable.class, params.getInstallableType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstallable_throws() throws Exception {
        final Map<String, Object> expectProperties = new LinkedHashMap<>();
        expectProperties.put("foo", "bar");
        expectProperties.put("bar", "foo");
        final String expectServicePid = "myServicePid";
        final String expectFactoryPid = "myFactoryPid";
        final Exception expectParseError = new IllegalArgumentException("my parse error");
        OsgiConfigInstallableParams params = new OsgiConfigInstallableParams(expectProperties, expectServicePid,
                expectFactoryPid, expectParseError);
        params.createInstallable(PackageId.fromString("test"), "/some/path");
    }

    @Test
    public void testCreateInstallable() throws Exception {
        final Map<String, Object> expectProperties = new LinkedHashMap<>();
        expectProperties.put("foo", "bar");
        expectProperties.put("bar", "foo");
        final String expectServicePid = "myServicePid";
        final String expectFactoryPid = "myFactoryPid";
        final PackageId expectParentId = PackageId.fromString("test");
        final String expectJcrPath = "/some/path";
        OsgiConfigInstallableParams params = new OsgiConfigInstallableParams(expectProperties, expectServicePid,
                expectFactoryPid, null);
        OsgiConfigInstallable installable = params.createInstallable(expectParentId, expectJcrPath);
        assertSame("expect same parentId", expectParentId, installable.getParentId());
        assertSame("expect same jcrPath", expectJcrPath, installable.getJcrPath());
        assertEquals("expect same properties", expectProperties, params.getProperties());
        assertEquals("expect same servicePid", expectServicePid, params.getServicePid());
        assertEquals("expect same factoryPid", expectFactoryPid, params.getFactoryPid());
        assertNull("expect null parseError", params.getParseError());
    }
}