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

package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Scans a list of artifacts by simulating package installation and listening for violations reported by the
 * configured {@code scriptReporters}. This goal supports the use of script reporters that must evaluate the side
 * effects of installing packages in combination, such as for detection of workspace filter overlap. More simply,
 * this goal can be used in a sidecar project to scan multiple artifacts produced by previous builds using a common
 * set of script reporters.
 *
 * @since 0.3.0
 */
@Mojo(name = "scan-many", configurator = OakpalComponentConfigurator.HINT,
        requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ScanManyArtifactsMojo extends AbstractScanMojo {

    /**
     * Specifically skip this plugin's execution.
     */
    @Parameter(property = "oakpal.scan-many.skip")
    public boolean skip;

    /**
     * Specify a list of content-package artifacts to download and scan in sequence.
     *
     * For example:
     *
     * <pre>
     * &lt;scanArtifacts&gt;
     *   &lt;scanArtifact&gt;
     *     &lt;groupId&gt;com.acmecorp&lt;/groupId&gt;
     *     &lt;artifactId&gt;acmecorp-common-ui.apps&lt;/artifactId&gt;
     *     &lt;version&gt;0.1.0-SNAPSHOT&lt;/version&gt;
     *     &lt;type&gt;zip&lt;/type&gt;
     *   &lt;/scanArtifact&gt;
     *   &lt;scanArtifact&gt;
     *     &lt;groupId&gt;com.acmecorp&lt;/groupId&gt;
     *     &lt;artifactId&gt;national-site-ui.apps&lt;/artifactId&gt;
     *     &lt;version&gt;0.1.0-SNAPSHOT&lt;/version&gt;
     *     &lt;type&gt;zip&lt;/type&gt;
     *   &lt;/scanArtifact&gt;
     *   &lt;scanArtifact&gt;
     *     &lt;groupId&gt;com.acmecorp&lt;/groupId&gt;
     *     &lt;artifactId&gt;northwest-region-ui.apps&lt;/artifactId&gt;
     *     &lt;version&gt;0.1.0-SNAPSHOT&lt;/version&gt;
     *     &lt;type&gt;zip&lt;/type&gt;
     *   &lt;/scanArtifact&gt;
     * &lt;/scanArtifacts&gt;
     * </pre>
     *
     */
    @Parameter(name = "scanArtifacts")
    protected List<Dependency> scanArtifacts = new ArrayList<>();

    /**
     * Specify a list of local package files to add to the list to scan. These will be installed after those listed in
     * {@code scanArtifacts}.
     *
     * For example:
     *
     * <pre>
     * &lt;scanFiles&gt;
     *   &lt;scanFile&gt;target/myPackages/firstPackage.zip&lt;/scanFile&gt;
     *   &lt;scanFile&gt;target/myPackages/secondPackage.zip&lt;/scanFile&gt;
     * &lt;/scanFiles&gt;
     * </pre>
     *
     */
    @Parameter(name = "scanFiles")
    protected List<File> scanFiles = new ArrayList<>();

    @Override
    protected boolean isIndividuallySkipped() {
        return skip;
    }

    @Override
    protected void executeGuardedIntegrationTest() throws MojoExecutionException, MojoFailureException {

        List<File> resolvedArtifacts = new ArrayList<>();

        if (scanArtifacts != null && !scanArtifacts.isEmpty()) {
            RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(session, project);

            Set<Artifact> preResolved = scanArtifacts.stream()
                    .map(d -> depToArtifact(d, baseRequest, false))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            Optional<Artifact> unresolvedArtifact = preResolved.stream()
                    .filter(a -> a.getFile() == null || !a.getFile().exists())
                    .findFirst();

            if (unresolvedArtifact.isPresent()) {
                Artifact a = unresolvedArtifact.get();
                throw new MojoExecutionException(String.format("Failed to resolve file for artifact: %s:%s:%s",
                        a.getGroupId(), a.getArtifactId(), a.getVersion()));
            }

            List<File> scannableResolved = preResolved.stream()
                    .map(a -> Optional.ofNullable(a.getFile()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(File::exists).collect(Collectors.toList());
            resolvedArtifacts.addAll(scannableResolved);
        }

        if (scanFiles != null && !scanFiles.isEmpty()) {
            resolvedArtifacts.addAll(scanFiles);
        }

        try {
            List<CheckReport> reports = getBuilder().build().scanPackages(resolvedArtifacts);

            reactToReports(reports, true);

        } catch (AbortedScanException e) {
            String currentFilePath = e.getCurrentPackageFile()
                    .map(f -> "Failed package: " + f.getAbsolutePath()).orElse("");
            throw new MojoExecutionException("Failed to execute package scan. " + currentFilePath, e);
        }
    }
}
