/*
 * Copyright 2018 Mark Adamcin
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

import java.io.File;
import java.util.Optional;

/**
 * Represents an error that causes a package scan to abort without notifying the {@link ErrorListener}.
 */
public class AbortedScanException extends Exception {

    private final File currentPackageFile;

    public AbortedScanException(Throwable cause, File currentPackageFile) {
        super(cause);
        this.currentPackageFile = currentPackageFile;
    }

    public AbortedScanException(Throwable cause) {
        super(cause);
        this.currentPackageFile = null;
    }

    public Optional<File> getCurrentPackageFile() {
        return Optional.ofNullable(currentPackageFile);
    }
}
