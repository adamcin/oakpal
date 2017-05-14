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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileVault Test Package Factory
 */
public class TestPackageUtil {
    static final Logger log = LoggerFactory.getLogger(TestPackageUtil.class);

    static final String PN_TEST_PACKAGES_SRC = "test-packages.src";
    static final String PN_TEST_PACKAGES_ROOT = "test-packages.root";
    private static String testPackagesRoot;
    private static String testPackagesSrc;
    static final Properties properties = new Properties();

    static {
        try {
            properties.load(TestPackageUtil.class.getResourceAsStream("/test-packages.properties"));
            testPackagesSrc = properties.getProperty(PN_TEST_PACKAGES_SRC);
            testPackagesRoot = properties.getProperty(PN_TEST_PACKAGES_ROOT);
            (new File(testPackagesRoot)).mkdir();
        } catch (IOException e) {
            log.error("Failed to load test-packages.properties");
            testPackagesRoot = "<invalid_root>";
        }
    }

    public static File prepareTestPackage(String filename) throws IOException {
        File file = new File(testPackagesRoot, filename);
        if (file.exists()) {
            file.delete();
        }
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = TestPackageUtil.class.getResourceAsStream(testPackagesSrc + filename);
            fos = new FileOutputStream(file);
            IOUtils.copy(is, fos);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(fos);
        }
        return file;
    }
}
