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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileVault Test Package Factory
 */
public class TestPackageUtil {

    private TestPackageUtil() {
        // no construct
    }

    static final Logger log = LoggerFactory.getLogger(TestPackageUtil.class);

    static final String PN_TEST_PACKAGES_SRC = "test-packages.src";
    static final String PN_TEST_PACKAGES_ROOT = "test-packages.root";
    private static Path testPackagesRoot;
    private static String testPackagesSrc;
    static final Properties properties = new Properties();

    static {
        try (InputStream propsIn = TestPackageUtil.class.getResourceAsStream("/test-packages.properties")) {
            if (propsIn != null) {
                properties.load(propsIn);
            }
        } catch (IOException e) {
            log.error("Failed to load test-packages.properties");
        }
        testPackagesSrc = properties.getProperty(PN_TEST_PACKAGES_SRC, "/oakpal-testing/test-packages/");
        // replace legacy non-windows suffix if it exists, then force resolved test-packages subdir.
        testPackagesRoot = Paths.get(properties.getProperty(PN_TEST_PACKAGES_ROOT, "target")
                .replaceAll("/test-packages$", "")).resolve("test-packages");
        testPackagesRoot.toFile().mkdir();
    }

    public static File prepareTestPackage(final String filename) throws IOException {
        File file = new File(testPackagesRoot.toFile(), filename);
        try (InputStream is = TestPackageUtil.class.getResourceAsStream(testPackagesSrc + filename);
             FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(is, fos);
        }
        return file;
    }

    public static File prepareTestPackageFromFolder(final String filename, final File srcFolder) throws IOException {
        if (srcFolder == null || !srcFolder.isDirectory()) {
            throw new IOException("expected directory in srcFolder parameter for test package filename " + filename);
        }
        File file = new File(testPackagesRoot.toFile(), filename);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            add(srcFolder, srcFolder, jos);
        }

        return file;
    }

    static void add(final File root, final File source, final JarOutputStream target) throws IOException {
        if (root == null || source == null) {
            throw new IllegalArgumentException("Cannot add from a null file");
        }
        if (!(source.getPath() + File.separator).startsWith(root.getPath() + File.separator)) {
            throw new IllegalArgumentException("source must be the same file or a child of root");
        }
        final String relPath;
        if (!root.getPath().equals(source.getPath())) {
            relPath = source.getPath().substring(root.getPath().length() + 1).replace(File.separator, "/");
        } else {
            relPath = "";
        }
        if (source.isDirectory()) {
            if (!relPath.isEmpty()) {
                String name = relPath;
                if (!name.endsWith("/")) {
                    name += "/";
                }
                JarEntry entry = new JarEntry(name);
                entry.setTime(source.lastModified());
                target.putNextEntry(entry);
                target.closeEntry();
            }
            File[] children = source.listFiles();
            if (children != null) {
                for (File nestedFile : children) {
                    add(root, nestedFile, target);
                }
            }
        } else {
            JarEntry entry = new JarEntry(relPath);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            try (InputStream in = new BufferedInputStream(new FileInputStream(source))) {
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1)
                        break;
                    target.write(buffer, 0, count);
                }
                target.closeEntry();
            }
        }
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


    public static void buildJarFromDir(final File srcDir, final File targetJar, final Map<String, File> additionalEntries) throws Exception {
        if (!targetJar.exists()) {
            try (InputStream manIn = new FileInputStream(new File(srcDir, JarFile.MANIFEST_NAME))) {
                final Manifest man = new Manifest(manIn);
                final File targetDir = targetJar.getParentFile();
                if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                    throw new IOException("failed to create parent target directory: " + targetDir.getAbsolutePath());
                }
                try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(targetJar), man)) {
                    final String absPath = srcDir.getAbsolutePath();
                    for (File file : FileUtils.listFilesAndDirs(srcDir, includedEntry, TrueFileFilter.INSTANCE)) {
                        final String filePath = file.getAbsolutePath();
                        final String entryName = filePath.substring(absPath.length())
                                .replaceFirst("^/?", "")
                                .replace(File.separator, "/");
                        if (file.isDirectory()) {
                            jos.putNextEntry(new ZipEntry(entryName + "/"));
                        } else {
                            jos.putNextEntry(new ZipEntry(entryName));
                            try (FileInputStream fileInput = new FileInputStream(file)) {
                                IOUtils.copy(fileInput, jos);
                            }
                        }
                        jos.closeEntry();
                    }
                    for (Map.Entry<String, File> add : additionalEntries.entrySet()) {
                        final String entryName = add.getKey();
                        if (add.getValue().isDirectory()) {
                            jos.putNextEntry(new ZipEntry(entryName.replaceAll("/?$", "/")));
                        } else {
                            jos.putNextEntry(new ZipEntry(entryName));
                            try (FileInputStream fileInput = new FileInputStream(add.getValue())) {
                                IOUtils.copy(fileInput, jos);
                            }
                        }
                        jos.closeEntry();
                    }
                }
            }
        }
    }
}
