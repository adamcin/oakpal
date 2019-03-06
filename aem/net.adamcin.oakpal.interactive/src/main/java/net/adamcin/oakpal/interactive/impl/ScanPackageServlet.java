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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.core.DefaultPackagingService;
import net.adamcin.oakpal.core.Locator;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.interactive.ChecklistTracker;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple POST servlet to scan a series of packages available in CRX Package Manager.
 */
@Component(service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/oakpal/scan-package",
                "sling.servlet.methods=POST"
        })
public class ScanPackageServlet extends SlingAllMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanPackageServlet.class);

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
    protected void doPost(@NotNull final SlingHttpServletRequest request,
                          @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {

        // use the Sling dynamic classloader for loading ProgressChecks
        final ClassLoader checkLoader = classLoaderManager.getDynamicClassLoader();

        final List<String> activeChecklistIds =
                Arrays.asList(Optional.ofNullable(request.getParameterValues("checklist")).orElse(new String[0]));
        ChecklistPlanner planner = new ChecklistPlanner(activeChecklistIds);
        planner.provideChecklists(checklistTracker.getBundleChecklists());

        final List<ProgressCheck> allChecks;
        try {
            allChecks = new ArrayList<>(Locator.loadFromCheckSpecs(
                    planner.getEffectiveCheckSpecs(Collections.emptyList()),
                    checkLoader));
        } catch (final Exception e) {
            throw new ServletException(e);
        }

        final List<Resource> pkgResources = new ArrayList<>();
        final String[] pkgPaths = request.getParameterValues("path");
        for (String pkgPath : pkgPaths) {
            final Resource pkgRes = request.getResourceResolver().getResource(pkgPath);
            pkgResources.add(pkgRes);
        }

        final OakMachine.Builder builder = new OakMachine.Builder();
        builder.withInitStages(planner.getInitStages());
        builder.withProgressChecks(allChecks);
        // create an instance of our reflection-based Packaging service to wrap the legacy packaging service using
        // using the bound Packaging service's classloader in order to avoid the nasty no-no stack trace on the left, and
        // the package event dispatcher on the right.
        builder.withPackagingService(new DefaultPackagingService(packagingService.getClass().getClassLoader()));

        final OakMachine machine = builder.build();

        try (ScanTempSpace tempSpace = new ScanTempSpace(pkgResources)) {
            List<CheckReport> reportList = machine.scanPackages(tempSpace.open());
            ReportMapper.writeReports(reportList, response::getWriter);
        } catch (final AbortedScanException e) {
            throw new ServletException("failed to complete scan", e);
        }
    }
}
