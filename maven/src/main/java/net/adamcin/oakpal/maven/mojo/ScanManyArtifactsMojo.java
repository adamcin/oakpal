/*
 * Copyright 2018 Mark Adamcin
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

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Scans a list of artifacts by simulating package installation and listening for violations reported by the
 * configured {@code checks}. This goal supports the use of {@link ProgressCheck}s that must
 * evaluate the side effects of installing packages in combination, such as for detection of workspace filter overlap.
 * More simply, this goal can be used in a sidecar project to scan multiple artifacts produced by previous builds using
 * a common library of checklists.
 *
 * @since 0.3.0
 */
@Mojo(name = "scan-many", configurator = OakpalComponentConfigurator.HINT,
        requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ScanManyArtifactsMojo extends AbstractITestWithPlanMojo {

    /**
     * Specifically skip this plugin's execution.
     */
    @Parameter(property = "oakpal.scan-many.skip")
    boolean skip;

    /**
     * Specify a list of content-package artifacts to download and scan in sequence.
     * <p>
     * For example:
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
     */
    @Parameter(name = "scanArtifacts")
    List<Dependency> scanArtifacts = new ArrayList<>();

    /**
     * Specify a list of local package files to add to the list to scan. These will be installed after those listed in
     * {@code scanArtifacts}.
     * <p>
     * For example:
     * <pre>
     * &lt;scanFiles&gt;
     *   &lt;scanFile&gt;target/myPackages/firstPackage.zip&lt;/scanFile&gt;
     *   &lt;scanFile&gt;target/myPackages/secondPackage.zip&lt;/scanFile&gt;
     * &lt;/scanFiles&gt;
     * </pre>
     */
    @Parameter(name = "scanFiles")
    List<File> scanFiles = new ArrayList<>();

    @Override
    protected boolean isIndividuallySkipped() {
        return skip;
    }

    final List<File> listScanFiles() throws MojoFailureException {
        List<File> resolvedArtifacts = new ArrayList<>();

        if (scanArtifacts != null && !scanArtifacts.isEmpty()) {
            resolvedArtifacts.addAll(resolveDependencies(scanArtifacts, false));
        }

        if (scanFiles != null && !scanFiles.isEmpty()) {
            resolvedArtifacts.addAll(scanFiles);
        }

        return resolvedArtifacts;
    }

    @Override
    protected void executeGuardedIntegrationTest() throws MojoFailureException {
        performScan(listScanFiles());
    }
}
