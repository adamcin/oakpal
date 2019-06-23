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

import static net.adamcin.oakpal.core.Fun.uncheck1;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;
import javax.jcr.Node;

/**
 * Represents an error that causes a package scan to abort without notifying the {@link ErrorListener}.
 */
public class AbortedScanException extends Exception {

    private final URL currentPackageUrl;
    private final File currentPackageFile;
    private final Node currentPackageNode;

    public AbortedScanException(final Throwable cause, final File currentPackageFile) {
        super(cause);
        this.currentPackageFile = currentPackageFile;
        this.currentPackageNode = null;
        this.currentPackageUrl = null;
    }

    public AbortedScanException(final Throwable cause, final URL currentPackageUrl) {
        super(cause);
        this.currentPackageFile = null;
        this.currentPackageNode = null;
        this.currentPackageUrl = currentPackageUrl;
    }

    public AbortedScanException(final Throwable cause, final Node currentPackageNode) {
        super(cause);
        this.currentPackageFile = null;
        this.currentPackageNode = currentPackageNode;
        this.currentPackageUrl = null;
    }

    public AbortedScanException(Throwable cause) {
        super(cause);
        this.currentPackageFile = null;
        this.currentPackageNode = null;
        this.currentPackageUrl = null;
    }

    public Optional<URL> getCurrentPackageUrl() {
        return Optional.ofNullable(currentPackageUrl);
    }

    public Optional<File> getCurrentPackageFile() {
        return Optional.ofNullable(currentPackageFile);
    }

    public Optional<Node> getCurrentPackageNode() {
        return Optional.ofNullable(currentPackageNode);
    }

    @Override
    public String getMessage() {
        return getFailedPackageMessage() + super.getMessage();
    }

    public String getFailedPackageMessage() {
        return Stream.of(
                Optional.ofNullable(this.currentPackageNode).map(uncheck1(Node::getPath)),
                Optional.ofNullable(this.currentPackageFile).map(File::getAbsolutePath),
                Optional.ofNullable(this.currentPackageUrl).map(URL::toString))
                .filter(Optional::isPresent)
                .findFirst()
                .map(location -> "(Failed package: " + location + ") ").orElse("");
    }
}
