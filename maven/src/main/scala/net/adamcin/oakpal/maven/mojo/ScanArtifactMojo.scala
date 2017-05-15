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

package net.adamcin.oakpal.maven.mojo

import net.adamcin.oakpal.core.JsonUtil
import net.adamcin.oakpal.maven.{InitsPackageScanner, ReportsViolations}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

/**
  * Scans the main project artifact by simulating package installation and listening for violations reported by the
  * configured {@code scriptReporters}.
  */
@Mojo(name = "scan",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST)
class ScanArtifactMojo extends BaseITMojo with InitsPackageScanner with ReportsViolations {

  @Parameter(property = "oakpal.skip.scan")
  var skip = false

  override def execute(): Unit = {
    super.execute()

    skipWithTestsOrExecute(skip) {
      val reports = builder().build().scanPackages(project.getArtifact.getFile)
      if (this.reportFile.getParentFile.isDirectory || this.reportFile.getParentFile.mkdirs()) {
        JsonUtil.writeToFile(reports, this.reportFile)
      }
    }
  }
}
