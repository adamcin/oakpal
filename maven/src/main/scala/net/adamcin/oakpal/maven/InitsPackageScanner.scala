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
import java.util.Collections

import net.adamcin.oakpal.core.{PackageScanner, ScriptPackageListener}
import net.adamcin.oakpal.maven.mojo.BaseMojo
import org.apache.maven.plugins.annotations.Parameter

import scala.collection.JavaConverters

/**
  * Created by madamcin on 5/11/17.
  */
trait InitsPackageScanner extends BaseMojo {

  @Parameter(name = "scriptReporters")
  val scriptReporters: java.util.List[File] = Collections.emptyList()

  @Parameter(name = "cndFiles")
  val cndFiles: java.util.List[File] = Collections.emptyList()

  def builder(): PackageScanner.Builder = {

    val listeners = JavaConverters.asScalaBuffer(scriptReporters).map { s =>
      ScriptPackageListener.createScriptHandler("nashorn", s.toURI.toURL) }
    new PackageScanner.Builder().withPackageListeners(listeners:_*)
      .withCndFiles(JavaConverters.asScalaBuffer(cndFiles).toArray:_*)
  }
}
