/*
 * Copyright 2022 HM Revenue & Customs
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

import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.14.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatest" %% "scalatest"              % "3.2.15"         % Test,
////    "com.typesafe.play"       %% "play-test"                % current                 % Test,
    //    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.64.6" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"  % Test,
    "com.github.tomakehurst"  % "wiremock-jre8-standalone" % "2.35.1" % Test,
    "org.scalamock"          %% "scalamock"                % "5.2.0"  % Test
  )
}
