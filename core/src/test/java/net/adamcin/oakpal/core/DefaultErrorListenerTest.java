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

import static org.junit.Assert.assertEquals;

import net.adamcin.oakpal.api.SimpleViolation;
import net.adamcin.oakpal.api.Violation;
import org.junit.Test;

public class DefaultErrorListenerTest {

    final Exception simpleCause = new IllegalStateException(new IllegalArgumentException());

    @Test
    public void testGetReportedViolations() {
        final DefaultErrorListener errorListener = new DefaultErrorListener();
        errorListener.reportViolation(new SimpleViolation(Violation.Severity.MINOR, "minor"));
        errorListener.reportViolation(new SimpleViolation(Violation.Severity.MAJOR, "major"));
        errorListener.reportViolation(new SimpleViolation(Violation.Severity.SEVERE, "severe"));
        assertEquals("should have reported", 3, errorListener.getReportedViolations().size());
    }

    @Test
    public void testOnNodeTypeRegistrationError() {
        new DefaultErrorListener().onNodeTypeRegistrationError(simpleCause, null);
    }

    @Test
    public void testOnJcrNamespaceRegistrationError() {
        new DefaultErrorListener().onJcrNamespaceRegistrationError(simpleCause, null, null);
    }

    @Test
    public void testOnJcrPrivilegeRegistrationError() {
        new DefaultErrorListener().onJcrPrivilegeRegistrationError(simpleCause, null);
    }

    @Test
    public void testOnForcedRootCreationError() {
        new DefaultErrorListener().onForcedRootCreationError(simpleCause, null);
    }

    @Test
    public void testOnListenerException() {
        new DefaultErrorListener().onListenerException(simpleCause, null, null);
    }

    @Test
    public void testOnSubpackageException() {
        new DefaultErrorListener().onSubpackageException(simpleCause, null);
    }

    @Test
    public void testOnImporterException() {
        new DefaultErrorListener().onImporterException(simpleCause, null, null);
    }

    @Test
    public void testOnListenerPathException() {
        new DefaultErrorListener().onListenerPathException(simpleCause, null, null, null);
    }

    @Test
    public void testOnInstallHookError() {
        new DefaultErrorListener().onInstallHookError(simpleCause, null);
    }

    @Test
    public void testOnProhibitedInstallHookRegistration() {
        new DefaultErrorListener().onProhibitedInstallHookRegistration(null);
    }
}