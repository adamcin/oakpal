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

package net.adamcin.oakpal.api;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class OsgiConfigInstallableTest {

    @Test
    public void testConstructorAndGetters() {
        final PackageId expectParentId = PackageId.fromString("test:test:1");
        final String expectJcrPath = "/some/path";
        final Map<String, Object> expectProps = Collections.singletonMap("key", "value");
        final String expectServicePid = "some.service.Pid";
        final String expectFactoryPid = "some.factory.Pid";
        final OsgiConfigInstallable installable = new OsgiConfigInstallable(expectParentId,
                expectJcrPath, expectProps, expectServicePid, expectFactoryPid);

        assertSame("expect parentId", expectParentId, installable.getParentId());
        assertSame("expect jcrPath", expectJcrPath, installable.getJcrPath());
        assertEquals("expect equal props", expectProps, installable.getProperties());
        assertEquals("expect equal servicePid", expectServicePid, installable.getServicePid());
        assertEquals("expect equal factoryPid", expectFactoryPid, installable.getFactoryPid());
    }
}