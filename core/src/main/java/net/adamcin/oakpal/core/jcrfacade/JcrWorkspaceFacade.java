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

package net.adamcin.oakpal.core.jcrfacade;

import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * Wraps a {@link javax.jcr.Workspace} to guards against writes by listeners.
 *
 * @param <S> Session type, likely to be {@link Session}.
 */
class JcrWorkspaceFacade<S extends Session> extends WorkspaceFacade<S, Workspace> implements Workspace {

    JcrWorkspaceFacade(final Workspace delegate, final SessionFacade<S> session) {
        super(delegate, session);
    }
}
