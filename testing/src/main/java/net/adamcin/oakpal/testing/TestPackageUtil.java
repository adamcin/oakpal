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

package net.adamcin.oakpal.testing;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * FileVault Test Package Factory
 */
public final class TestPackageUtil {
    private TestPackageUtil() {
        // no construct
    }

    private static final Logger log = LoggerFactory.getLogger(TestPackageUtil.class);
    private static final Properties properties = new Properties();
    private static String testPackagesSrc;

    static final String PN_TEST_PACKAGES_SRC = "test-packages.src";
    static final String PN_TEST_PACKAGES_ROOT = "test-packages.root";
    private static Path testPackagesRoot;

    static void loadPropertiesFromResource(final @NotNull Properties props, final @NotNull String resourceName) {
        try (InputStream propsIn = TestPackageUtil.class.getResourceAsStream(resourceName)) {
            if (propsIn != null) {
                props.load(propsIn);
            } else {
                throw new IOException("failed to open stream from resource: " + resourceName);
            }
        } catch (IOException e) {
            log.error("Failed to load properties from " + resourceName + " (see cause in DEBUG)");
            log.debug("Failed to load properties from " + resourceName, e);
        }
    }

    static {
        loadPropertiesFromResource(properties, "/test-packages.properties");

        testPackagesSrc = properties.getProperty(PN_TEST_PACKAGES_SRC, "/oakpal-testing/test-packages/");
        // replace legacy non-windows suffix if it exists, then force resolved test-packages subdir.
        testPackagesRoot = Paths.get(properties.getProperty(PN_TEST_PACKAGES_ROOT, "target")
                .replaceAll("/test-packages$", "")).resolve("test-packages");
        testPackagesRoot.toFile().mkdirs();
    }

    public static File prepareTestPackage(final String filename) throws IOException {
        File file = new File(testPackagesRoot.toFile(), filename);
        final String resourceName = testPackagesSrc + filename;
        try (InputStream is = TestPackageUtil.class.getResourceAsStream(resourceName);
             FileOutputStream fos = new FileOutputStream(file)) {
            if (is == null) {
                throw new IOException("failed to open resource as stream: " + resourceName);
            }
            IOUtils.copy(is, fos);
        }
        return file;
    }

    public static File getCaliperPackage() {
        return Paths.get("target/test-classes/oakpal-caliper.all.zip").toFile();
    }

    public static File deleteTestPackage(final @NotNull String filename) throws IOException {
        final File file = new File(testPackagesRoot.toFile(), filename);
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    public static File prepareTestPackageFromFolder(final @NotNull String filename,
                                                    final @NotNull File srcFolder) throws IOException {

        return prepareTestPackageFromFolder(filename, srcFolder, Collections.emptyMap());
    }

    public static File prepareTestPackageFromFolder(final @NotNull String filename,
                                                    final @NotNull File srcFolder,
                                                    final @NotNull Map<String, File> additionalEntries) throws IOException {
        final File absFile = srcFolder.getAbsoluteFile();
        if (!absFile.isDirectory()) {
            throw new IOException("expected directory in srcFolder parameter for test package filename "
                    + filename + ", srcFolder exists " + srcFolder);
        }
        File file = new File(testPackagesRoot.toFile(), filename);
        buildJarFromDir(absFile, file.getAbsoluteFile(), additionalEntries);

        return file;
    }

    static final IOFileFilter includedEntry = new IOFileFilter() {
        @Override
        public boolean accept(File file) {
            return !("META-INF".equals(file.getParentFile().getName()) && "MANIFEST.MF".equals(file.getName()));
        }

        @Override
        public boolean accept(File dir, String name) {
            return !("META-INF".equals(dir.getName()) && "MANIFEST.MF".equals(name));
        }
    };

    static void buildJarOutputStreamFromDir(final @NotNull File srcDir,
                                            final @NotNull JarOutputStream jos,
                                            final @NotNull Map<String, File> additionalEntries) throws IOException {
        final String absPath = srcDir.getAbsolutePath();
        for (File file : FileUtils.listFilesAndDirs(srcDir, includedEntry, TrueFileFilter.INSTANCE)) {
            final String filePath = file.getAbsolutePath();
            final String entryName = filePath.substring(absPath.length())
                    .replaceFirst("^" + Pattern.quote(File.separator) + "?", "")
                    .replace(File.separator, "/");
            if (entryName.isEmpty()) {
                continue;
            }
            if (file.isDirectory()) {
                JarEntry entry = new JarEntry(entryName + "/");
                entry.setTime(file.lastModified());
                jos.putNextEntry(entry);
            } else {
                JarEntry entry = new JarEntry(entryName);
                entry.setTime(file.lastModified());
                jos.putNextEntry(entry);
                try (FileInputStream fileInput = new FileInputStream(file)) {
                    IOUtils.copy(fileInput, jos);
                }
            }
            jos.closeEntry();
        }
        for (Map.Entry<String, File> add : additionalEntries.entrySet()) {
            final String entryName = add.getKey();
            if (entryName.isEmpty()) {
                continue;
            }
            if (add.getValue().isDirectory()) {
                JarEntry entry = new JarEntry(entryName.replaceFirst("/?$", "/"));
                entry.setTime(add.getValue().lastModified());
                jos.putNextEntry(entry);
            } else {
                JarEntry entry = new JarEntry(entryName.replaceFirst("/?$", ""));
                entry.setTime(add.getValue().lastModified());
                jos.putNextEntry(entry);
                try (FileInputStream fileInput = new FileInputStream(add.getValue())) {
                    IOUtils.copy(fileInput, jos);
                }
            }
            jos.closeEntry();
        }
    }

    public static void buildJarFromDir(final @NotNull File srcDir,
                                       final @NotNull File targetJar,
                                       final @NotNull Map<String, File> additionalEntries) throws IOException {
        if (!targetJar.exists()) {
            final File targetDir = targetJar.getParentFile();
            if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                throw new IOException("failed to create parent target directory: " + targetDir.getAbsolutePath());
            }
            final File manifestFile = new File(srcDir, JarFile.MANIFEST_NAME);
            if (manifestFile.exists()) {
                try (InputStream manIn = new FileInputStream(manifestFile)) {
                    final Manifest man = new Manifest(manIn);
                    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(targetJar), man)) {
                        buildJarOutputStreamFromDir(srcDir, jos, additionalEntries);
                    }
                }
            } else {
                try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(targetJar))) {
                    buildJarOutputStreamFromDir(srcDir, jos, additionalEntries);
                }
            }
        }
    }
}
