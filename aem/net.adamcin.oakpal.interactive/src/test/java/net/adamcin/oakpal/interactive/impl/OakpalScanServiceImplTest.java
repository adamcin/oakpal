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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.interactive.ChecklistTracker;
import net.adamcin.oakpal.interactive.OakpalScanInput;
import net.adamcin.oakpal.interactive.OakpalScanResult;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class OakpalScanServiceImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OakpalScanServiceImplTest.class);

    @Rule
    public final SlingContext context = new SlingContextBuilder().resourceResolverType(ResourceResolverType.JCR_MOCK).build();

    @Mock
    private Packaging packagingService;

    @Mock
    private DynamicClassLoaderManager classLoaderManager;

    @Mock
    private ChecklistTracker checklistTracker;

    @Before
    public void setUp() throws Exception {
        // no osgi classloaders to worry about in mocks.
        when(classLoaderManager.getDynamicClassLoader()).thenReturn(getClass().getClassLoader());
        context.registerService(Packaging.class, packagingService);
        //context.registerService(DynamicClassLoaderManager.class, classLoaderManager);
        final List<Checklist> checklists = ChecklistPlanner.constructChecklists(
                ChecklistPlanner.parseChecklists(OakpalScanServiceImplTest.class.getClassLoader()));
        when(checklistTracker.getBundleChecklists()).thenReturn(checklists);
        context.registerService(ChecklistTracker.class, checklistTracker);
    }

    @Test
    public void testSimpleScan() throws Exception {
        final OakpalScanServiceImpl scanService = context.registerInjectActivateService(new OakpalScanServiceImpl());
        final File pack1 = TestPackageUtil.prepareTestPackage("tmp_foo.zip");
        final File pack2 = TestPackageUtil.prepareTestPackage("tmp_foo_bar.zip");
        final Resource pack1Res = context.create().resource("/packs/pack1",
                "jcr:primaryType", "nt:resource",
                "jcr:data", new FileInputStream(pack1));
        final Resource pack2Res = context.create().resource("/packs/pack2",
                "jcr:primaryType", "nt:resource",
                "jcr:data", new FileInputStream(pack2));
        ResourceResolver resolver = context.resourceResolver();
        OakpalScanInput input = mock(OakpalScanInput.class);

        when(input.getChecklists()).thenReturn(Collections.singletonList("basic"));
        when(input.getPackagePaths()).thenReturn(Arrays.asList(pack1Res.getPath(), pack2Res.getPath()));
        when(input.getPreInstallPackagePaths()).thenReturn(Collections.emptyList());
        when(input.getChecks()).thenReturn(Collections.emptyList());
        when(input.isInstallPlatformNodetypes()).thenReturn(false);

        OakpalScanResult result = scanService.performScan(resolver, input);
        assertTrue("should be a report for basic/overlaps",
                result.getReports().stream().anyMatch(report -> report.getCheckName().endsWith("basic/overlaps")));
        assertTrue("should be a violation for basic/overlaps",
                result.getReports().stream().filter(report -> report.getCheckName().endsWith("basic/overlaps"))
                        .anyMatch(report -> report.getViolations().stream().findAny().isPresent()));
        ReportMapper.reportsToJson(result.getReports()).forEach(report ->
                LOGGER.info("report: {}", report.toString()));
    }


}
