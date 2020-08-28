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

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Manifest;

/**
 * Simple verbose package check that logs all scan events to standard out. Extend to override methods.
 */
@ConsumerType
public class Echo implements ProgressCheck {

    private final AtomicLong lastEvent = new AtomicLong(System.nanoTime());

    @Override
    public Collection<Violation> getReportedViolations() {
        return Collections.emptyList();
    }

    /**
     * Stops the current interval, starts a new interval, and returns the duration.
     *
     * @return the duration of the stopped interval
     */
    private Duration stopInterval() {
        final long now = System.nanoTime();
        final long last = this.lastEvent.getAndSet(now);
        return Duration.ofNanos(now - last);
    }

    /**
     * Formats a duration.
     *
     * @param duration the duration
     * @return the formatted duration timestamp
     */
    private String formatDuration(final @NotNull Duration duration) {
        final long seconds = duration.getSeconds();
        final int nanos = duration.getNano();
        return String.format("%d:%02d:%02d:%03d", seconds / 3600, (seconds % 3600) / 60, seconds % 60, nanos / 1000000);
    }

    /**
     * Override this method to use a logger instead of System.out, for example.
     *
     * @param message    message to print
     * @param formatArgs optional args to {@link String#format(String, Object...)}
     */
    protected void echo(final String message, final @NotNull Object... formatArgs) {
        final Duration diff = stopInterval();

        System.out.println("[ECHO " + formatDuration(diff) + "] " + String.format(message, formatArgs));
    }

    @Override
    public String getCheckName() {
        return "echo";
    }

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        echo("identifyPackage(packageId: %s, file: %s)", packageId,
                Optional.ofNullable(file).map(File::getAbsolutePath).orElse(null));
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        echo("identifySubpackage(packageId: %s, parentId: %s)", packageId, parentId);
    }

    @Override
    public void readManifest(final PackageId packageId, final Manifest manifest) {
        echo("readManifest(packageId: %s, manifestKeys: %s)", packageId,
                Optional.ofNullable(manifest).map(man -> man.getEntries().keySet()).orElse(null));
    }

    @Override
    public void beforeExtract(final PackageId packageId, final Session inspectSession,
                              final PackageProperties packageProperties, final MetaInf metaInf,
                              final List<PackageId> subpackages) throws RepositoryException {
        echo("beforeExtract(packageId: %s, ..., subpackages: %s)", packageId, subpackages);
    }

    @Deprecated
    public void importedPath(final PackageId packageId, final String path, final Node node) throws RepositoryException {
        importedPath(packageId, path, node, PathAction.UNKNOWN);
    }

    @Override
    public void importedPath(final PackageId packageId, final String path, final Node node, final PathAction action)
            throws RepositoryException {
        echo("importedPath(packageId: %s, path: %s, ..., action: %s)", packageId, path, action);
    }

    @Override
    public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
            throws RepositoryException {
        echo("deletedPath(packageId: %s, path: %s, ...)", packageId, path);
    }

    @Override
    public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        echo("afterExtract(packageId: %s, inspectSession: %s)", packageId,
                sessionToString(inspectSession));
    }

    @Override
    public void simulateSling(final SlingSimulator slingSimulator, final Set<String> runModes) {
        echo("simulateSling(slingSimulator: %s, runModes: %s)", slingSimulator.getClass().getName(), runModes);
    }

    @Override
    public void identifyEmbeddedPackage(final PackageId packageId, final PackageId parentId, final String jcrPath) {
        echo("identifyEmbeddedPackage(packageId: %s, parentId: %s, jcrPath: %s)", packageId, parentId, jcrPath);
    }

    @Override
    public void beforeSlingInstall(final PackageId lastPackage, final SlingInstallable<?> slingInstallable, final Session inspectSession) throws RepositoryException {
        echo("beforeSlingInstall(lastPackage: %s, slingInstallable: %s, inspectSession: %s)", lastPackage, slingInstallable,
                sessionToString(inspectSession));
    }

    @Override
    public void appliedRepoInitScripts(final PackageId lastPackage, final SlingInstallable<?> slingInstallable, final Session inspectSession) throws RepositoryException {
        echo("appliedRepoInitScripts(lastPackage: %s, slingInstallable: %s, inspectSession: %s)", lastPackage, slingInstallable,
                sessionToString(inspectSession));
    }

    @Override
    public void afterScanPackage(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        echo("afterScanPackage(packageId: %s, inspectSession: %s)", packageId, sessionToString(inspectSession));
    }

    @Nullable String sessionToString(final @Nullable Session session) throws RepositoryException {
        return Optional.ofNullable(session).map(Session::getUserID).orElse(null);
    }

    @Override
    public void startedScan() {
        echo("startedScan()");
    }

    @Override
    public void finishedScan() {
        echo("finishedScan()");
    }
}
