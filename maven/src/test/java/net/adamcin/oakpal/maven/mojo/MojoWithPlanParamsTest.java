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
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.SlingNodetypesScanner;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.vault.fs.spi.DefaultNodeTypeSet;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.entriesToMap;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.Fun.zipKeysWithValueFunc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MojoWithPlanParamsTest {
    private final File testOutBaseDir = new File("target/test-out/MojoWithPlanParamsTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    @Test
    public void testGetPlanBaseUrl() throws Exception {
        final File defaultFile = new File(".").getAbsoluteFile();
        final File realBaseDir = new File(testOutBaseDir, "testGetPlanBaseUrl").getAbsoluteFile();
        FileUtils.deleteDirectory(realBaseDir);
        realBaseDir.mkdirs();
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).getPlanBaseUrl();

        assertEquals("default url matches", defaultFile.toURI().toURL(), mojo.getPlanBaseUrl());

        MavenProject project = mock(MavenProject.class);
        when(mojo.getProject()).thenReturn(Optional.of(project));

        assertEquals("default url matches with project", defaultFile.toURI().toURL(),
                mojo.getPlanBaseUrl());

        when(project.getBasedir()).thenReturn(realBaseDir);

        assertEquals("basedir url matches", realBaseDir.toURI().toURL(), mojo.getPlanBaseUrl());
    }

    @Test
    public void testGetPlanName() {
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).getPlanName();
        assertNull("expect null planName by default", mojo.getPlanName());
    }

    @Test
    public void testBuildPlan() throws Exception {
        final File realBaseDir = new File(testOutBaseDir, "testBuildPlan").getAbsoluteFile();
        FileUtils.deleteDirectory(realBaseDir);
        realBaseDir.mkdirs();
        final MockMojoLog log = new MockMojoLog();
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).buildPlan();
        when(mojo.getLog()).thenReturn(log);

        final PlanBuilderParams params = mock(PlanBuilderParams.class);
        when(mojo.getPlanBuilderParams()).thenReturn(params);

        NodeTypeSet nodeTypes = new DefaultNodeTypeSet("<aggregate>");
        when(mojo.aggregateCnds(any(PlanBuilderParams.class), any(NamespaceMapping.class),
                any(Fun.ThrowingFunction.class), any(Fun.ThrowingSupplier.class)))
                .thenReturn(nodeTypes);
        final URL expectPlanBaseUrl = realBaseDir.toURI().toURL();
        when(mojo.getPlanBaseUrl()).thenReturn(expectPlanBaseUrl);
        final List<String> expectChecklists = Arrays.asList("one", "two");
        when(params.getChecklists()).thenReturn(expectChecklists);
        final CheckSpec check1 = new CheckSpec();
        check1.setName("check1");
        final CheckSpec check2 = new CheckSpec();
        check2.setName("check2");
        final List<CheckSpec> expectChecks = Arrays.asList(check1, check2);
        when(params.getChecks()).thenReturn(expectChecks);
        final List<ForcedRoot> expectForcedRoots = Arrays.asList(
                new ForcedRoot().withPath("/one"),
                new ForcedRoot().withPath("/two"));
        when(params.getForcedRoots()).thenReturn(expectForcedRoots);
        final InstallHookPolicy expectPolicy = InstallHookPolicy.PROHIBIT;
        when(params.getInstallHookPolicy()).thenReturn(expectPolicy);

        final OakpalPlan plan = mojo.buildPlan();
        assertNotNull("expect plan", plan);
        assertEquals("expect planBaseUrl", expectPlanBaseUrl, plan.getBase());
        assertEquals("expect default plan name", "plan.json", plan.getName());
        assertEquals("expect checklists", expectChecklists, plan.getChecklists());
        assertEquals("expect checks", expectChecks, plan.getChecks());
        assertEquals("expect forcedRoots", expectForcedRoots, plan.getForcedRoots());
        assertSame("expect installHookPolicy", expectPolicy, plan.getInstallHookPolicy());
    }

    @Test
    public void testAggregateCnds() throws Exception {
        final File srcDir = new File("src/test/resources/MojoWithPlanParamsTest");
        final File testOutDir = new File(testOutBaseDir, "testAggregateCnds").getAbsoluteFile();
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).aggregateCnds(
                any(PlanBuilderParams.class), any(NamespaceMapping.class),
                any(Fun.ThrowingFunction.class), any(Fun.ThrowingSupplier.class));

        final MockMojoLog log = new MockMojoLog();
        when(mojo.getLog()).thenReturn(log);

        final PlanBuilderParams params = mock(PlanBuilderParams.class);
        final NamespaceMapping mapping = JsonCnd.BUILTIN_MAPPINGS;
        final Map<String, URL> allCnds = Stream.of("a", "b", "c", "d", "e", "f", "y", "z")
                .map(letter -> letter + ".cnd")
                .map(zipKeysWithValueFunc(uncheck1(name -> new File(srcDir, name).toURI().toURL())))
                .collect(entriesToMap());
        final Fun.ThrowingFunction<List<String>, Map<String, URL>> cndResolver =
                names -> names.stream().map(zipKeysWithValueFunc(allCnds::get)).collect(entriesToMap());

        final Fun.ThrowingSupplier<List<URL>> slingCndFinder = () -> Stream.of("b", "c", "d", "e", "f")
                .map(letter -> new File(srcDir, letter + ".cnd").toURI())
                .map(uncheck1(URI::toURL))
                .collect(Collectors.toList());

        assertTrue("empty to start", mojo.aggregateCnds(params, mapping, cndResolver, slingCndFinder)
                .getNodeTypes().isEmpty());
        log.printAll();
        assertTrue("log is empty", log.entries.isEmpty());

        when(params.isSlingNodeTypes()).thenReturn(true);


        final NodeTypeSet justSlingNts = mojo.aggregateCnds(params, mapping, cndResolver, slingCndFinder);
        final NamePathResolver justSlingResolver = new DefaultNamePathResolver(justSlingNts.getNamespaceMapping());
        assertFalse("not empty with slingNodeTypes", justSlingNts.getNodeTypes().isEmpty());
        assertTrue("b defined",
                justSlingNts.getNodeTypes().containsKey(justSlingResolver.getQName("b:primaryType")));
        assertFalse("a not defined",
                justSlingNts.getNodeTypes().containsKey(justSlingResolver.getQName("a:primaryType")));
        log.printAll();
        assertFalse("log is not empty", log.entries.isEmpty());
        assertTrue("log is not empty", log.entries.stream()
                .allMatch(entry -> entry.message.startsWith(SlingNodetypesScanner.SLING_NODETYPES)));

        when(params.getCndNames()).thenReturn(Collections.singletonList("a.cnd"));
        final NodeTypeSet withAlphaCnd = mojo.aggregateCnds(params, mapping, cndResolver, slingCndFinder);
        final NamePathResolver withAlphaCndResolver = new DefaultNamePathResolver(withAlphaCnd.getNamespaceMapping());
        assertFalse("not empty with slingNodeTypes", withAlphaCnd.getNodeTypes().isEmpty());
        assertTrue("b defined",
                withAlphaCnd.getNodeTypes().containsKey(withAlphaCndResolver.getQName("b:primaryType")));
        assertTrue("a defined",
                withAlphaCnd.getNodeTypes().containsKey(withAlphaCndResolver.getQName("a:primaryType")));
    }

    @Test(expected = MojoFailureException.class)
    public void testAggregateCnds_cndResolver_throws() throws Exception {
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).aggregateCnds(
                any(PlanBuilderParams.class), any(NamespaceMapping.class),
                any(Fun.ThrowingFunction.class), any(Fun.ThrowingSupplier.class));

        final PlanBuilderParams params = mock(PlanBuilderParams.class);
        final NamespaceMapping mapping = JsonCnd.BUILTIN_MAPPINGS;
        final Fun.ThrowingFunction<List<String>, Map<String, URL>> cndResolver = names -> {
            throw new Exception("Phbbt.");
        };

        final Fun.ThrowingSupplier<List<URL>> slingCndFinder = Collections::emptyList;

        when(params.getCndNames()).thenReturn(Arrays.asList("foo", "bar"));
        mojo.aggregateCnds(params, mapping, cndResolver, slingCndFinder);
    }

    @Test(expected = MojoFailureException.class)
    public void testAggregateCnds_wrongResolver_throws() throws Exception {
        final File realBaseDir = new File(testOutBaseDir, "testAggregateCnds_wrongResolver_throws").getAbsoluteFile();
        FileUtils.deleteDirectory(realBaseDir);
        realBaseDir.mkdirs();

        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).aggregateCnds(
                any(PlanBuilderParams.class), any(NamespaceMapping.class),
                any(Fun.ThrowingFunction.class), any(Fun.ThrowingSupplier.class));

        final PlanBuilderParams params = mock(PlanBuilderParams.class);
        final NamespaceMapping mapping = JsonCnd.BUILTIN_MAPPINGS;
        final URL fooUrl = new File(realBaseDir, "foo.zip").toURI().toURL();
        final Fun.ThrowingFunction<List<String>, Map<String, URL>> cndResolver =
                names -> Collections.singletonMap("foo", fooUrl);

        final Fun.ThrowingSupplier<List<URL>> slingCndFinder = Collections::emptyList;

        when(params.getCndNames()).thenReturn(Arrays.asList("foo", "bar"));
        mojo.aggregateCnds(params, mapping, cndResolver, slingCndFinder);
    }

    @Test(expected = MojoFailureException.class)
    public void testAggregateCnds_slingNodeTypes_throws() throws Exception {
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).aggregateCnds(
                any(PlanBuilderParams.class), any(NamespaceMapping.class),
                any(Fun.ThrowingFunction.class), any(Fun.ThrowingSupplier.class));

        final PlanBuilderParams params = mock(PlanBuilderParams.class);
        final NamespaceMapping mapping = JsonCnd.BUILTIN_MAPPINGS;
        final Fun.ThrowingFunction<List<String>, Map<String, URL>> cndResolver = names -> Collections.emptyMap();

        final Fun.ThrowingSupplier<List<URL>> slingCndFinder = () -> {
            throw new Exception("Dang.");
        };

        when(params.isSlingNodeTypes()).thenReturn(true);
        mojo.aggregateCnds(params, mapping, cndResolver, slingCndFinder);
    }

    @Test
    public void testGetPreInstallFiles() throws Exception {
        final File realBaseDir = new File(testOutBaseDir, "testGetPreInstallFiles").getAbsoluteFile();
        FileUtils.deleteDirectory(realBaseDir);
        realBaseDir.mkdirs();
        MojoWithPlanParams mojo = mock(MojoWithPlanParams.class);
        doCallRealMethod().when(mojo).getPreInstallFiles(any(PlanBuilderParams.class));
        final PlanBuilderParams params = mock(PlanBuilderParams.class);
        final File preInstall1 = new File(realBaseDir, "pre-install-1.zip");
        final File preInstall2 = new File(realBaseDir, "pre-install-2.zip");


        final List<File> preInstallFiles = Arrays.asList(preInstall1, preInstall2);
        when(params.getPreInstallFiles()).thenReturn(preInstallFiles);
        assertEquals("expect preinstall files", preInstallFiles, mojo.getPreInstallFiles(params));

        final File depFilter1File = new File(realBaseDir, "dep-filter-1.zip");
        final DependencyFilter depFilter1 = new DependencyFilter().withArtifactId("dep-filter-1").withType("zip");
        final List<DependencyFilter> preInstallArtifacts = Arrays.asList(depFilter1);
        when(params.getPreInstallArtifacts()).thenReturn(preInstallArtifacts);
        assertEquals("expect preinstall files", preInstallFiles, mojo.getPreInstallFiles(params));

        final MavenProject project = mock(MavenProject.class);
        when(mojo.getProject()).thenReturn(Optional.of(project));

        final List<Dependency> projectDependencies = Arrays.asList(depFilter1.toDependency());
        when(project.getDependencies()).thenReturn(projectDependencies);

        doAnswer(call ->
                ((List<Dependency>) call.getArgument(0, List.class)).stream().map(dep -> {
                    if ("dep-filter-1".equals(dep.getArtifactId())) {
                        return depFilter1File;
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList())
        ).when(mojo).resolveDependencies(any(List.class), anyBoolean());

        final List<File> allFiles = Arrays.asList(depFilter1File, preInstall1, preInstall2);
        assertEquals("expect preinstall files", allFiles, mojo.getPreInstallFiles(params));
    }
}