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

package net.adamcin.oakpal.webster;

import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Session;

/**
 * Simple interface for executing a Webster action that generates one source files.
 */
@ProviderType
public interface WebsterTarget {

    /**
     * Perform the action using the given session.
     *
     * @param session the JCR session to export from
     * @throws Exception for any error
     */
    void perform(Session session) throws Exception;
}
