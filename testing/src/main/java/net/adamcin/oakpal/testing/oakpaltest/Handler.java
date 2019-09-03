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

package net.adamcin.oakpal.testing.oakpaltest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;

public class Handler extends URLStreamHandler {
    @SuppressWarnings("WeakerAccess")
    public static final String PROTO = "oakpaltest";
    static final String HANDLER_PKG = "net.adamcin.oakpal.testing";
    static final String PROP = "java.protocol.handler.pkgs";

    public static void register() {
        final String was = System.getProperty(PROP, "");
        if (!was.contains(HANDLER_PKG)) {
            System.setProperty("java.protocol.handler.pkgs", HANDLER_PKG + (was.isEmpty() ? "" : "|" + was));
        }
    }

    private final Path basedir = new File(".").toPath().normalize();

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (!PROTO.equals(url.getProtocol())) {
            return null;
        }
        final String path = url.getPath().replaceFirst("^/+", "");
        return basedir.resolve(path).normalize().toUri().toURL().openConnection();
    }
}
