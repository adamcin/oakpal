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

package net.adamcin.oakpal.core.checks;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.PackageScanner;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressCheckTestBase {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public void logViolations(final String prefix, final CheckReport report) {
        if (!report.getViolations().isEmpty()) {
            report.getViolations().stream().forEachOrdered(viol -> LOGGER.info("{}: {}", prefix, viol));
        }
    }

    public CheckReport scanWithCheck(final ProgressCheck check, final String... filename) throws Exception {
        List<File> artifacts = new ArrayList<>();
        for (String filen : filename) {
            artifacts.add(TestPackageUtil.prepareTestPackage(filen));
        }
        Optional<CheckReport> reports = new PackageScanner.Builder().withProgressChecks(check)
                .build().scanPackages(artifacts).stream()
                .filter(report -> check.getCheckName().equals(report.getCheckName()))
                .findFirst();
        assertTrue(String.format("report for %s is present", check.getCheckName()), reports.isPresent());
        return reports.get();
    }
}
