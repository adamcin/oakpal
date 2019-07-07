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

package net.adamcin.oakpal.interactive.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.core.DefaultPackagingService;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.Locator;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ScanTempSpace;
import net.adamcin.oakpal.interactive.ChecklistTracker;
import net.adamcin.oakpal.interactive.OakpalScanInput;
import net.adamcin.oakpal.interactive.OakpalScanResult;
import net.adamcin.oakpal.interactive.OakpalScanService;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
class OakpalScanServiceImpl implements OakpalScanService {

    /**
     * The singleton packaging service is used to retrieve specified packages, but it should not be used as the
     * packaging service for the scan.
     */
    @Reference
    private Packaging packagingService;

    /**
     * This bundle can't possibly import all the world's progresscheck namespaces, so we have to either specify
     * {@code Dynamic-ImportPackage: *} in the manifest, or just use the Sling Dynamic ClassLoader service.
     */
    @Reference
    private DynamicClassLoaderManager classLoaderManager;

    /**
     * The checklist tracker service
     */
    @Reference
    private ChecklistTracker checklistTracker;

    @Override
    public OakpalScanResult performScan(final ResourceResolver resolver, final OakpalScanInput input)
            throws IOException, AbortedScanException {

        if (resolver == null) {
            throw new NullPointerException("resolver");
        }
        if (input == null) {
            throw new NullPointerException("input");
        }
        final ScanResult result = new ScanResult(input);
        // use the Sling dynamic classloader for loading ProgressChecks
        final ClassLoader checkLoader = classLoaderManager.getDynamicClassLoader();

        ChecklistPlanner planner = new ChecklistPlanner(input.getChecklists());
        planner.provideChecklists(checklistTracker.getBundleChecklists());

        final List<ProgressCheck> allChecks;
        try {
            allChecks = new ArrayList<>(Locator.loadFromCheckSpecs(
                    planner.getEffectiveCheckSpecs(input.getChecks()),
                    checkLoader));
        } catch (final Exception e) {
            throw new AbortedScanException(e);
        }

        final List<Resource> prePkgResources = new ArrayList<>();
        for (String pkgPath : input.getPreInstallPackagePaths()) {
            final Resource pkgRes = resolver.getResource(pkgPath);
            prePkgResources.add(pkgRes);
        }

        final List<Resource> pkgResources = new ArrayList<>();
        for (String pkgPath : input.getPackagePaths()) {
            final Resource pkgRes = resolver.getResource(pkgPath);
            pkgResources.add(pkgRes);
        }

        final OakMachine.Builder builder = new OakMachine.Builder();
        builder.withPackagingService(new DefaultPackagingService(packagingService.getClass().getClassLoader()));
        builder.withInitStages(planner.getInitStages());
        builder.withProgressChecks(allChecks);

        try (ScanTempSpace<Resource> preInstallSpace = new ScanTempSpace<>(prePkgResources, OakpalScanServiceImpl::adaptResource, null);
             ScanTempSpace<Resource> scanSpace = new ScanTempSpace<>(pkgResources, OakpalScanServiceImpl::adaptResource, null);
             PlatformCndExport cndExport = new PlatformCndExport(resolver)) {

            if (input.isInstallPlatformNodetypes()) {
                builder.withInitStage(new InitStage.Builder().withOrderedCndUrls(cndExport.open()).build());
            }

            final OakMachine machine = builder.withPreInstallPackages(preInstallSpace.open()).build();

            List<CheckReport> reportList = machine.scanPackages(scanSpace.open());

            result.setReports(reportList);
            return result;
        }
    }

    static InputStream adaptResource(final Resource resource) {
        return resource.adaptTo(InputStream.class);
    }

    class ScanResult implements OakpalScanResult {
        private final OakpalScanInput input;
        private List<CheckReport> reports;

        ScanResult(final OakpalScanInput input) {
            this.input = input;
        }

        @Override
        public OakpalScanInput getInput() {
            return input;
        }

        @Override
        public List<CheckReport> getReports() {
            return reports;
        }

        void setReports(final List<CheckReport> reports) {
            this.reports = reports;
        }
    }

}
