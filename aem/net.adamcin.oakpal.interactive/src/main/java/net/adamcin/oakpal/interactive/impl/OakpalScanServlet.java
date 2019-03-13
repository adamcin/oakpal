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
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.interactive.OakpalInteractiveConstants;
import net.adamcin.oakpal.interactive.OakpalScanResult;
import net.adamcin.oakpal.interactive.OakpalScanService;
import net.adamcin.oakpal.interactive.models.OakpalScanInputResource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple POST servlet to scan a series of packages available in CRX Package Manager.
 */
@Component(service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=" + OakpalInteractiveConstants.RT_OAKPAL_SCAN_INPUT,
                "sling.servlet.selectors=scan",
                "sling.servlet.extensions=json",
                "sling.servlet.methods=POST"
        })
class OakpalScanServlet extends SlingAllMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(OakpalScanServlet.class);

    /**
     * The singleton packaging service is used to retrieve specified packages, but it should not be used as the
     * packaging service for the scan.
     */
    @Reference
    private OakpalScanService oakpalScanService;

    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response)
            throws ServletException, IOException {

        final Resource scanResource = request.getResource();
        final OakpalScanInputResource oakpalScan = scanResource.adaptTo(OakpalScanInputResource.class);

        if (oakpalScan == null) {
            response.sendError(404);
            return;
        }

        try {
            OakpalScanResult result = oakpalScanService.performScan(request.getResourceResolver(), oakpalScan);
            ReportMapper.writeReports(result.getReports(), response::getWriter);
        } catch (final AbortedScanException e) {
            throw new ServletException("failed to complete scan", e);
        }
    }
}
