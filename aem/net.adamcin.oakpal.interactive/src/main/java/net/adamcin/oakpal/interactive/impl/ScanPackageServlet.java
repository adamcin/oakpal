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

import static net.adamcin.oakpal.core.OrgJson.arr;
import static net.adamcin.oakpal.core.OrgJson.key;
import static net.adamcin.oakpal.core.checks.Rule.RuleType.DENY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.core.checks.Echo;
import net.adamcin.oakpal.core.checks.Paths;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = "sling.servlet.paths=/bin/oakpal/scan-package", service = Servlet.class)
public class ScanPackageServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanPackageServlet.class);

    @Reference
    private Packaging packagingService;

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {

        List<Resource> pkgResources = new ArrayList<>();
        final String[] pkgPaths = request.getParameterValues("path");
        for (String pkgPath : pkgPaths) {
            final Resource pkgRes = request.getResourceResolver().getResource(pkgPath);
            pkgResources.add(pkgRes);
        }

        OakMachine.Builder builder = new OakMachine.Builder();

        builder.withPackagingService(packagingService);

        ProgressCheck check = new Paths().newInstance(
                key("rules",
                        arr().val(new Rule(DENY, Pattern.compile("/apps(/.*)?"))))
                        .get());

        builder.withProgressChecks(check, new Echo() {
            @Override
            protected void echo(final String message, final Object... formatArgs) {
                LOGGER.info(String.format(message, formatArgs));
            }
        });

        OakMachine machine = builder.build();

        try (ScanTempSpace tempSpace = new ScanTempSpace(pkgResources)) {
            List<CheckReport> reportList = machine.scanPackages(tempSpace.open());

            ReportMapper.writeReportsToWriter(reportList, response.getWriter());
        } catch (final RepositoryException | AbortedScanException e) {
            throw new ServletException("failed to complete scan", e);
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }
}
