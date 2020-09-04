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

package net.adamcin.oakpal.maven.mojo;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.JcrNs;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PlanParamsTest {

    @Test
    public void testDefaults() {
        PlanParams params = new PlanParams();
        assertEquals("expect preInstallArtifacts",
                Collections.emptyList(), params.getPreInstallArtifacts());
        assertEquals("expect preInstallFiles",
                Collections.emptyList(), params.getPreInstallFiles());
        assertEquals("expect cndNames",
                Collections.emptyList(), params.getCndNames());
        assertEquals("expect jcrNamespaces",
                Collections.emptyList(), params.getJcrNamespaces());
        assertEquals("expect jcrPrivileges",
                Collections.emptyList(), params.getJcrPrivileges());
        assertEquals("expect forcedRoots",
                Collections.emptyList(), params.getForcedRoots());
        assertEquals("expect checks",
                Collections.emptyList(), params.getChecks());
        assertEquals("expect checklists",
                Collections.emptyList(), params.getChecklists());
        assertEquals("expect repoInits",
                Collections.emptyList(), params.getRepoInits());
        assertEquals("expect runModes",
                Collections.emptyList(), params.getRunModes());

        assertFalse("expect false slingNodeTypes", params.isSlingNodeTypes());
        assertFalse("expect false enablePreInstallHooks", params.isEnablePreInstallHooks());
        assertNull("expect null installHookPolicy", params.getInstallHookPolicy());
    }

    @Test
    public void testSetters() {
        final PlanParams params = new PlanParams();
        params.setSlingNodeTypes(true);
        assertTrue("expect slingNodeTypes", params.isSlingNodeTypes());

        params.setEnablePreInstallHooks(true);
        assertTrue("expect enablePreInstallHooks", params.isEnablePreInstallHooks());

        params.setInstallHookPolicy(InstallHookPolicy.PROHIBIT);
        assertSame("expect installHookPolicy", InstallHookPolicy.PROHIBIT, params.getInstallHookPolicy());

        final List<DependencyFilter> expectPreInstallArtifacts = Collections.singletonList(new DependencyFilter());
        params.setPreInstallArtifacts(expectPreInstallArtifacts);
        assertEquals("expect preInstallArtifacts",
                expectPreInstallArtifacts, params.getPreInstallArtifacts());

        final List<File> expectPreInstallFiles = Collections.singletonList(new File("."));
        params.setPreInstallFiles(expectPreInstallFiles);
        assertEquals("expect preInstallFiles",
                expectPreInstallFiles, params.getPreInstallFiles());

        final List<String> expectCndNames = Collections.singletonList("a.cnd");
        params.setCndNames(expectCndNames);
        assertEquals("expect cndNames", expectCndNames, params.getCndNames());

        final List<JcrNs> expectJcrNamespaces = Collections.singletonList(JcrNs.create("foo", "http://foo.com"));
        params.setJcrNamespaces(expectJcrNamespaces);
        assertEquals("expect jcrNamespaces", expectJcrNamespaces, params.getJcrNamespaces());

        final List<String> expectJcrPrivileges = Collections.singletonList("foo:canDo");
        params.setJcrPrivileges(expectJcrPrivileges);
        assertEquals("expect jcrPrivileges", expectJcrPrivileges, params.getJcrPrivileges());

        final List<ForcedRoot> expectForcedRoots = Collections.singletonList(new ForcedRoot().withPath("/foo"));
        params.setForcedRoots(expectForcedRoots);
        assertEquals("expect forcedRoots", expectForcedRoots, params.getForcedRoots());

        final List<CheckSpec> expectChecks = Collections.singletonList(new CheckSpec());
        params.setChecks(expectChecks);
        assertEquals("expect checks", expectChecks, params.getChecks());

        final List<String> expectChecklists = Collections.singletonList("checklist");
        params.setChecklists(expectChecklists);
        assertEquals("expect checklists", expectChecklists, params.getChecklists());

        final List<String> expectRepoInits = Collections.singletonList("create user foo");
        params.setRepoInits(expectRepoInits);
        assertEquals("expect repoInits", expectRepoInits, params.getRepoInits());

        final List<File> expectRepoInitFiles = Collections.singletonList(new File("."));
        params.setRepoInitFiles(expectRepoInitFiles);
        assertEquals("expect repoInitFiles",
                expectRepoInitFiles, params.getRepoInitFiles());

        final List<String> expectRunModes = Arrays.asList("author", "publish");
        params.setRepoInits(expectRunModes);
        assertEquals("expect runModes", expectRunModes, params.getRepoInits());

    }

    @Test
    public void testToString() {
        assertFalse("non-empty string", new PlanParams().toString().isEmpty());
    }


}