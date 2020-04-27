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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @deprecated 2.0.0 use {@link net.adamcin.oakpal.api.SimpleProgressCheck}
 */
@Deprecated
@ProviderType
public class SimpleProgressCheck extends net.adamcin.oakpal.api.SimpleProgressCheck implements ProgressCheck {

    @Override
    protected final @Nullable ResourceBundle getResourceBundle() throws MissingResourceException {
        return null;
    }

    protected final void reportViolation(final Violation.Severity severity,
                                         final String description,
                                         final PackageId... packages) {
        this.reportViolation(severity.getSeverity(), description, packages);
    }
}
