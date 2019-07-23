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

package net.adamcin.oakpal.core;

import static net.adamcin.oakpal.core.JavaxJson.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class ChecklistPlannerTest {
    private static final String MODULE_ALPHA = "alpha";
    private static final String MODULE_BRAVO = "bravo";
    private static final String MODULE_CHARLIE = "charlie";
    private static final String CKL_NAME_ALPHA = "alpha";
    private static final String CKL_ID_ALPHA = MODULE_ALPHA + "/" + CKL_NAME_ALPHA;
    private static final String CKL_NAME_BRAVO_1 = "bravo-1";
    private static final String CKL_ID_BRAVO_1 = MODULE_BRAVO + "/" + CKL_NAME_BRAVO_1;
    private static final String CKL_NAME_BRAVO_2 = "bravo-2";
    private static final String CKL_ID_BRAVO_2 = MODULE_BRAVO + "/" + CKL_NAME_BRAVO_2;
    private static final String CKL_NAME_CHARLIE = "charlie";
    private static final String CKL_ID_CHARLIE = MODULE_CHARLIE + "/" + CKL_NAME_CHARLIE;
    private static final List<String> ALL_CKL_IDS = Arrays.asList(
            CKL_ID_ALPHA, CKL_ID_BRAVO_1, CKL_ID_BRAVO_2, CKL_ID_CHARLIE);

    private final File resourceDir = new File("src/test/resources/ChecklistPlannerTest");
    private final File alphaDir = new File(resourceDir, MODULE_ALPHA);
    private final File bravoDir = new File(resourceDir, MODULE_BRAVO);
    private final File charlieDir = new File(resourceDir, MODULE_CHARLIE);
    private final File zuluDir = new File(resourceDir, "zulu");
    private URL alphaManifest;
    private URL bravoManifest;
    private URL charlieManifest;
    private URL zuluManifest;

    @Before
    public void setUp() throws Exception {
        alphaManifest = new File(alphaDir, JarFile.MANIFEST_NAME).toURI().toURL();
        bravoManifest = new File(bravoDir, JarFile.MANIFEST_NAME).toURI().toURL();
        charlieManifest = new File(charlieDir, JarFile.MANIFEST_NAME).toURI().toURL();
        zuluManifest = new File(zuluDir, JarFile.MANIFEST_NAME).toURI().toURL();
    }

    @Test
    public void testApplyOverrides() {
        final CheckSpec base = new CheckSpec();
        base.setName(CKL_ID_ALPHA + "/" + "foo");
        base.setImpl("base_impl");
        base.setConfig(key("zero", "orez").key("shared", "zero").get());

        final CheckSpec implicitSkipOverride = new CheckSpec();
        implicitSkipOverride.setName("foo");
        implicitSkipOverride.setSkip(true);

        final CheckSpec implicitConfigOverride1 = new CheckSpec();
        implicitConfigOverride1.setName("foo");
        implicitConfigOverride1.setConfig(key("one", "eno").key("shared", "one").get());

        final CheckSpec implicitConfigOverride2 = new CheckSpec();
        implicitConfigOverride2.setName("foo");
        implicitConfigOverride2.setConfig(key("two", "owt").key("shared", "two").get());

        final CheckSpec result = ChecklistPlanner.applyOverrides(Arrays.asList(
                implicitSkipOverride,
                implicitConfigOverride1,
                implicitConfigOverride2
        ), base);

        assertTrue("skipped", result.isSkip());
        assertEquals("shared should be", "two", result.getConfig().getString("shared"));
        assertEquals("zero should be", "orez", result.getConfig().getString("zero"));
        assertEquals("one should be", "eno", result.getConfig().getString("one"));
        assertEquals("two should be", "owt", result.getConfig().getString("two"));
    }

    @Test
    public void testGetEffectiveCheckSpecs() throws Exception {
        ChecklistPlanner planner = new ChecklistPlanner(Arrays.asList(CKL_NAME_ALPHA, CKL_NAME_BRAVO_1));
        planner.provideChecklists(constructIndivChecklists());

        List<CheckSpec> effNoOverrides = planner.getEffectiveCheckSpecs(null);
        assertEquals("effective checks size should be", 2, effNoOverrides.size());
        assertEquals("first should be alpha", CKL_NAME_ALPHA + "/pathy", effNoOverrides.get(0).getName());
        assertEquals("second should be bravo/bravo-1", CKL_ID_BRAVO_1 + "/pathy", effNoOverrides.get(1).getName());

        final CheckSpec pathyOverride = new CheckSpec();
        pathyOverride.setName("pathy");
        pathyOverride.setSkip(true);
        List<CheckSpec> effOneOverride = planner.getEffectiveCheckSpecs(Collections.singletonList(pathyOverride));
        assertEquals("effective checks size should be", 0, effOneOverride.size());

        final CheckSpec alphaPathyOverride = new CheckSpec();
        alphaPathyOverride.setName("alpha/pathy");
        alphaPathyOverride.setSkip(true);

        List<CheckSpec> effOneAlphaOverride = planner.getEffectiveCheckSpecs(Collections.singletonList(alphaPathyOverride));
        assertEquals("effective checks size should be", 1, effOneAlphaOverride.size());

        final CheckSpec alphaPathy2Override = new CheckSpec();
        alphaPathy2Override.setName("pathy2");
        alphaPathy2Override.setTemplate("alpha/pathy");
        List<CheckSpec> effOneAlphaInherit = planner.getEffectiveCheckSpecs(Arrays.asList(alphaPathyOverride, alphaPathy2Override));
        assertEquals("effective checks size should be", 2, effOneAlphaInherit.size());

        final CheckSpec implSpec = new CheckSpec();
        implSpec.setImpl("foo");
        List<CheckSpec> effWithImplSpec = planner.getEffectiveCheckSpecs(Arrays.asList(alphaPathyOverride, alphaPathy2Override, implSpec));
        assertEquals("effective checks size should be", 3, effWithImplSpec.size());

    }

    final Predicate<Checklist> alphaFilter = checklist ->
            MODULE_ALPHA.equals(checklist.getModuleName()) && CKL_NAME_ALPHA.equals(checklist.getName());
    final Predicate<Checklist> bravo1Filter = checklist ->
            MODULE_BRAVO.equals(checklist.getModuleName()) && CKL_NAME_BRAVO_1.equals(checklist.getName());
    final Predicate<Checklist> bravo2Filter = checklist ->
            MODULE_BRAVO.equals(checklist.getModuleName()) && CKL_NAME_BRAVO_2.equals(checklist.getName());
    final Predicate<Checklist> charlieFilter = checklist ->
            MODULE_CHARLIE.equals(checklist.getModuleName()) && CKL_NAME_CHARLIE.equals(checklist.getName());

    @Test
    public void testDiscoverChecklistsNoArg() throws Exception {
        ChecklistPlanner planner = new ChecklistPlanner(ALL_CKL_IDS);
        final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getTestClassLoader());
            planner.discoverChecklists();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }

        List<Checklist> allChecklists = planner.getAllChecklists().collect(Collectors.toList());
        assertTrue("constructed has alpha/alpha checklist", allChecklists.stream().anyMatch(alphaFilter));
        assertTrue("constructed has bravo/bravo-1 checklist", allChecklists.stream().anyMatch(bravo1Filter));
        assertTrue("constructed has bravo/bravo-2 checklist", allChecklists.stream().anyMatch(bravo2Filter));
        assertTrue("constructed has charlie/charlie checklist", allChecklists.stream().anyMatch(charlieFilter));
    }

    @Test
    public void testDiscoverChecklistsNoArgThrows() throws Exception {
        ChecklistPlanner planner = new ChecklistPlanner(ALL_CKL_IDS);
        final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = mock(ClassLoader.class);
            doThrow(IOException.class).when(classLoader).getResources(anyString());
            Thread.currentThread().setContextClassLoader(classLoader);
            planner.discoverChecklists();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }

        List<Checklist> allChecklists = planner.getAllChecklists().collect(Collectors.toList());
        assertTrue("checklists should be empty", allChecklists.isEmpty());
    }

    @Test
    public void testDiscoverChecklistsFileList() {
        ChecklistPlanner planner = new ChecklistPlanner(ALL_CKL_IDS);
        planner.discoverChecklists(Arrays.asList(alphaDir, bravoDir, charlieDir));
        List<Checklist> allChecklists = planner.getAllChecklists().collect(Collectors.toList());
        assertTrue("constructed has alpha/alpha checklist", allChecklists.stream().anyMatch(alphaFilter));
        assertTrue("constructed has bravo/bravo-1 checklist", allChecklists.stream().anyMatch(bravo1Filter));
        assertTrue("constructed has bravo/bravo-2 checklist", allChecklists.stream().anyMatch(bravo2Filter));
        assertTrue("constructed has charlie/charlie checklist", allChecklists.stream().anyMatch(charlieFilter));
    }

    @Test
    public void testDiscoverChecklistsFileListThrows() {
        ChecklistPlanner planner = new ChecklistPlanner(ALL_CKL_IDS);
        planner.discoverChecklists(Arrays.asList(alphaDir, bravoDir, charlieDir, zuluDir));
        List<Checklist> allChecklists = planner.getAllChecklists().collect(Collectors.toList());
        assertTrue("checklists should be empty", allChecklists.isEmpty());
    }

    @Test
    public void testProvideChecklists() throws Exception {
        List<Checklist> checklists = this.constructIndivChecklists();
        ChecklistPlanner planner = new ChecklistPlanner(ALL_CKL_IDS);
        planner.provideChecklists(checklists);
        List<Checklist> allChecklists = planner.getAllChecklists().collect(Collectors.toList());
        assertTrue("constructed has alpha/alpha checklist", allChecklists.stream().anyMatch(alphaFilter));
        assertTrue("constructed has bravo/bravo-1 checklist", allChecklists.stream().anyMatch(bravo1Filter));
        assertTrue("constructed has bravo/bravo-2 checklist", allChecklists.stream().anyMatch(bravo2Filter));
        assertTrue("constructed has charlie/charlie checklist", allChecklists.stream().anyMatch(charlieFilter));
    }

    @Test
    public void testSelectChecklists() throws Exception {
        List<Checklist> checklists = this.constructIndivChecklists();
        ChecklistPlanner planner = new ChecklistPlanner(Arrays.asList(CKL_NAME_ALPHA, ""));
        planner.provideChecklists(checklists);
        List<Checklist> allChecklists = planner.getAllChecklists().collect(Collectors.toList());
        assertTrue("all has alpha/alpha checklist", allChecklists.stream().anyMatch(alphaFilter));
        assertTrue("all has bravo/bravo-1 checklist", allChecklists.stream().anyMatch(bravo1Filter));
        assertTrue("all has bravo/bravo-2 checklist", allChecklists.stream().anyMatch(bravo2Filter));
        assertTrue("all has charlie/charlie checklist", allChecklists.stream().anyMatch(charlieFilter));

        List<Checklist> selectedChecklists = planner.getSelectedChecklists().collect(Collectors.toList());
        assertTrue("selected alpha/alpha checklist", selectedChecklists.stream().anyMatch(alphaFilter));
        assertFalse("not selected bravo/bravo-1 checklist", selectedChecklists.stream().anyMatch(bravo1Filter));
        assertFalse("not selected bravo/bravo-2 checklist", selectedChecklists.stream().anyMatch(bravo2Filter));
        assertFalse("not selected charlie/charlie checklist", selectedChecklists.stream().anyMatch(charlieFilter));
    }

    @Test
    public void testConstructChecklists() throws Exception {
        List<Checklist> constructed = constructIndivChecklists();
        assertEquals("constructed size is", 4, constructed.size());
        assertTrue("constructed has alpha/alpha checklist", constructed.stream().anyMatch(alphaFilter));
        assertTrue("constructed has bravo/bravo-1 checklist", constructed.stream().anyMatch(bravo1Filter));
        assertTrue("constructed has bravo/bravo-2 checklist", constructed.stream().anyMatch(bravo2Filter));
        assertTrue("constructed has charlie/charlie checklist", constructed.stream().anyMatch(charlieFilter));
    }

    @Test
    public void testGetInitStages() throws Exception {
        List<Checklist> checklists = this.constructIndivChecklists();
        ChecklistPlanner planner = new ChecklistPlanner(ALL_CKL_IDS);
        planner.provideChecklists(checklists);
        List<InitStage> initStages = planner.getInitStages();
        assertEquals("init stages count should be ", 4, initStages.size());
        new OakMachine.Builder().withInitStages(initStages).build().initAndInspect(session -> {
            assertTrue("/ChecklistPlannerTest/alpha node should exist",
                    session.nodeExists("/ChecklistPlannerTest/alpha"));
            assertTrue("/ChecklistPlannerTest/bravo-1 node should exist",
                    session.nodeExists("/ChecklistPlannerTest/bravo-1"));
            assertTrue("/ChecklistPlannerTest/bravo-2 node should exist",
                    session.nodeExists("/ChecklistPlannerTest/bravo-2"));
            assertTrue("/ChecklistPlannerTest/charlie node should exist",
                    session.nodeExists("/ChecklistPlannerTest/charlie"));
        });
        ChecklistPlanner selectivePlanner = new ChecklistPlanner(Collections.singletonList(CKL_NAME_BRAVO_2));
        selectivePlanner.provideChecklists(checklists);
        List<InitStage> selectiveStages = selectivePlanner.getInitStages();
        assertEquals("selective init stages count should be ", 1, selectiveStages.size());
        new OakMachine.Builder().withInitStages(selectiveStages).build().initAndInspect(session -> {
            assertFalse("/ChecklistPlannerTest/alpha node should not exist",
                    session.nodeExists("/ChecklistPlannerTest/alpha"));
            assertFalse("/ChecklistPlannerTest/bravo-1 node should not exist",
                    session.nodeExists("/ChecklistPlannerTest/bravo-1"));
            assertTrue("/ChecklistPlannerTest/bravo-2 node should exist",
                    session.nodeExists("/ChecklistPlannerTest/bravo-2"));
            assertFalse("/ChecklistPlannerTest/charlie node should not exist",
                    session.nodeExists("/ChecklistPlannerTest/charlie"));
        });
    }

    Map<URL, List<JsonObject>> parseIndivChecklists() throws Exception {
        Map<URL, List<JsonObject>> alphaParsed = ChecklistPlanner.parseChecklists(alphaManifest);
        Map<URL, List<JsonObject>> bravoParsed = ChecklistPlanner.parseChecklists(bravoManifest);
        Map<URL, List<JsonObject>> charlieParsed = ChecklistPlanner.parseChecklists(charlieManifest);
        Map<URL, List<JsonObject>> parsed = new LinkedHashMap<>();
        parsed.putAll(alphaParsed);
        parsed.putAll(bravoParsed);
        parsed.putAll(charlieParsed);
        return parsed;
    }

    List<Checklist> constructIndivChecklists() throws Exception {
        Map<URL, List<JsonObject>> parsed = parseIndivChecklists();
        return ChecklistPlanner.constructChecklists(parsed);
    }

    ClassLoader getTestClassLoader() throws Exception {
        return new URLClassLoader(new URL[]{
                alphaDir.toURI().toURL(),
                bravoDir.toURI().toURL(),
                charlieDir.toURI().toURL()},
                null);
    }

    @Test
    public void testParseChecklists() throws Exception {
        List<Fun.ThrowingSupplier<Map<URL, List<JsonObject>>>> parsers = Arrays.asList(
                () -> ChecklistPlanner.parseChecklists(Arrays.asList(alphaDir, bravoDir, charlieDir)),
                () -> ChecklistPlanner.parseChecklists(getTestClassLoader()),
                this::parseIndivChecklists
        );
        for (Fun.ThrowingSupplier<Map<URL, List<JsonObject>>> parser : parsers) {
            Map<URL, List<JsonObject>> parsed = parser.tryGet();
            assertTrue("parsed contains alpha module", parsed.containsKey(alphaManifest));
            assertEquals("alpha checklist size is", 1, parsed.get(alphaManifest).size());
            assertEquals("alpha checklist 0 name is", "alpha", parsed.get(alphaManifest).get(0).getString("name"));

            assertTrue("parsed contains bravo module", parsed.containsKey(bravoManifest));
            assertEquals("bravo checklist size is", 2, parsed.get(bravoManifest).size());
            assertEquals("bravo checklist 0 name is", "bravo-1", parsed.get(bravoManifest).get(0).getString("name"));
            assertEquals("bravo checklist 1 name is", "bravo-2", parsed.get(bravoManifest).get(1).getString("name"));

            assertTrue("parsed contains charlie module", parsed.containsKey(charlieManifest));
            assertEquals("charlie checklist size is", 1, parsed.get(charlieManifest).size());
            assertFalse("charlie checklist 0 name is missing (defaults to module name)",
                    parsed.get(charlieManifest).get(0).containsKey("name"));
        }
    }

    @Test
    public void testBestModuleName() throws Exception {
        final File manifestWithOakpalModuleName = new File("src/test/resources/manifestWithOakpalModuleName.mf");
        final File manifestWithBundleSymbolicName = new File("src/test/resources/manifestWithBundleSymbolicName.mf");
        final File manifestWithAutomaticModuleName = new File("src/test/resources/manifestWithAutomaticModuleName.mf");
        final File manifestWithNoModuleName = new File("src/test/resources/manifestWithNoModuleName.mf");

        assertEquals(ChecklistPlanner.OAKPAL_MODULENAME + " takes precedence",
                "oakpal.moduleName",
                ChecklistPlanner.bestModuleName(manifestWithOakpalModuleName.toURI().toURL()));
        assertEquals(ChecklistPlanner.BUNDLE_SYMBOLICNAME + " takes precedence over AMN",
                "bundle.symbolicName",
                ChecklistPlanner.bestModuleName(manifestWithBundleSymbolicName.toURI().toURL()));
        assertEquals(ChecklistPlanner.AUTOMATIC_MODULE_NAME + " is last resort",
                "auto.module.name",
                ChecklistPlanner.bestModuleName(manifestWithAutomaticModuleName.toURI().toURL()));
        assertEquals("empty string if no header is present",
                "",
                ChecklistPlanner.bestModuleName(manifestWithNoModuleName.toURI().toURL()));
    }

}