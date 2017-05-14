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

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.settings.Settings

/**
  * Base mojo class
  *
  * @since 0.1.0
  */
abstract class BaseMojo extends AbstractMojo {

  @Parameter(defaultValue = "${settings}", readonly = true)
  var settings: Settings = null

  @Parameter(defaultValue = "${session}", readonly = true)
  var session: MavenSession = null

  override def execute(): Unit = {}

  def skipOrExecute(skip: Boolean)(body: => Unit) {
    if (skip) {
      getLog.info("skipping [skip=" + skip + "]")
    } else {
      body
    }
  }


}
