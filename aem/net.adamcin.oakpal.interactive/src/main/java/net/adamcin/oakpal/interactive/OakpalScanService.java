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

package net.adamcin.oakpal.interactive;

import java.io.IOException;

import net.adamcin.oakpal.core.AbortedScanException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Service that encapsulates the builder and cleanup logic necessary for performing an OakMachine package scan.
 */
public interface OakpalScanService {

    /**
     * Perform the specified scan, using the provided ResourceResolver to resolve each package path from
     * {@link OakpalScanInput#getPreInstallPackagePaths()} and {@link OakpalScanInput#getPackagePaths()}, as well as to
     * export the platform JCR nodetypes by adapting it to {@link javax.jcr.Session}.
     *
     * @param resolver the ResourceResolver for access to the platform repository
     * @param input    the Oakpal scan input
     * @return the result of the scan
     * @throws IOException          for I/O related errors, like filesystem issues
     * @throws AbortedScanException if the scan could not complete for some reason
     */
    OakpalScanResult performScan(ResourceResolver resolver, OakpalScanInput input) throws IOException, AbortedScanException;
}
