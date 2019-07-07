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

package net.adamcin.oakpal.core.checks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.RepositoryException;

import org.junit.Test;

public class EchoTest {


    @Test
    public void testGetReportedViolations() {
        Echo echo = new Echo();
        assertNotNull("reported violations should not be null", echo.getReportedViolations());
        assertTrue("reported violations should be empty", echo.getReportedViolations().isEmpty());
    }

    @Test
    public void testGetCheckName() {
        assertEquals("echo's check name should be 'echo'", "echo", new Echo().getCheckName());
    }

    @Test
    public void testStartedScan() {
        new Echo().startedScan();
    }

    @Test
    public void testReadManifest() {
        new Echo().readManifest(null, null);
    }

    @Test
    public void testIdentifyPackage() {
        new Echo().identifyPackage(null, null);
    }

    @Test
    public void testIdentifySubpackage() {
        new Echo().identifySubpackage(null, null);
    }

    @Test
    public void testBeforeExtract() throws RepositoryException {
        new Echo().beforeExtract(null, null, null, null, null);
    }

    @Test
    public void testImportedPath() throws RepositoryException {
        new Echo().importedPath(null, null, null);
    }

    @Test
    public void testDeletedPath() throws RepositoryException {
        new Echo().deletedPath(null, null, null);
    }

    @Test
    public void testAfterExtract() throws RepositoryException {
        new Echo().afterExtract(null, null);
    }

    @Test
    public void testFinishedScan() {
        new Echo().finishedScan();
    }
}
