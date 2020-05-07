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

import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.core.Util;
import net.adamcin.oakpal.testing.TestPackageUtil;
import net.adamcin.oakpal.testing.oakpaltest.Handler;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.maven.mojo.OpearPackageMojo.OAKPAL_API_ARTIFACT_ID;
import static net.adamcin.oakpal.maven.mojo.OpearPackageMojo.OAKPAL_CORE_ARTIFACT_ID;
import static net.adamcin.oakpal.maven.mojo.OpearPackageMojo.OAKPAL_GROUP_ID;
import static net.adamcin.oakpal.maven.mojo.OpearPackageMojo.OPEAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpearPackageMojoTest {
    private final File testOutBaseDir = new File("target/test-out/OpearPackageMojoTest");
    private final File srcDir = new File("src/test/resources/OpearPackageMojoTest");

    @Before
    public void setUp() throws Exception {
        Handler.register();
        testOutBaseDir.mkdirs();
    }

    private static OpearPackageMojo newMojo() {
        final OpearPackageMojo mojo = new OpearPackageMojo();
        final MockMojoLog log = new MockMojoLog();
        mojo.setLog(log);
        return mojo;
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecute_fails() throws Exception {
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        when(project.clone()).thenReturn(project);
        mojo.project = project;
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        mojo.execute();
    }

    @Test
    public void testExecute_asJar() throws Exception {
        final File preInstallPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final File testOutDir = new File(testOutBaseDir, "testExecute_asJar");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        when(project.getPackaging()).thenReturn("jar");
        final String expectGroupId = "com.example.testAssembleOpear";
        final String expectArtifactId = "assembly1";
        final String expectVersion = "1.0-SNAPSHOT";
        when(project.getGroupId()).thenReturn(expectGroupId);
        when(project.getArtifactId()).thenReturn(expectArtifactId);
        when(project.getVersion()).thenReturn(expectVersion);
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        final ArtifactHandlerManager artifactHandlerManager = mock(ArtifactHandlerManager.class);
        mojo.artifactHandlerManager = artifactHandlerManager;
        final ArtifactHandler opearHandler = mock(ArtifactHandler.class);
        when(artifactHandlerManager.getArtifactHandler(OPEAR)).thenReturn(opearHandler);

        final File basedir = new File(testOutDir, "basedir");
        final File projectFile = new File(basedir, "pom.xml");
        FileUtils.touch(projectFile);
        when(project.getFile()).thenReturn(projectFile);

        final File target = new File(basedir, "target");
        target.mkdirs();
        mojo.outputDirectory = target;
        final File targetClasses = new File(target, "classes");
        targetClasses.mkdirs();
        final Build build = mock(Build.class);
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn(target.getPath());
        when(build.getOutputDirectory()).thenReturn(targetClasses.getPath());
        FileUtils.copyFile(new File(srcDir, "echo.js"), new File(targetClasses, "echo.js"));

        final String expectFinalName = expectArtifactId + "-" + expectVersion;
        mojo.finalName = expectFinalName;
        final File fakeJar = new File(target, expectFinalName + ".jar");
        FileUtils.touch(fakeJar);

        final Artifact projectArtifact = mock(Artifact.class);
        when(projectArtifact.getType()).thenReturn("jar");
        when(project.getArtifact()).thenReturn(projectArtifact);
        when(projectArtifact.getFile()).thenReturn(fakeJar);

        final String expectOakpalVersion = "0.31415926535898";
        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectOakpalVersion)
                .toDependency();
        final File oakpalDepFile = new File(testOutDir, "oakpal-core.jar");
        FileUtils.touch(oakpalDepFile);
        final Artifact oakpalArt = mock(Artifact.class);
        when(oakpalArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalArt.getVersion()).thenReturn(expectOakpalVersion);
        when(oakpalArt.getScope()).thenReturn("compile");
        when(oakpalArt.getFile()).thenReturn(oakpalDepFile);

        final Dependency compileDep = new DependencyFilter().toDependency("compile");
        final File compileDepFile = new File(testOutDir, "compile-scope.jar");
        FileUtils.touch(compileDepFile);
        final Artifact compileArt = mock(Artifact.class);
        when(compileArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(compileArt.getArtifactId()).thenReturn("compile-scope");
        when(compileArt.getScope()).thenReturn("compile");
        when(compileArt.getFile()).thenReturn(compileDepFile);
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalCoreDep, compileDep));
        doAnswer(call -> {
            final Dependency input = call.getArgument(0, Dependency.class);
            if (input == oakpalCoreDep) {
                return oakpalArt;
            } else if (input == compileDep) {
                return compileArt;
            } else {
                return null;
            }
        }).when(repositorySystem).createDependencyArtifact(any(Dependency.class));

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
            when(result.getArtifacts()).thenReturn(Collections.singleton(request.getArtifact()));
            return result;
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        final String expectFilename = "sample-plan.json";
        final File samplePlanFile = new File(srcDir, expectFilename);
        mojo.planFile = samplePlanFile;

        when(project.clone()).thenReturn(project);
        final File expectFinalFile = new File(target, expectFinalName + "." + OPEAR);

        final CompletableFuture<Artifact> attachedSlot = new CompletableFuture<>();
        doAnswer(call -> attachedSlot.complete(call.getArgument(0, Artifact.class)))
                .when(project).addAttachedArtifact(any(Artifact.class));
        mojo.execute();
        final Artifact attached = attachedSlot.getNow(null);
        assertNotNull("attached is present", attached);
        assertEquals("expect opear file", expectFinalFile, attached.getFile());
        try (JarFile jarFile = new JarFile(expectFinalFile)) {
            final Set<String> allEntries = jarFile.stream().map(JarEntry::getName).collect(Collectors.toSet());
            final List<String> expectEntries = Arrays.asList(
                    "lib/" + fakeJar.getName(),
                    "lib/" + compileDepFile.getName(),
                    "tmp_foo_bar.zip",
                    expectFilename
            );
            for (String expectEntry : expectEntries) {
                assertTrue(String.format("should have entry %s in %s", expectEntry, allEntries),
                        jarFile.stream().anyMatch(entry -> expectEntry.equals(entry.getName())));
            }
            final List<String> expectNoEntries = Arrays.asList(
                    "lib/classes/",
                    "lib/classes/echo.js",
                    "lib/" + oakpalDepFile.getName()
            );
            for (String noExpectEntry : expectNoEntries) {
                assertTrue(String.format("should not have entry %s in %s", noExpectEntry, allEntries),
                        jarFile.stream().noneMatch(entry -> noExpectEntry.equals(entry.getName())));
            }
            final Manifest manifest = jarFile.getManifest();
            assertEquals("expect bsn", Collections.singletonList(expectArtifactId + "-" + OPEAR),
                    Util.getManifestHeaderValues(manifest, "Bundle-SymbolicName"));
            assertEquals("expect bcp", Arrays.asList("lib/" + fakeJar.getName(), "lib/" + compileDepFile.getName()),
                    Util.getManifestHeaderValues(manifest, "Bundle-ClassPath"));
            assertEquals("expect oakpal version", Collections.singletonList(expectOakpalVersion),
                    Util.getManifestHeaderValues(manifest, "Oakpal-Version"));
            assertEquals("expect oakpal plan", Collections.singletonList(expectFilename),
                    Util.getManifestHeaderValues(manifest, "Oakpal-Plan"));
        }
    }


    @Test(expected = MojoExecutionException.class)
    public void testExecute_failToAssemble() throws Exception {
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        when(project.clone()).thenReturn(project);
        mojo.project = project;
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        mojo.execute();
    }

    @Test
    public void testGetOwnVersion() {
        assertFalse("getOwnVersion", newMojo().getOwnVersion().isEmpty());
    }

    @Test
    public void testGetOakpalCoreVersion() {
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final String expectVersion = "0.31415926535898";
        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectVersion)
                .toDependency();
        when(project.getDependencies()).thenReturn(Collections.singletonList(oakpalCoreDep));
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;
        final Artifact oakpalCoreArt = mock(Artifact.class);
        final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
        when(result.getArtifacts()).thenReturn(Collections.singleton(oakpalCoreArt));

        when(oakpalCoreArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalCoreArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalCoreArt.getVersion()).thenReturn(expectVersion);
        when(repositorySystem.createDependencyArtifact(oakpalCoreDep)).thenReturn(oakpalCoreArt);

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            if (request.getArtifact() == oakpalCoreArt) {
                return result;
            } else {
                return null;
            }
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        assertEquals("expect version", expectVersion, mojo.getOakpalCoreVersion());
    }

    @Test
    public void testGetOakpalCoreVersionWithApi() {
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final String expectVersion = "0.31415926535898";
        final Dependency oakpalApiDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_API_ARTIFACT_ID)
                .withVersion(expectVersion)
                .toDependency();
        when(project.getDependencies()).thenReturn(Collections.singletonList(oakpalApiDep));
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;
        final Artifact oakpalApiArt = mock(Artifact.class);
        final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
        when(result.getArtifacts()).thenReturn(Collections.singleton(oakpalApiArt));

        when(oakpalApiArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalApiArt.getArtifactId()).thenReturn(OAKPAL_API_ARTIFACT_ID);
        when(oakpalApiArt.getVersion()).thenReturn(expectVersion);
        when(repositorySystem.createDependencyArtifact(oakpalApiDep)).thenReturn(oakpalApiArt);

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            if (request.getArtifact() == oakpalApiArt) {
                return result;
            } else {
                return null;
            }
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        assertEquals("expect version", expectVersion, mojo.getOakpalCoreVersion());
    }
    @Test
    public void testGetOakpalCoreVersionWithCoreAndApi() {
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final String expectApiVersion = "0.31415926535898";
        final String expectCoreVersion = "1.31415926535898";

        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;
        final Dependency oakpalApiDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_API_ARTIFACT_ID)
                .withVersion(expectApiVersion)
                .toDependency();
        final Artifact oakpalApiArt = mock(Artifact.class);
        when(oakpalApiArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalApiArt.getArtifactId()).thenReturn(OAKPAL_API_ARTIFACT_ID);
        when(oakpalApiArt.getVersion()).thenReturn(expectApiVersion);
        when(repositorySystem.createDependencyArtifact(oakpalApiDep)).thenReturn(oakpalApiArt);

        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectCoreVersion)
                .toDependency();
        final Artifact oakpalCoreArt = mock(Artifact.class);
        when(oakpalCoreArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalCoreArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalCoreArt.getVersion()).thenReturn(expectCoreVersion);
        when(repositorySystem.createDependencyArtifact(oakpalCoreDep)).thenReturn(oakpalCoreArt);

        final Dependency oakpalApiDepTrans = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_API_ARTIFACT_ID)
                .withVersion(expectCoreVersion)
                .toDependency();
        final Artifact oakpalApiArtTrans = mock(Artifact.class);
        when(oakpalApiArtTrans.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalApiArtTrans.getArtifactId()).thenReturn(OAKPAL_API_ARTIFACT_ID);
        when(oakpalApiArtTrans.getVersion()).thenReturn(expectCoreVersion);
        when(repositorySystem.createDependencyArtifact(oakpalApiDepTrans)).thenReturn(oakpalApiArtTrans);

        final ArtifactResolutionResult apiOnlyResult = mock(ArtifactResolutionResult.class);
        when(apiOnlyResult.getArtifacts()).thenReturn(Collections.singleton(oakpalApiArt));
        final ArtifactResolutionResult coreOnlyResult = mock(ArtifactResolutionResult.class);
        when(coreOnlyResult.getArtifacts()).thenReturn(Collections.singleton(oakpalCoreArt));
        final ArtifactResolutionResult coreTransResult = mock(ArtifactResolutionResult.class);
        when(coreTransResult.getArtifacts()).thenReturn(new HashSet<>(Arrays.asList(oakpalCoreArt, oakpalApiArtTrans)));
        final ArtifactResolutionResult apiTransOnlyResult = mock(ArtifactResolutionResult.class);
        when(apiTransOnlyResult.getArtifacts()).thenReturn(Collections.singleton(oakpalApiArtTrans));

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            if (request.getArtifact() == oakpalApiArt) {
                return apiOnlyResult;
            } else if (request.getArtifact() == oakpalApiArtTrans) {
                return apiTransOnlyResult;
            } else if (request.getArtifact() == oakpalCoreArt) {
                if (request.isResolveTransitively()) {
                    return coreTransResult;
                } else {
                    return coreOnlyResult;
                }
            } else {
                return null;
            }
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        when(project.getDependencies()).thenReturn(Collections.singletonList(oakpalApiDep));
        assertEquals("expect api version", expectApiVersion, mojo.getOakpalCoreVersion());
        when(project.getDependencies()).thenReturn(Collections.singletonList(oakpalCoreDep));
        assertEquals("expect core version", expectCoreVersion, mojo.getOakpalCoreVersion());
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalCoreDep, oakpalApiDepTrans));
        assertEquals("expect core version", expectCoreVersion, mojo.getOakpalCoreVersion());
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalApiDep, oakpalCoreDep, oakpalApiDepTrans));
        assertEquals("expect api version", expectApiVersion, mojo.getOakpalCoreVersion());
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalApiDep, oakpalApiDepTrans));
        assertEquals("expect api version", expectApiVersion, mojo.getOakpalCoreVersion());
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalApiDepTrans, oakpalApiDep));
        assertEquals("expect core version", expectCoreVersion, mojo.getOakpalCoreVersion());
    }

    @Test
    public void testAssembleOpear_asOpear() throws Exception {
        final File preInstallPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final File testOutDir = new File(testOutBaseDir, "testAssembleOpear_asOpear");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        when(project.getPackaging()).thenReturn(OPEAR);
        final String expectGroupId = "com.example.testAssembleOpear";
        final String expectArtifactId = "assembly1";
        final String expectVersion = "1.0-SNAPSHOT";
        when(project.getGroupId()).thenReturn(expectGroupId);
        when(project.getArtifactId()).thenReturn(expectArtifactId);
        when(project.getVersion()).thenReturn(expectVersion);
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        final File basedir = new File(testOutDir, "basedir");
        final File projectFile = new File(basedir, "pom.xml");
        FileUtils.touch(projectFile);
        when(project.getFile()).thenReturn(projectFile);

        final File target = new File(basedir, "target");
        target.mkdirs();
        mojo.outputDirectory = target;
        final File targetClasses = new File(target, "classes");
        targetClasses.mkdirs();
        final Build build = mock(Build.class);
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn(target.getPath());
        when(build.getOutputDirectory()).thenReturn(targetClasses.getPath());
        FileUtils.copyFile(new File(srcDir, "echo.js"), new File(targetClasses, "echo.js"));

        final String expectFinalName = expectArtifactId + "-" + expectVersion;
        mojo.finalName = expectFinalName;

        final Artifact projectArtifact = mock(Artifact.class);
        when(projectArtifact.getType()).thenReturn(OPEAR);
        when(project.getArtifact()).thenReturn(projectArtifact);

        final String expectOakpalVersion = "0.31415926535898";
        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectOakpalVersion)
                .toDependency();
        final File oakpalDepFile = new File(testOutDir, "oakpal-core.jar");
        final Artifact oakpalArt = mock(Artifact.class);
        when(oakpalArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalArt.getVersion()).thenReturn(expectOakpalVersion);
        when(oakpalArt.getScope()).thenReturn("compile");
        when(oakpalArt.getFile()).thenReturn(oakpalDepFile);

        final Dependency compileDep = new DependencyFilter().toDependency("compile");
        final File compileDepFile = new File(testOutDir, "compile-scope.jar");
        FileUtils.touch(compileDepFile);
        final Artifact compileArt = mock(Artifact.class);
        when(compileArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(compileArt.getArtifactId()).thenReturn("compile-scope");
        when(compileArt.getScope()).thenReturn("compile");
        when(compileArt.getFile()).thenReturn(compileDepFile);
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalCoreDep, compileDep));
        doAnswer(call -> {
            final Dependency input = call.getArgument(0, Dependency.class);
            if (input == oakpalCoreDep) {
                return oakpalArt;
            } else if (input == compileDep) {
                return compileArt;
            } else {
                return null;
            }
        }).when(repositorySystem).createDependencyArtifact(any(Dependency.class));

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
            when(result.getArtifacts()).thenReturn(Collections.singleton(request.getArtifact()));
            return result;
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        final String expectFilename = "sample-plan.json";
        final File samplePlanFile = new File(srcDir, expectFilename);
        mojo.planFile = samplePlanFile;

        when(project.clone()).thenReturn(project);
        final File expectFinalFile = new File(target, expectFinalName + "." + OPEAR);
        assertEquals("", expectFinalFile, mojo.assembleOpear());
        try (JarFile jarFile = new JarFile(expectFinalFile)) {
            final Set<String> allEntries = jarFile.stream().map(JarEntry::getName).collect(Collectors.toSet());
            final List<String> expectEntries = Arrays.asList(
                    "lib/classes/echo.js",
                    "lib/" + compileDepFile.getName(),
                    "tmp_foo_bar.zip",
                    expectFilename
            );
            for (String expectEntry : expectEntries) {
                assertTrue(String.format("should have entry %s in %s", expectEntry, allEntries),
                        jarFile.stream().anyMatch(entry -> expectEntry.equals(entry.getName())));
            }
            final List<String> expectNoEntries = Arrays.asList(
                    "lib/" + oakpalDepFile.getName()
            );
            for (String noExpectEntry : expectNoEntries) {
                assertTrue(String.format("should not have entry %s in %s", noExpectEntry, allEntries),
                        jarFile.stream().noneMatch(entry -> noExpectEntry.equals(entry.getName())));
            }
            final Manifest manifest = jarFile.getManifest();
            assertEquals("expect bsn", Collections.singletonList(expectArtifactId),
                    Util.getManifestHeaderValues(manifest, "Bundle-SymbolicName"));
            assertEquals("expect bcp", Arrays.asList("lib/classes", "lib/" + compileDepFile.getName()),
                    Util.getManifestHeaderValues(manifest, "Bundle-ClassPath"));
            assertEquals("expect oakpal version", Collections.singletonList(expectOakpalVersion),
                    Util.getManifestHeaderValues(manifest, "Oakpal-Version"));
            assertEquals("expect oakpal plan", Collections.singletonList(expectFilename),
                    Util.getManifestHeaderValues(manifest, "Oakpal-Plan"));
        }
    }

    @Test
    public void testAssembleOpear_asJar() throws Exception {
        final File preInstallPackage = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final File testOutDir = new File(testOutBaseDir, "testAssembleOpear_asJar");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        when(project.getPackaging()).thenReturn("jar");
        final String expectGroupId = "com.example.testAssembleOpear";
        final String expectArtifactId = "assembly1";
        final String expectVersion = "1.0-SNAPSHOT";
        when(project.getGroupId()).thenReturn(expectGroupId);
        when(project.getArtifactId()).thenReturn(expectArtifactId);
        when(project.getVersion()).thenReturn(expectVersion);
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        final File basedir = new File(testOutDir, "basedir");
        final File projectFile = new File(basedir, "pom.xml");
        FileUtils.touch(projectFile);
        when(project.getFile()).thenReturn(projectFile);

        final File target = new File(basedir, "target");
        target.mkdirs();
        mojo.outputDirectory = target;
        final File targetClasses = new File(target, "classes");
        targetClasses.mkdirs();
        final Build build = mock(Build.class);
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn(target.getPath());
        when(build.getOutputDirectory()).thenReturn(targetClasses.getPath());
        FileUtils.copyFile(new File(srcDir, "echo.js"), new File(targetClasses, "echo.js"));

        final String expectFinalName = expectArtifactId + "-" + expectVersion;
        mojo.finalName = expectFinalName;
        final File fakeJar = new File(target, expectFinalName + ".jar");
        FileUtils.touch(fakeJar);

        final Artifact projectArtifact = mock(Artifact.class);
        when(projectArtifact.getType()).thenReturn("jar");
        when(project.getArtifact()).thenReturn(projectArtifact);
        when(projectArtifact.getFile()).thenReturn(fakeJar);

        final String expectOakpalVersion = "0.31415926535898";
        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectOakpalVersion)
                .toDependency();
        final File oakpalDepFile = new File(testOutDir, "oakpal-core.jar");
        final Artifact oakpalArt = mock(Artifact.class);
        when(oakpalArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalArt.getVersion()).thenReturn(expectOakpalVersion);
        when(oakpalArt.getScope()).thenReturn("compile");
        when(oakpalArt.getFile()).thenReturn(oakpalDepFile);

        final Dependency compileDep = new DependencyFilter().toDependency("compile");
        final File compileDepFile = new File(testOutDir, "compile-scope.jar");
        FileUtils.touch(compileDepFile);
        final Artifact compileArt = mock(Artifact.class);
        when(compileArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(compileArt.getArtifactId()).thenReturn("compile-scope");
        when(compileArt.getScope()).thenReturn("compile");
        when(compileArt.getFile()).thenReturn(compileDepFile);
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalCoreDep, compileDep));
        doAnswer(call -> {
            final Dependency input = call.getArgument(0, Dependency.class);
            if (input == oakpalCoreDep) {
                return oakpalArt;
            } else if (input == compileDep) {
                return compileArt;
            } else {
                return null;
            }
        }).when(repositorySystem).createDependencyArtifact(any(Dependency.class));

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
            when(result.getArtifacts()).thenReturn(Collections.singleton(request.getArtifact()));
            return result;
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        final String expectFilename = "sample-plan.json";
        final File samplePlanFile = new File(srcDir, expectFilename);
        mojo.planFile = samplePlanFile;

        when(project.clone()).thenReturn(project);
        final File expectFinalFile = new File(target, expectFinalName + "." + OPEAR);
        assertEquals("", expectFinalFile, mojo.assembleOpear());
        try (JarFile jarFile = new JarFile(expectFinalFile)) {
            final Set<String> allEntries = jarFile.stream().map(JarEntry::getName).collect(Collectors.toSet());
            final List<String> expectEntries = Arrays.asList(
                    "lib/" + fakeJar.getName(),
                    "lib/" + compileDepFile.getName(),
                    "tmp_foo_bar.zip",
                    expectFilename
            );
            for (String expectEntry : expectEntries) {
                assertTrue(String.format("should have entry %s in %s", expectEntry, allEntries),
                        jarFile.stream().anyMatch(entry -> expectEntry.equals(entry.getName())));
            }
            final List<String> expectNoEntries = Arrays.asList(
                    "lib/classes/",
                    "lib/classes/echo.js",
                    "lib/" + oakpalDepFile.getName()
            );
            for (String noExpectEntry : expectNoEntries) {
                assertTrue(String.format("should not have entry %s in %s", noExpectEntry, allEntries),
                        jarFile.stream().noneMatch(entry -> noExpectEntry.equals(entry.getName())));
            }
            final Manifest manifest = jarFile.getManifest();
            assertEquals("expect bsn", Collections.singletonList(expectArtifactId + "-" + OPEAR),
                    Util.getManifestHeaderValues(manifest, "Bundle-SymbolicName"));
            assertEquals("expect bcp", Arrays.asList("lib/" + fakeJar.getName(), "lib/" + compileDepFile.getName()),
                    Util.getManifestHeaderValues(manifest, "Bundle-ClassPath"));
            assertEquals("expect oakpal version", Collections.singletonList(expectOakpalVersion),
                    Util.getManifestHeaderValues(manifest, "Oakpal-Version"));
            assertEquals("expect oakpal plan", Collections.singletonList(expectFilename),
                    Util.getManifestHeaderValues(manifest, "Oakpal-Plan"));
        }
    }

    @Test(expected = Exception.class)
    public void testAssembleOpear_asJar_stillDir() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testAssembleOpear_asJar_stillDir");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        when(project.getPackaging()).thenReturn("jar");
        final String expectGroupId = "com.example.testAssembleOpear";
        final String expectArtifactId = "assembly1";
        final String expectVersion = "1.0-SNAPSHOT";
        when(project.getGroupId()).thenReturn(expectGroupId);
        when(project.getArtifactId()).thenReturn(expectArtifactId);
        when(project.getVersion()).thenReturn(expectVersion);
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        final File basedir = new File(testOutDir, "basedir");
        final File projectFile = new File(basedir, "pom.xml");
        FileUtils.touch(projectFile);
        when(project.getFile()).thenReturn(projectFile);

        final Artifact projectArtifact = mock(Artifact.class);
        when(projectArtifact.getType()).thenReturn("jar");
        when(project.getArtifact()).thenReturn(projectArtifact);

        final File target = new File(basedir, "target");
        target.mkdirs();
        mojo.outputDirectory = target;
        final File targetClasses = new File(target, "classes");
        targetClasses.mkdirs();

        when(projectArtifact.getFile()).thenReturn(targetClasses);

        final String expectFinalName = expectArtifactId + "-" + expectVersion;
        mojo.finalName = expectFinalName;

        final String expectOakpalVersion = "0.31415926535898";
        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectOakpalVersion)
                .toDependency();
        final File oakpalDepFile = new File(testOutDir, "oakpal-core.jar");
        final Artifact oakpalArt = mock(Artifact.class);
        when(oakpalArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalArt.getVersion()).thenReturn(expectOakpalVersion);
        when(oakpalArt.getScope()).thenReturn("compile");
        when(oakpalArt.getFile()).thenReturn(oakpalDepFile);

        final Dependency compileDep = new DependencyFilter().toDependency("compile");
        final File compileDepFile = new File(testOutDir, "compile-scope.jar");
        FileUtils.touch(compileDepFile);
        final Artifact compileArt = mock(Artifact.class);
        when(compileArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(compileArt.getArtifactId()).thenReturn("compile-scope");
        when(compileArt.getScope()).thenReturn("compile");
        when(compileArt.getFile()).thenReturn(compileDepFile);
        when(project.getDependencies()).thenReturn(Arrays.asList(oakpalCoreDep, compileDep));
        doAnswer(call -> {
            final Dependency input = call.getArgument(0, Dependency.class);
            if (input == oakpalCoreDep) {
                return oakpalArt;
            } else if (input == compileDep) {
                return compileArt;
            } else {
                return null;
            }
        }).when(repositorySystem).createDependencyArtifact(any(Dependency.class));

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
            when(result.getArtifacts()).thenReturn(Collections.singleton(request.getArtifact()));
            return result;
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        final String expectFilename = "sample-plan.json";
        final File samplePlanFile = new File(srcDir, expectFilename);
        mojo.planFile = samplePlanFile;

        when(project.clone()).thenReturn(project);
        mojo.assembleOpear();
    }

    @Test
    public void testAttachArtifact() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testAttachArtifact");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final String expectGroupId = "com.example.artifacts";
        when(project.getGroupId()).thenReturn(expectGroupId);
        final String expectArtifactId = "sample-project";
        when(project.getArtifactId()).thenReturn(expectArtifactId);
        final String expectVersion = "0.314159";
        when(project.getVersion()).thenReturn(expectVersion);
        when(project.getPackaging()).thenReturn("jar");

        final ArtifactHandlerManager artifactHandlerManager = mock(ArtifactHandlerManager.class);
        mojo.artifactHandlerManager = artifactHandlerManager;
        final ArtifactHandler opearHandler = mock(ArtifactHandler.class);
        when(artifactHandlerManager.getArtifactHandler(OPEAR)).thenReturn(opearHandler);

        final CompletableFuture<Artifact> attachSlot = new CompletableFuture<>();
        doAnswer(call -> attachSlot.complete(call.getArgument(0, Artifact.class)))
                .when(project).addAttachedArtifact(any(Artifact.class));

        final File finalFile = new File(testOutDir, "finalFile.opear");
        final Artifact jarAttached = mojo.attachArtifact(finalFile);
        assertSame("same attachment", jarAttached, attachSlot.getNow(null));
        assertSame("same file", finalFile, jarAttached.getFile());
        assertSame("same handler", opearHandler, jarAttached.getArtifactHandler());
        assertEquals("same groupId", project.getGroupId(), jarAttached.getGroupId());
        assertEquals("same artifactId", project.getArtifactId(), jarAttached.getArtifactId());
        assertEquals("same version", project.getVersion(), jarAttached.getVersion());
        assertEquals("same type", OPEAR, jarAttached.getType());

        final Artifact projectArtifact = mock(Artifact.class);
        when(project.getArtifact()).thenReturn(projectArtifact);
        final CompletableFuture<File> fileSlot = new CompletableFuture<>();
        doAnswer(call -> fileSlot.complete(call.getArgument(0, File.class)))
                .when(projectArtifact).setFile(any(File.class));
        final CompletableFuture<ArtifactHandler> handlerSlot = new CompletableFuture<>();
        doAnswer(call -> handlerSlot.complete(call.getArgument(0, ArtifactHandler.class)))
                .when(projectArtifact).setArtifactHandler(any(ArtifactHandler.class));

        when(project.getPackaging()).thenReturn(OPEAR);
        final Artifact opearAttached = mojo.attachArtifact(finalFile);
        assertSame("same project artifact", projectArtifact, opearAttached);
        assertSame("same artifact handler", opearHandler, handlerSlot.getNow(null));
    }

    @Test
    public void testGetEmbeddedLibraries() {
        final File testOutDir = new File(testOutBaseDir, "testGetEmbeddedLibraries");
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        final MavenSession session = mock(MavenSession.class);
        mojo.session = session;
        final MavenExecutionRequest executionRequest = mock(MavenExecutionRequest.class);
        when(session.getRequest()).thenReturn(executionRequest);
        final RepositorySystem repositorySystem = mock(RepositorySystem.class);
        mojo.repositorySystem = repositorySystem;

        final String expectVersion = "0.31415926535898";
        final Dependency oakpalCoreDep = new DependencyFilter()
                .withGroupId(OAKPAL_GROUP_ID)
                .withArtifactId(OAKPAL_CORE_ARTIFACT_ID)
                .withVersion(expectVersion)
                .toDependency();
        final File oakpalDepFile = new File(testOutDir, "oakpal-core.jar");
        final Artifact oakpalArt = mock(Artifact.class);
        when(oakpalArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(oakpalArt.getArtifactId()).thenReturn(OAKPAL_CORE_ARTIFACT_ID);
        when(oakpalArt.getScope()).thenReturn("compile");
        when(oakpalArt.getFile()).thenReturn(oakpalDepFile);

        final Dependency importDep = new DependencyFilter().toDependency("import");
        final File importDepFile = new File(testOutDir, "import-scope.jar");
        final Artifact importArt = mock(Artifact.class);
        when(importArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(importArt.getArtifactId()).thenReturn("import-scope");
        when(importArt.getScope()).thenReturn("import");
        when(importArt.getFile()).thenReturn(importDepFile);

        final Dependency providedDep = new DependencyFilter().toDependency("provided");
        final File providedDepFile = new File(testOutDir, "provided-scope.jar");
        final Artifact providedArt = mock(Artifact.class);
        when(providedArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(providedArt.getArtifactId()).thenReturn("provided-scope");
        when(providedArt.getScope()).thenReturn("provided");
        when(providedArt.getFile()).thenReturn(providedDepFile);

        final Dependency testDep = new DependencyFilter().toDependency("test");
        final File testDepFile = new File(testOutDir, "test-scope.jar");
        final Artifact testArt = mock(Artifact.class);
        when(testArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(testArt.getArtifactId()).thenReturn("test-scope");
        when(testArt.getScope()).thenReturn("test");
        when(testArt.getFile()).thenReturn(testDepFile);

        final Dependency compileDep = new DependencyFilter().toDependency("compile");
        final File compileDepFile = new File(testOutDir, "compile-scope.jar");
        final Artifact compileArt = mock(Artifact.class);
        when(compileArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(compileArt.getArtifactId()).thenReturn("compile-scope");
        when(compileArt.getScope()).thenReturn("compile");
        when(compileArt.getFile()).thenReturn(compileDepFile);

        final Dependency runtimeDep = new DependencyFilter().toDependency("runtime");
        final File runtimeDepFile = new File(testOutDir, "runtime-scope.jar");
        final Artifact runtimeArt = mock(Artifact.class);
        when(runtimeArt.getGroupId()).thenReturn(OAKPAL_GROUP_ID);
        when(runtimeArt.getArtifactId()).thenReturn("runtime-scope");
        when(runtimeArt.getScope()).thenReturn("runtime");
        when(runtimeArt.getFile()).thenReturn(runtimeDepFile);

        final List<Dependency> projectDependencies =
                Arrays.asList(oakpalCoreDep, importDep, providedDep, testDep, importDep,
                        compileDep, runtimeDep);
        when(project.getDependencies()).thenReturn(projectDependencies);

        doAnswer(call -> {
            final Dependency input = call.getArgument(0, Dependency.class);
            if (input == oakpalCoreDep) {
                return oakpalArt;
            } else if (input == importDep) {
                return importArt;
            } else if (input == providedDep) {
                return providedArt;
            } else if (input == testDep) {
                return testArt;
            } else if (input == compileDep) {
                return compileArt;
            } else if (input == runtimeDep) {
                return runtimeArt;
            } else {
                return null;
            }
        }).when(repositorySystem).createDependencyArtifact(any(Dependency.class));

        doAnswer(call -> {
            final ArtifactResolutionRequest request = call.getArgument(0, ArtifactResolutionRequest.class);
            final ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
            when(result.getArtifacts()).thenReturn(Collections.singleton(request.getArtifact()));
            return result;
        }).when(repositorySystem).resolve(any(ArtifactResolutionRequest.class));

        final List<File> actualLibraries = mojo.getEmbeddedLibraries();
        assertFalse("not empty: " + actualLibraries, actualLibraries.isEmpty());
        assertEquals("expect files", Arrays.asList(compileDepFile, runtimeDepFile), actualLibraries);

    }

    @Test
    public void testShrinkWrapPlans() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testShrinkWrapPlans");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File copyTarget = new File(testOutDir, "copied");
        copyTarget.mkdirs();

        final String preInstallFilename = "tmp_foo_bar.zip";
        final File preInstallPackage = TestPackageUtil.prepareTestPackage(preInstallFilename);

        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;

        final String expectFilename = "sample-plan.json";
        final File samplePlanFile = new File(srcDir, expectFilename);
        mojo.planFile = samplePlanFile;
        final int addPlans = 10;
        for (int i = 0; i < addPlans; i++) {
            mojo.additionalPlans.add(samplePlanFile);
        }

        List<String> expectedPlanNames = new ArrayList<>();
        expectedPlanNames.add(expectFilename);
        for (int i = 0; i < addPlans; i++) {
            expectedPlanNames.add("sample-plan_" + (i+1) + ".json");
        }

        Result<List<String>> planNamesResult = mojo.shrinkWrapPlans(copyTarget);
        assertTrue("is success: " + planNamesResult, planNamesResult.isSuccess());
        assertEquals("expect plan names",
                expectedPlanNames,
                planNamesResult.getOrDefault(Collections.emptyList()));
        assertTrue("preinstall is file", new File(copyTarget, preInstallFilename).isFile());
        File[] otherZips = copyTarget.listFiles((dir, name) -> !preInstallFilename.equals(name) && name.endsWith(".zip"));
        assertNotNull("other zips should not be null", otherZips);
        assertEquals("other zips should be empty", 0, otherZips.length);
    }

    @Test
    public void testShrinkWrapPlans_nodefault() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testShrinkWrapPlans_nodefault");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File copyTarget = new File(testOutDir, "copied");
        copyTarget.mkdirs();

        final String preInstallFilename = "tmp_foo_bar.zip";
        final File preInstallPackage = TestPackageUtil.prepareTestPackage(preInstallFilename);

        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;

        final String expectFilename = "sample-plan.json";
        final File samplePlanFile = new File(srcDir, expectFilename);
        mojo.planFile = new File(testOutDir, "no-plan.json");
        final int addPlans = 10;
        for (int i = 0; i < addPlans; i++) {
            mojo.additionalPlans.add(samplePlanFile);
        }

        List<String> expectedPlanNames = new ArrayList<>();
        expectedPlanNames.add(expectFilename);
        for (int i = 1; i < addPlans; i++) {
            expectedPlanNames.add("sample-plan_" + i + ".json");
        }

        Result<List<String>> planNamesResult = mojo.shrinkWrapPlans(copyTarget);
        assertTrue("is success: " + planNamesResult, planNamesResult.isSuccess());
        assertEquals("expect plan names",
                expectedPlanNames,
                planNamesResult.getOrDefault(Collections.emptyList()));
        assertTrue("preinstall is file", new File(copyTarget, preInstallFilename).isFile());
    }

    @Test
    public void testRewritePlan() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testRewritePlan");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File copyTarget = new File(testOutDir, "copied");
        copyTarget.mkdirs();

        final String expectFilename = "sample-plan.json";
        final URL samplePlanUrl = new File(srcDir, expectFilename).toURI().toURL();
        final Result<OakpalPlan> simplePlanResult = OakpalPlan.fromJson(samplePlanUrl);
        assertTrue("plan is successful", simplePlanResult.isSuccess());
        final OakpalPlan simplePlan = simplePlanResult.getOrDefault(null);

        final String preInstallFilename = "tmp_foo_bar.zip";
        final File preInstallPackage = TestPackageUtil.prepareTestPackage(preInstallFilename);
        final URL preInstallUrl = new URL("oakpaltest:/target/test-packages/" + preInstallPackage.getName());

        final URL repoInitUrl1 = new URL("oakpaltest:/target/test-classes/OpearPackageMojoTest/repoinit1.txt");
        final URL repoInitUrl2 = new URL("oakpaltest:/target/test-classes/OpearPackageMojoTest/repoinit2.txt");

        final String rewrittenName = "my-plan.json";
        final String rewrittenPreInstallName = "my_preinstall.zip";
        final String rewrittenRepoInitName1 = "repoinit1.txt";
        final String rewrittenRepoInitName2 = "repoinit2.txt";
        final List<String> rewrittenRepoInitNames = Arrays.asList(rewrittenRepoInitName1, rewrittenRepoInitName2);

        final Map<URL, String> rewrites = new HashMap<>();
        rewrites.put(preInstallUrl, rewrittenPreInstallName);
        rewrites.put(repoInitUrl1, rewrittenRepoInitName1);
        rewrites.put(repoInitUrl2, rewrittenRepoInitName2);
        OakpalPlan rewritten = OpearPackageMojo.rewritePlan(copyTarget, rewrites, simplePlan, rewrittenName);

        assertEquals("expect new preinstall url",
                new File(copyTarget, rewrittenPreInstallName).getAbsoluteFile().toURI().toURL(),
                rewritten.getPreInstallUrls().get(0));

        assertEquals("expect relativized preinstall url", rewrittenPreInstallName,
                rewritten.toJson().getJsonArray("preInstallUrls").getString(0));

        assertEquals("expect new repoInitUrl1",
                new File(copyTarget, rewrittenRepoInitName1).getAbsoluteFile().toURI().toURL(),
                rewritten.getRepoInitUrls().get(0));
        assertEquals("expect new repoInitUrl2",
                new File(copyTarget, rewrittenRepoInitName2).getAbsoluteFile().toURI().toURL(),
                rewritten.getRepoInitUrls().get(1));

        assertEquals("expect relativized repoInitUrls", rewrittenRepoInitNames,
                JavaxJson.unwrapArray(rewritten.toJson().getJsonArray("repoInitUrls")));
    }

    @Test
    public void testCopyPlans() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testCopyPlans");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();
        final File copyTarget = new File(testOutDir, "copied");
        copyTarget.mkdirs();
        final String expectFilename = "sample-plan.json";
        final URL samplePlanUrl = new File(srcDir, expectFilename).toURI().toURL();
        final Result<OakpalPlan> simplePlan = OakpalPlan.fromJson(samplePlanUrl);

        Result<Map<String, OakpalPlan>> copyResult = OpearPackageMojo.copyPlans(copyTarget,
                Arrays.asList(simplePlan.getOrDefault(null)));
        assertTrue("is success " + copyResult, copyResult.isSuccess());
        Map<String, OakpalPlan> copyMap = copyResult.getOrDefault(Collections.emptyMap());

        assertTrue("contains key: " + copyMap.keySet(),
                copyMap.containsKey(expectFilename));

        final File destFile = new File(copyTarget, expectFilename);
        assertTrue("file exists: " + destFile, destFile.isFile());
    }

    @Test
    public void testGetBundleSymbolicName() {
        final OpearPackageMojo mojo = newMojo();
        final MavenProject project = mock(MavenProject.class);
        mojo.project = project;
        when(project.getArtifactId()).thenReturn("some-artifactId");
        when(project.getPackaging()).thenReturn("jar");
        assertEquals("expect -opear classifier", "some-artifactId-opear", mojo.getBundleSymbolicName());
        when(project.getPackaging()).thenReturn("opear");
        assertEquals("expect no classifier", "some-artifactId", mojo.getBundleSymbolicName());
    }

    @Test
    public void testCopyUrlStreams() throws Exception {
        final File testOutDir = new File(testOutBaseDir, "testCopyUrlStreams");
        FileUtils.deleteDirectory(testOutDir);
        testOutDir.mkdirs();

        final File copyMe1 = new File(srcDir, "copyme.txt");
        final File copyMe2 = new File(srcDir, "sub1/copyme.txt");

        final Result<Map<URL, String>> result = OpearPackageMojo.copyUrlStreams(testOutDir,
                Stream.of(copyMe1, copyMe2)
                        .map(Fun.compose1(File::toURI, Fun.uncheck1(URI::toURL)))
                        .collect(Collectors.toList())).teeLogError();
        assertTrue("result is successful: " + result.getError(), result.isSuccess());
        assertEquals("same values",
                Stream.of("copyme.txt", "copyme_1.txt")
                        .collect(Collectors.toSet()),
                new HashSet<>(result.getOrDefault(Collections.emptyMap()).values()));
    }

    @Test
    public void testOpearArchiver_construct() {
        OpearPackageMojo.OpearArchiver archiver = new OpearPackageMojo.OpearArchiver();
    }
}