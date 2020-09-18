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

package net.adamcin.oakpal.core.checks;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PackageGraphTest {
    private final PackageId container1 = PackageId.fromString("container1");
    private final PackageId cont1sub1 = PackageId.fromString("cont1sub1");
    private final PackageId cont1emb1 = PackageId.fromString("cont1emb1");
    private final EmbeddedPackageInstallable installable1 = new EmbeddedPackageInstallable(container1, "", cont1emb1);
    private final PackageId container2 = PackageId.fromString("container2");
    private final PackageId cont2sub1 = PackageId.fromString("cont2sub1");
    private final PackageId cont2emb1 = PackageId.fromString("cont2emb1");
    private final EmbeddedPackageInstallable installable2 = new EmbeddedPackageInstallable(container2, "", cont2emb1);

    private final PackageGraph graph = new PackageGraph();

    private final Set<PackageId> scan1 = Stream.of(container1, cont1sub1, cont1emb1).collect(Collectors.toSet());
    private final Set<PackageId> scan2 = Stream.of(container2, cont2sub1, cont2emb1).collect(Collectors.toSet());

    @Before
    public void setUp() throws Exception {
        graph.startedScan();
        graph.identifyPackage(container1, null);
        graph.identifySubpackage(cont1sub1, container1);
        graph.identifyEmbeddedPackage(cont1emb1, container1, installable1);
        graph.identifyPackage(container2, null);
        graph.identifySubpackage(cont2sub1, container2);
        graph.identifyEmbeddedPackage(cont2emb1, container2, installable2);
    }

    @Test
    public void testIsIdentified() {
        for (PackageId id : scan1) {
            assertTrue("expect identified " + id, graph.isIdentified(id));
        }
        for (PackageId id : scan2) {
            assertTrue("expect identified " + id, graph.isIdentified(id));
        }
        graph.startedScan();
        for (PackageId id : scan1) {
            assertFalse("expect not identified " + id, graph.isIdentified(id));
        }
        for (PackageId id : scan2) {
            assertFalse("expect not identified " + id, graph.isIdentified(id));
        }
    }

    @Test
    public void testGetReportedViolations() {
        graph.finishedScan();
        assertEquals("expect no violations", Collections.emptyList(),
                graph.getReportedViolations());
        graph.startedScan();
        assertEquals("expect no violations after startedScan", Collections.emptyList(),
                graph.getReportedViolations());
    }

    @Test
    public void testLastIdentified() {
        assertEquals("expect cont2emb1 is last identified",
                cont2emb1, graph.getLastIdentified());
        graph.startedScan();
        assertNull("expect last identified is null", graph.getLastIdentified());

    }

    @Test
    public void testIdentifyPackage() {
        graph.startedScan();
        assertNull("expect last identified is null", graph.getLastIdentified());
        assertFalse("expect not identified " + container2, graph.isIdentified(container2));
        graph.identifyPackage(container2, null);
        assertTrue("expect identified " + container2, graph.isIdentified(container2));
        assertEquals("expect container2 is last identified",
                container2, graph.getLastIdentified());
        assertFalse("expect not identified " + container1, graph.isIdentified(container1));
        graph.identifyPackage(container1, null);
        assertTrue("expect identified " + container1, graph.isIdentified(container1));
        assertEquals("expect container1 is last identified",
                container1, graph.getLastIdentified());
    }

    @Test
    public void testIdentifySubpackage() {
        graph.startedScan();
        assertNull("expect last identified is null", graph.getLastIdentified());
        assertFalse("expect not identified " + cont2sub1, graph.isIdentified(cont2sub1));
        graph.identifySubpackage(cont2sub1, container2);
        assertTrue("expect identified " + cont2sub1, graph.isIdentified(cont2sub1));
        assertEquals("expect cont2sub1 is last identified",
                cont2sub1, graph.getLastIdentified());
        assertFalse("expect not identified " + cont1sub1, graph.isIdentified(cont1sub1));
        graph.identifySubpackage(cont1sub1, container1);
        assertTrue("expect identified " + cont1sub1, graph.isIdentified(cont1sub1));
        assertEquals("expect cont1sub1 is last identified",
                cont1sub1, graph.getLastIdentified());
    }

    @Test
    public void testIdentifyEmbeddedPackage() {
        graph.startedScan();
        assertNull("expect last identified is null", graph.getLastIdentified());
        assertFalse("expect not identified " + cont2emb1, graph.isIdentified(cont2emb1));
        graph.identifyEmbeddedPackage(cont2emb1, null,
                new EmbeddedPackageInstallable(container2, "", cont2emb1));
        assertTrue("expect identified " + cont2emb1, graph.isIdentified(cont2emb1));
        assertEquals("expect cont2emb1 is last identified",
                cont2emb1, graph.getLastIdentified());
        assertFalse("expect not identified " + cont1emb1, graph.isIdentified(cont1emb1));
        graph.identifyEmbeddedPackage(cont1emb1, container1, null);
        assertTrue("expect identified " + cont1emb1, graph.isIdentified(cont1emb1));
        assertEquals("expect cont1emb1 is last identified",
                cont1emb1, graph.getLastIdentified());
    }

    @Test
    public void testIsRoot() {
        assertTrue("expect isRoot " + container1, graph.isRoot(container1));
        assertFalse("expect not isRoot " + cont1sub1, graph.isRoot(cont1sub1));
        assertFalse("expect not isRoot " + cont1emb1, graph.isRoot(cont1emb1));
        assertTrue("expect isRoot " + container2, graph.isRoot(container2));
        assertFalse("expect not isRoot " + cont2sub1, graph.isRoot(cont2sub1));
        assertFalse("expect not isRoot " + cont2emb1, graph.isRoot(cont2emb1));
        graph.startedScan();
        assertTrue("expect isRoot " + container1, graph.isRoot(container1));
        assertTrue("expect isRoot " + cont1sub1, graph.isRoot(cont1sub1));
        assertTrue("expect isRoot " + cont1emb1, graph.isRoot(cont1emb1));
        assertTrue("expect isRoot " + container2, graph.isRoot(container2));
        assertTrue("expect isRoot " + cont2sub1, graph.isRoot(cont2sub1));
        assertTrue("expect isRoot " + cont2emb1, graph.isRoot(cont2emb1));
    }

    @Test
    public void testIsLeftDescendantOfRight() {
        assertTrue("expect yes: cont1 -> cont1", graph.isLeftDescendantOfRight(container1, container1));
        assertTrue("expect yes: cont2 -> cont2", graph.isLeftDescendantOfRight(container2, container2));
        assertFalse("expect not: cont1 -> cont2", graph.isLeftDescendantOfRight(container1, container2));
        assertFalse("expect not: cont2 -> cont1", graph.isLeftDescendantOfRight(container2, container1));
        assertTrue("expect yes: cont1sub1 -> cont1", graph.isLeftDescendantOfRight(cont1sub1, container1));
        assertFalse("expect not: cont1 -> cont1sub1 ", graph.isLeftDescendantOfRight(container1, cont1sub1));
        assertTrue("expect yes: cont1emb1 -> cont1", graph.isLeftDescendantOfRight(cont1emb1, container1));
        assertFalse("expect not: cont1 -> cont1emb1 ", graph.isLeftDescendantOfRight(container1, cont1emb1));
        assertFalse("expect not: cont1sub1 -> cont1emb1 ", graph.isLeftDescendantOfRight(cont1sub1, cont1emb1));
        assertFalse("expect not: cont1emb1 -> cont1sub1 ", graph.isLeftDescendantOfRight(cont1emb1, cont1sub1));
        assertTrue("expect yes: cont2sub1 -> cont2", graph.isLeftDescendantOfRight(cont2sub1, container2));
        assertFalse("expect not: cont2 -> cont2sub1 ", graph.isLeftDescendantOfRight(container2, cont2sub1));
        assertTrue("expect yes: cont2emb1 -> cont2", graph.isLeftDescendantOfRight(cont2emb1, container2));
        assertFalse("expect not: cont2 -> cont2emb1 ", graph.isLeftDescendantOfRight(container2, cont2emb1));
        assertFalse("expect not: cont2sub1 -> cont2emb1 ", graph.isLeftDescendantOfRight(cont2sub1, cont2emb1));
        assertFalse("expect not: cont2emb1 -> cont2sub1 ", graph.isLeftDescendantOfRight(cont2emb1, cont2sub1));

        // mix things up a bit

        graph.identifyPackage(cont1sub1, null);
        assertFalse("expect not after new event: cont1sub1 -> cont1", graph.isLeftDescendantOfRight(cont1sub1, container1));
        graph.identifySubpackage(container2, cont1emb1);
        assertTrue("expect yes after new event: cont2 -> cont1emb1", graph.isLeftDescendantOfRight(container2, cont1emb1));
        assertTrue("expect yes after new event: cont2 -> cont1", graph.isLeftDescendantOfRight(container2, container1));
        graph.identifyEmbeddedPackage(container1, cont2sub1, null);
        assertTrue("expect yes after cycle: cont2 -> cont1emb1", graph.isLeftDescendantOfRight(container2, cont1emb1));
        assertFalse("expect not after cycle: cont2 -> cont1", graph.isLeftDescendantOfRight(container2, container1));
        assertFalse("expect not after cycle: cont1emb1 -> cont1", graph.isLeftDescendantOfRight(cont1emb1, container1));
        assertTrue("expect cont1emb1 is root after cycle", graph.isRoot(cont1emb1));
    }

    @Test
    public void testGetSelfAndAncestors() {
        assertEquals("expect for self " + container1,
                Collections.singletonList(container1), graph.getSelfAndAncestors(container1));
        assertEquals("expect for self " + container2,
                Collections.singletonList(container2), graph.getSelfAndAncestors(container2));

        assertEquals("expect for self " + cont1sub1, Arrays.asList(cont1sub1, container1),
                graph.getSelfAndAncestors(cont1sub1));
        assertEquals("expect for self " + cont2sub1, Arrays.asList(cont2sub1, container2),
                graph.getSelfAndAncestors(cont2sub1));
        assertEquals("expect for self " + cont1emb1, Arrays.asList(cont1emb1, container1),
                graph.getSelfAndAncestors(cont1emb1));
        assertEquals("expect for self " + cont2emb1, Arrays.asList(cont2emb1, container2),
                graph.getSelfAndAncestors(cont2emb1));
    }

    @Test
    public void testGetSelfAndDescendants() {
        assertEquals("expect for self " + container1,
                Arrays.asList(container1, cont1sub1, cont1emb1), graph.getSelfAndDescendants(container1));
        assertEquals("expect for self " + container2,
                Arrays.asList(container2, cont2sub1, cont2emb1), graph.getSelfAndDescendants(container2));

        assertEquals("expect for self " + cont1sub1, Collections.singletonList(cont1sub1),
                graph.getSelfAndDescendants(cont1sub1));
        assertEquals("expect for self " + cont2sub1, Collections.singletonList(cont2sub1),
                graph.getSelfAndDescendants(cont2sub1));
        assertEquals("expect for self " + cont1emb1, Collections.singletonList(cont1emb1),
                graph.getSelfAndDescendants(cont1emb1));
        assertEquals("expect for self " + cont2emb1, Collections.singletonList(cont2emb1),
                graph.getSelfAndDescendants(cont2emb1));
    }

}