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

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.toEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MojoWithRepositoryParamsTest {
    private final File testOutBaseDir = new File("target/test-out/MojoWithRepositoryParamsTest");

    @Before
    public void setUp() throws Exception {
        testOutBaseDir.mkdirs();
    }

    @Test
    public void testIsTestScopeContainer() {
        MojoWithRepositoryParams mojo = mock(MojoWithRepositoryParams.class);
        doCallRealMethod().when(mojo).isTestScopeContainer();
        assertFalse("expect false", mojo.isTestScopeContainer());
    }

    @Test(expected = MojoFailureException.class)
    public void testResolveDependencies_unresolved() throws Exception {
        MojoWithRepositoryParams mojo = mock(MojoWithRepositoryParams.class);
        Artifact artifact = mock(Artifact.class);
        List<Artifact> unresolved = Collections.singletonList(artifact);
        when(mojo.resolveArtifacts(any(List.class), anyBoolean())).thenReturn(unresolved);
        doCallRealMethod().when(mojo).resolveDependencies(any(List.class), anyBoolean());
        mojo.resolveDependencies(Collections.emptyList(), true);
    }

    @Test
    public void testResolveDependencies() throws Exception {
        MojoWithRepositoryParams mojo = mock(MojoWithRepositoryParams.class);
        Artifact artifact = mock(Artifact.class);
        final File artifactFile = new File(testOutBaseDir, "artifactFile.txt");
        FileUtils.touch(artifactFile);
        when(artifact.getFile()).thenReturn(artifactFile);
        List<Artifact> unresolved = Collections.singletonList(artifact);
        when(mojo.resolveArtifacts(any(List.class), anyBoolean())).thenReturn(unresolved);
        doCallRealMethod().when(mojo).resolveDependencies(any(List.class), anyBoolean());
        assertEquals("same file list", Collections.singletonList(artifactFile),
                mojo.resolveDependencies(Collections.emptyList(), true));
    }

    @Test
    public void testResolveArtifacts() {
        final MavenSession session = mock(MavenSession.class);
        final MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(request);
        final MavenProject project = mock(MavenProject.class);
        MojoWithRepositoryParams mojo = mock(MojoWithRepositoryParams.class);
        when(mojo.getSession()).thenReturn(session);
        when(mojo.getProject()).thenReturn(Optional.of(project));
        Artifact artifact = mock(Artifact.class);
        final Set<Artifact> unresolved = Collections.singleton(artifact);

        final CompletableFuture<Map.Entry<Dependency, Boolean>> firstSlot = new CompletableFuture<>();
        doAnswer(call -> {
            firstSlot.complete(toEntry(call.getArgument(0, Dependency.class), call.getArgument(2, Boolean.class)));
            return unresolved;
        }).when(mojo).depToArtifact(any(Dependency.class), any(RepositoryRequest.class), anyBoolean());
        final Dependency firstDependency = mock(Dependency.class);
        doCallRealMethod().when(mojo).resolveArtifacts(any(List.class), anyBoolean());
        assertEquals("expect artifact", Collections.singletonList(artifact),
                mojo.resolveArtifacts(Collections.singletonList(firstDependency), false));
        assertSame("same dependency", firstDependency, firstSlot.getNow(toEntry(null, true)).getKey());
        assertFalse("same transitive", firstSlot.getNow(toEntry(null, true)).getValue());

        final CompletableFuture<Map.Entry<Dependency, Boolean>> secondSlot = new CompletableFuture<>();
        doAnswer(call -> {
            secondSlot.complete(toEntry(call.getArgument(0, Dependency.class), call.getArgument(2, Boolean.class)));
            return unresolved;
        }).when(mojo).depToArtifact(any(Dependency.class), any(RepositoryRequest.class), anyBoolean());
        final Dependency secondDependency = mock(Dependency.class);
        assertEquals("expect artifact", Collections.singletonList(artifact),
                mojo.resolveArtifacts(Collections.singletonList(secondDependency), true));
        assertSame("same dependency", secondDependency, secondSlot.getNow(toEntry(null, false)).getKey());
        assertTrue("same transitive", secondSlot.getNow(toEntry(null, false)).getValue());
    }

    @Test
    public void testDepToArtifact() {
        final MavenSession session = mock(MavenSession.class);
        final MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(request);
        final MavenProject project = mock(MavenProject.class);
        MojoWithRepositoryParams mojo = mock(MojoWithRepositoryParams.class);
        when(mojo.getSession()).thenReturn(session);
        when(mojo.getProject()).thenReturn(Optional.of(project));
        final RepositoryRequest baseRequest = mock(RepositoryRequest.class);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        when(mojo.getRepositorySystem()).thenReturn(repositorySystem);
        final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
        when(repositorySystem.resolve(any(ArtifactResolutionRequest.class))).thenReturn(result);
        final Artifact createdArtifact = mock(Artifact.class);
        when(repositorySystem.createDependencyArtifact(any(Dependency.class))).thenReturn(createdArtifact);

        doCallRealMethod().when(mojo).depToArtifact(any(Dependency.class), any(RepositoryRequest.class), anyBoolean());

        final Artifact dependencyArtifact = mock(Artifact.class);
        final Set<Artifact> allArtifacts = new HashSet<>(Arrays.asList(createdArtifact, dependencyArtifact));
        when(result.getArtifacts()).thenReturn(allArtifacts);
        final Dependency firstDependency = mock(Dependency.class);
        assertSame("expect all artifacts", allArtifacts, mojo.depToArtifact(firstDependency, baseRequest, true));
        assertEquals("expect created artifact", Collections.singleton(createdArtifact),
                mojo.depToArtifact(firstDependency, baseRequest, false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateContainerClassLoader() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testCreateContainerClassLoader").getAbsoluteFile();
        FileUtils.deleteDirectory(testOutDir);
        final File buildTestOutputDir = new File(testOutDir, "test-classes");
        final File buildOutputDir = new File(testOutDir, "classes");

        final Build build = mock(Build.class);
        when(build.getOutputDirectory()).thenReturn(buildOutputDir.getAbsolutePath());
        when(build.getTestOutputDirectory()).thenReturn(buildTestOutputDir.getAbsolutePath());

        final Dependency compileDependency = new Dependency();
        final Dependency testDependency = new Dependency();
        testDependency.setScope("test");
        final List<Dependency> projectDependencies = Arrays.asList(compileDependency, testDependency);

        final File jarsDir = new File(testOutDir, "jars");
        final File compileJar = new File(jarsDir, "for-compile.jar");
        FileUtils.touch(compileJar);
        final File testJar = new File(jarsDir, "for-test.jar");
        FileUtils.touch(testJar);

        final MavenProject project = mock(MavenProject.class);
        when(project.getBuild()).thenReturn(build);
        when(project.getDependencies()).thenReturn(projectDependencies);

        MojoWithRepositoryParams mojo = mock(MojoWithRepositoryParams.class);
        when(mojo.getProject()).thenReturn(Optional.of(project));

        doAnswer(call ->
                ((List<Dependency>) call.getArgument(0, List.class)).stream().map(dep -> {
                    if (dep == compileDependency) {
                        return compileJar;
                    } else if (dep == testDependency) {
                        return testJar;
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList())
        ).when(mojo).resolveDependencies(any(List.class), anyBoolean());

        doCallRealMethod().when(mojo).createContainerClassLoader();

        final URLClassLoader compileCl = (URLClassLoader) mojo.createContainerClassLoader();
        assertTrue("should have classes dir",
                Stream.of(compileCl.getURLs()).anyMatch(buildOutputDir.toURI().toURL()::equals));
        assertFalse("should not have test-classes dir",
                Stream.of(compileCl.getURLs()).anyMatch(buildTestOutputDir.toURI().toURL()::equals));
        assertTrue("should have compile jar",
                Stream.of(compileCl.getURLs()).anyMatch(compileJar.toURI().toURL()::equals));
        assertFalse("should not have test jar",
                Stream.of(compileCl.getURLs()).anyMatch(testJar.toURI().toURL()::equals));

        when(mojo.isTestScopeContainer()).thenReturn(true);
        final URLClassLoader testCl = (URLClassLoader) mojo.createContainerClassLoader();
        assertFalse("should not have classes dir",
                Stream.of(testCl.getURLs()).anyMatch(buildOutputDir.toURI().toURL()::equals));
        assertTrue("should have test-classes dir",
                Stream.of(testCl.getURLs()).anyMatch(buildTestOutputDir.toURI().toURL()::equals));
        assertFalse("should not have compile jar",
                Stream.of(testCl.getURLs()).anyMatch(compileJar.toURI().toURL()::equals));
        assertTrue("should have test jar",
                Stream.of(testCl.getURLs()).anyMatch(testJar.toURI().toURL()::equals));
    }
}