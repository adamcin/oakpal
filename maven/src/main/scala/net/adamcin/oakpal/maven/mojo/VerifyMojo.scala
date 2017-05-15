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
import net.adamcin.oakpal.core.Violation.Severity
import net.adamcin.oakpal.maven.ReportsViolations
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

import scala.collection.JavaConverters

/**
  * Verifies the violation report created during the integration-test phase.
  */
@Mojo(name = "verify",
  defaultPhase = LifecyclePhase.VERIFY)
class VerifyMojo extends BaseITMojo with ReportsViolations {

  @Parameter(property = "oakpal.skip.verify")
  var skip = false

  @Parameter
  val failOnSeverity: Severity = Severity.MAJOR

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {
      val errorMessage = s"Violations were reported at or above severity: $failOnSeverity"
      if (reportFile.exists()) {
        val reports = JavaConverters.collectionAsScalaIterable(JsonUtil.readFromFile(reportFile))
        val nonEmptyReports = reports.filterNot(_.getViolations().isEmpty)
        val shouldFail = !nonEmptyReports.forall { r => r.getViolations(failOnSeverity).isEmpty }
        if (nonEmptyReports.nonEmpty) {
          nonEmptyReports.foreach { r =>
            getLog.info(s"OakPAL Reporter: ${r.getReporterUrl}")
            val viols = JavaConverters.collectionAsScalaIterable(r.getViolations())
            viols.foreach { v =>
              if (v.getSeverity.isLessSevereThan(failOnSeverity)) {
                getLog.info(v.getDescription)
              } else {
                getLog.error(v.getDescription)
              }
            }
          }
        }

        if (shouldFail) {
          getLog.error(errorMessage)
          throw new MojoFailureException(errorMessage)
        }
      }
    }
  }
}
