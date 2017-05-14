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

import net.adamcin.oakpal.maven.RequiresProject
import org.apache.maven.plugins.annotations.Parameter

/**
  * Base project IT mojo defining common mojo parameters and methods for enabling/disabling traffic to the configured
  * integration test server for goals bound to the integration test phase of the default lifecycle.
  * @since 0.1.0
  */
class BaseITMojo extends BaseMojo with RequiresProject {


  /**
    * By convention, this parameter is used to disable execution of the maven-failsafe-plugin.
    * It is recognized by graniteit to disable uploading of test artifacts and integration test reporting goals.
    */
  @Parameter(property = "skipITs")
  val skipITs = false

  /**
    * By convention, this parameter is used to disable execution of maven-surefire-plugin-derived goals.
    * It is recognized by graniteit to disable uploading of test artifacts and integration test reporting goals.
    */
  @Parameter(property = "skipTests")
  val skipTests = false

  override def skipOrExecute(skip: Boolean)(body: => Unit) {
    if (skip) {
      getLog.info("skipping [skip=" + skip + "]")
    } else {
      body
    }
  }

  def skipWithTestsOrExecute(skip: Boolean)(body: => Unit) {
    if (skip || skipITs || skipTests) {
      getLog.info("skipping [skip=" + skip + "][skipITs=" + skipITs + "][skipTests=" + skipTests + "]")
    } else {
      body
    }
  }
}
