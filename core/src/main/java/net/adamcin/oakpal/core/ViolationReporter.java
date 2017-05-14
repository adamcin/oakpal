/*
 * Copyright 2017 Mark Adamcin
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

import java.net.URL;
import java.util.Collection;

import aQute.bnd.annotation.ConsumerType;

/**
 * Base interface for {@link PackageListener} and {@link ErrorListener}.
 */
@ConsumerType
public interface ViolationReporter {

    /**
     * Return a URL identifying the reporter.
     *
     * @return a URL identifying the reporter
     */
    URL getReporterUrl();

    /**
     * Called at the end of execution to collect any detected violations.
     * @return any reported violations.
     */
    Collection<Violation> reportViolations();
}
