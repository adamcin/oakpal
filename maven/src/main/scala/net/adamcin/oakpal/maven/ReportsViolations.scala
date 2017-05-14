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

package net.adamcin.oakpal.maven

import java.io.File

import org.apache.maven.plugins.annotations.Parameter

/**
  * Trait defining the location of the violations report file.
  * @since 0.1.0
  */
trait ReportsViolations extends RequiresProject {

  final val defaultReportFile = "${project.build.directory}/oakpal/scan-violations.json"

  @Parameter(property = "reportFile", defaultValue = defaultReportFile)
  val reportFile = new File(defaultReportFile)


}
