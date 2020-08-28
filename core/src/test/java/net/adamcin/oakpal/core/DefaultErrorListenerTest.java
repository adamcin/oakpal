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
import net.adamcin.oakpal.api.RepoInitScriptsInstallable;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleViolation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class DefaultErrorListenerTest {

    final Exception simpleCause = new IllegalStateException(new IllegalArgumentException());

    @Test
    public void testSetResourceBundle() {
        final DefaultErrorListener listener = new DefaultErrorListener();

        ResourceBundle originalBundle = listener.getResourceBundle();
        assertSame("same object returned twice", originalBundle, listener.getResourceBundle());
        ResourceBundle newBundle = ResourceBundle.getBundle(listener.getResourceBundleBaseName(),
                Locale.getDefault(), new URLClassLoader(new URL[0], getClass().getClassLoader()));
        assertNotSame("not same object as created externally", newBundle, listener.getResourceBundle());
        listener.setResourceBundle(newBundle);
        assertSame("same object as set", newBundle, listener.getResourceBundle());
        assertSame("same object as set, again", newBundle, listener.getResourceBundle());
    }

    @Test
    public void testGetString() {
        final DefaultErrorListener listener = new DefaultErrorListener();
        assertEquals("expect passthrough", "testKey", listener.getString("testKey"));
        ResourceBundle newBundle = ResourceBundle.getBundle(getClass().getName());
        listener.setResourceBundle(newBundle);
        assertEquals("expect from bundle", "yeKtset", listener.getString("testKey"));
    }

    @Test
    public void testGetReportedViolations() {
        final DefaultErrorListener errorListener = new DefaultErrorListener();
        errorListener.reportViolation(new SimpleViolation(Severity.MINOR, "minor"));
        errorListener.reportViolation(new SimpleViolation(Severity.MAJOR, "major"));
        errorListener.reportViolation(new SimpleViolation(Severity.SEVERE, "severe"));
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

    @Test
    public void testOnRepoInitUrlError() {
        new DefaultErrorListener().onRepoInitUrlError(simpleCause, null);
    }

    @Test
    public void testOnRepoInitInlineError() {
        new DefaultErrorListener().onRepoInitInlineError(simpleCause, null);
    }

    @Test
    public void testOnSlingEmbeddedPackageError() {
        new DefaultErrorListener().onSlingEmbeddedPackageError(simpleCause,
                new EmbeddedPackageInstallable(PackageId.fromString("test"), "/some/path",
                        PackageId.fromString("testtest")));
    }

    @Test
    public void testOnSlingRepoInitScriptsError() {
        new DefaultErrorListener().onSlingRepoInitScriptsError(simpleCause, null,
                new RepoInitScriptsInstallable(PackageId.fromString("test"), "/some/path",
                        Arrays.asList("some", "scripts")));
    }
}