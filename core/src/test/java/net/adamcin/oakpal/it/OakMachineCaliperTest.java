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

package net.adamcin.oakpal.it;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.OsgiConfigInstallable;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.checks.SlingJcrInstaller;
import net.adamcin.oakpal.core.sling.DefaultSlingSimulator;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Run end-to-end caliper tests using pre-shaded jar.
 */
public class OakMachineCaliperTest {

    private File grandTourPackage = TestPackageUtil.getCaliperPackage();

    private Map<String, List<String>> dependsOn = new HashMap<>();

    private static final String NAME_ALL = "oakpal-caliper.all";
    private static final String NAME_APPS = "oakpal-caliper.ui.apps";
    private static final String NAME_APPS_AUTHOR = "oakpal-caliper.ui.apps.author";
    private static final String NAME_APPS_PUBLISH = "oakpal-caliper.ui.apps.publish";
    private static final String NAME_CONTENT = "oakpal-caliper.ui.content";
    private static final String NAME_CONTENT_SUBC1 = "oakpal-caliper.ui.content.subc1";
    private static final String NAME_CONTENT_SUBA2 = "oakpal-caliper.ui.content.suba2";
    private static final String NAME_CONTENT_SUBB3 = "oakpal-caliper.ui.content.subb3";

    private Set<String> allNames = new HashSet<>(Arrays.asList(
            NAME_ALL,
            NAME_APPS,
            NAME_APPS_AUTHOR,
            NAME_APPS_PUBLISH,
            NAME_CONTENT,
            NAME_CONTENT_SUBC1,
            NAME_CONTENT_SUBA2,
            NAME_CONTENT_SUBB3
    ));

    private Set<String> installOrderFull = new HashSet<>(Arrays.asList(
            NAME_ALL,
            NAME_APPS,
            NAME_APPS_AUTHOR,
            NAME_APPS_PUBLISH,
            NAME_CONTENT,
            NAME_CONTENT_SUBC1,
            // not actually installed: "oakpal-caliper.ui.content.suba2",
            NAME_CONTENT_SUBB3
    ));

    private Set<String> installOrderNoRunModesNoSubs = new HashSet<>(Arrays.asList(
            NAME_ALL,
            NAME_APPS,
            NAME_CONTENT
    ));

    @Before
    public void setUp() throws Exception {
        // populate with direct dependencies as notated in caliper poms
        dependsOn.put(NAME_APPS, Collections.singletonList(NAME_ALL));
        dependsOn.put(NAME_APPS_AUTHOR, Collections.singletonList(NAME_APPS));
        dependsOn.put(NAME_APPS_PUBLISH, Collections.singletonList(NAME_APPS));
        dependsOn.put(NAME_CONTENT, Arrays.asList(NAME_ALL, NAME_APPS));
        dependsOn.put(NAME_CONTENT_SUBC1, Collections.singletonList(NAME_CONTENT));
        dependsOn.put(NAME_CONTENT_SUBB3, Arrays.asList(NAME_CONTENT, NAME_CONTENT_SUBC1));

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

        assertAllDependenciesInList("testScanNoRunModes_captureIdentifiedPackages",
                installOrderNoRunModesNoSubs,
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

        assertAllDependenciesInList("testScanAllRunModesWithSubs_captureIdentifiedPackages",
                installOrderFull,
                identifiedPackageIds.stream().map(PackageId::getName).collect(Collectors.toList()));
    }

    public Collection<String> getDependencies(final String packageName) {
        return dependsOn.getOrDefault(packageName, Collections.emptyList());
    }

    public void assertAllDependenciesInList(final String message,
                                            final Set<String> expected,
                                            final List<String> actual) {
        for (String expectedName : expected) {
            Collection<String> dependencies = getDependencies(expectedName).stream()
                    .filter(expected::contains)
                    .collect(Collectors.toSet());
            for (String dependency : dependencies) {
                assertStringBeforeAfter(message, actual, dependency, expectedName);
            }
        }
        final Set<String> expectMissing = new HashSet<>(allNames);
        expectMissing.removeAll(expected);
        for (String unexpectedName : expectMissing) {
            assertFalse(String.format(message + ": expect %s not contains %s", actual, unexpectedName),
                    actual.contains(unexpectedName));
        }
    }

    public void assertStringBeforeAfter(final String message, final List<String> elements, final String before, final String after) {
        final int indexBefore = elements.indexOf(before);
        assertTrue(String.format(message + ": expect %s contains before %s", elements, before), indexBefore >= 0);
        final int indexAfter = elements.indexOf(after);
        assertTrue(String.format(message + ": expect %s contains after %s", elements, after), indexAfter >= 0);
        assertTrue(String.format(message + ": expect in %s, before %s prior to after %s", elements, before, after),
                indexBefore < indexAfter);
    }

    @Test
    public void testCfgJsonIsParsed() throws Exception {
        final CompletableFuture<OsgiConfigInstallable> installableLatch = new CompletableFuture<>();

        final ProgressCheck check = new ProgressCheck() {
            @Override
            public void beforeSlingInstall(final PackageId scanPackageId,
                                           final SlingInstallable slingInstallable,
                                           final Session inspectSession) throws RepositoryException {
                if (slingInstallable instanceof OsgiConfigInstallable) {
                    OsgiConfigInstallable installable = (OsgiConfigInstallable) slingInstallable;
                    if ("net.adamcin.oakpal.example.NotAJcrResourceResolverFactoryImpl".equals(
                            installable.getServicePid())) {
                        installableLatch.complete(installable);
                    }
                }
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

        OsgiConfigInstallable installable = installableLatch.getNow(null);
        assertNotNull("expect osgi config installable is captured", installable);

        final Map<String, Object> expectProps = new HashMap<>();
        expectProps.put("resource.resolver.searchpath", new String[]{"/apps", "/libs"});
        expectProps.put("resource.resolver.map.location", "/etc/map");
        expectProps.put("resource.resolver.providerhandling.paranoid", false);
        expectProps.put("resource.resolver.enable.vanitypath", true);
        expectProps.put("resource.resolver.vanitypath.maxEntries", -1L);
        expectProps.put("resource.resolver.log.closing", false);
        expectProps.put("resource.resolver.vanitypath.maxEntries.startup", true);
        expectProps.put("resource.resolver.vanity.precedence", false);
        expectProps.put("resource.resolver.vanitypath.blacklist", new String[]{"/content/usergenerated"});
        expectProps.put("resource.resolver.vanitypath.whitelist", new String[]{"/apps/", "/libs/", "/content/"});
        expectProps.put("resource.resolver.manglenamespaces", true);
        expectProps.put("resource.resolver.default.vanity.redirect.status", 302L);
        expectProps.put("resource.resolver.optimize.alias.resolution", true);
        expectProps.put("installation.hint", "config");
        expectProps.put("resource.resolver.allowDirect", true);
        expectProps.put("resource.resolver.required.providers",
                new String[]{"org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory"});
        expectProps.put("resource.resolver.virtual", new String[]{"/:/"});
        expectProps.put("resource.resolver.mapping", new String[]{
                "/etc/designs/www/images/>/images/",
                "/etc/designs/www/fonts/>/fonts/",
                "/-/"
        });

        checkExpectedProperties(expectProps, installable.getProperties());
    }

    void checkExpectedProperties(final Map<String, Object> expectProps, final Map<String, Object> props) {
        assertEquals("expect same keys", expectProps.keySet(), props.keySet());
        for (Map.Entry<String, Object> entry : expectProps.entrySet()) {
            Object expectValue = entry.getValue();
            if (expectValue.getClass().isArray()) {
                assertArrayEquals("expect equal array for key " + entry.getKey(), (Object[]) expectValue,
                        (Object[]) props.get(entry.getKey()));
            } else {
                assertEquals("expect equal value for key " + entry.getKey(), expectValue,
                        props.get(entry.getKey()));
            }
        }
    }
}
