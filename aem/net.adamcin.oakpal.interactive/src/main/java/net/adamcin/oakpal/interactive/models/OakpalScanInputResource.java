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

package net.adamcin.oakpal.interactive.models;

import java.util.List;
import javax.inject.Inject;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.interactive.OakpalInteractiveConstants;
import net.adamcin.oakpal.interactive.OakpalScanInput;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;

/**
 * Resource-based representation of OakPAL scan input parameters.
 */
@Model(
        adaptables = Resource.class,
        resourceType = OakpalInteractiveConstants.RT_OAKPAL_SCAN_INPUT,
        adapters = {OakpalScanInput.class, OakpalScanInputResource.class})
public final class OakpalScanInputResource implements OakpalScanInput {

    @Optional
    @Inject
    private List<String> packagePaths;

    @Optional
    @Inject
    private List<String> checklists;

    @Optional
    @Inject
    private List<String> preInstallPackagePaths;

    @Optional
    @Inject
    private boolean installPlatformNodetypes;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private List<CheckSpec> checks;

    /**
     * Get the selected checklists.
     *
     * @return the selected checklists
     */
    @Override
    public List<String> getChecklists() {
        return checklists;
    }

    /**
     * Set the selected checklists for the scan.
     *
     * @param checklists the selected checklists for the scan
     */
    public void setChecklists(final List<String> checklists) {
        this.checklists = checklists;
    }

    /**
     * Get the paths to packages selected for pre-install.
     *
     * @return the paths to packages selected for pre-install.
     */
    @Override
    public List<String> getPreInstallPackagePaths() {
        return preInstallPackagePaths;
    }

    /**
     * Set the paths to packages selected for pre-install.
     *
     * @param preInstallPackagePaths the paths to packages selected for pre-install
     */
    public void setPreInstallPackagePaths(final List<String> preInstallPackagePaths) {
        this.preInstallPackagePaths = preInstallPackagePaths;
    }

    /**
     * Get the paths to packages selected for the scan.
     *
     * @return the paths to packages selected for the scan
     */
    @Override
    public List<String> getPackagePaths() {
        return packagePaths;
    }

    /**
     * Set the paths to packages selected for the scan.
     *
     * @param packagePaths the paths to packages selected for the scan
     */
    public void setPackagePaths(final List<String> packagePaths) {
        this.packagePaths = packagePaths;
    }

    /**
     * Get the check specification overlays.
     *
     * @return the check specification overlays
     */
    @Override
    public List<CheckSpec> getChecks() {
        return checks;
    }

    /**
     * Set the check specification overlays.
     *
     * @param checks the check specification overlays.
     */
    public void setChecks(final List<CheckSpec> checks) {
        this.checks = checks;
    }

    /**
     * Returns true if will install all platform nodetypes before the scan.
     *
     * @return true to install all platform nodetypes before the scan.
     */
    @Override
    public boolean isInstallPlatformNodetypes() {
        return installPlatformNodetypes;
    }

    /**
     * Set to true to install all platform nodetypes before the scan.
     *
     * @param installPlatformNodetypes true to install all platform nodetypes before the scan.
     */
    public void setInstallPlatformNodetypes(final boolean installPlatformNodetypes) {
        this.installPlatformNodetypes = installPlatformNodetypes;
    }
}
