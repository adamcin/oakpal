/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.checks.SlingJcrInstaller;
import net.adamcin.oakpal.core.sling.DefaultSlingSimulator;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OakMachineCaliperTest {

    private File grandTourPackage = TestPackageUtil.getCaliperPackage();

    private List<String> installOrderFull = Arrays.asList(
            "oakpal-caliper.all",
            "oakpal-caliper.ui.apps",
            "oakpal-caliper.ui.apps.author",
            "oakpal-caliper.ui.apps.publish",
            "oakpal-caliper.ui.content",
            "oakpal-caliper.ui.content.subc1",
            // not actually installed: "oakpal-caliper.ui.content.suba2",
            "oakpal-caliper.ui.content.subb3"
    );

    private List<String> installOrderNoRunModesNoSubs = Arrays.asList(
            "oakpal-caliper.all",
            "oakpal-caliper.ui.apps",
            "oakpal-caliper.ui.content"
    );

    @Before
    public void setUp() throws Exception {
        assertTrue("expect grand tour package has been copied to " + grandTourPackage.getAbsolutePath(),
                grandTourPackage.exists());
    }

    @Test
    public void testScanNoRunModes() throws Exception {
        OakpalPlan.fromJson(obj().get()).toOakMachineBuilder(null, getClass().getClassLoader()).build()
                .scanPackage(grandTourPackage);
    }

    @Test
    public void testScanNoRunModes_captureIdentifiedPackages() throws Exception {
        final List<PackageId> identifiedPackageIds = new ArrayList<>();
        final ProgressCheck check = new ProgressCheck() {
            @Override
            public void identifyPackage(final PackageId packageId, final File file) {
                identifiedPackageIds.add(packageId);
            }

            @Override
            public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
                identifiedPackageIds.add(packageId);
            }

            @Override
            public void identifyEmbeddedPackage(final PackageId packageId, final PackageId parentId,
                                                final EmbeddedPackageInstallable slingInstallable) {
                identifiedPackageIds.add(packageId);
            }

            @Override
            public Collection<Violation> getReportedViolations() {
                return Collections.emptyList();
            }
        };
        OakpalPlan.fromJson(obj().get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .withProgressCheck(check, new SlingJcrInstaller().newInstance(obj().get()))
                .withSlingSimulator(DefaultSlingSimulator.instance())
                .withSubpackageSilencer((sub, parent) -> true)
                .build()
                .scanPackage(grandTourPackage);

        assertEquals("expect identified packages in order", installOrderNoRunModesNoSubs,
                identifiedPackageIds.stream().map(PackageId::getName).collect(Collectors.toList()));
    }

    @Test
    public void testScanAllRunModesWithSubs_captureIdentifiedPackages() throws Exception {
        final List<PackageId> identifiedPackageIds = new ArrayList<>();
        final ProgressCheck check = new ProgressCheck() {
            @Override
            public void identifyPackage(final PackageId packageId, final File file) {
                identifiedPackageIds.add(packageId);
            }

            @Override
            public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
                identifiedPackageIds.add(packageId);
            }

            @Override
            public void identifyEmbeddedPackage(final PackageId packageId, final PackageId parentId,
                                                final EmbeddedPackageInstallable slingInstallable) {
                identifiedPackageIds.add(packageId);
            }

            @Override
            public Collection<Violation> getReportedViolations() {
                return Collections.emptyList();
            }
        };
        OakpalPlan.fromJson(obj().get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .withProgressCheck(check, new SlingJcrInstaller().newInstance(obj().get()))
                .withSlingSimulator(DefaultSlingSimulator.instance())
                .withRunModes(Stream.of("author", "publish").collect(Collectors.toSet()))
                .build()
                .scanPackage(grandTourPackage);

        assertEquals("expect identified packages in set", installOrderFull,
                identifiedPackageIds.stream().map(PackageId::getName).collect(Collectors.toList()));
    }
}
