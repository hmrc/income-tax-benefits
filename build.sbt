import uk.gov.hmrc.DefaultBuildSettings

val appName = "income-tax-benefits"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"

lazy val coverageSettings: Seq[Setting[?]] = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*standardError*.*",
    ".*govuk_wrapper*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "controllers.testOnly.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 99,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=html/.*:s",
      "-Wconf:msg=unused import&src=.*routes.*:s",
      "-Wconf:msg=unused.*&src=.*routes.*:s"
    )
  )
  .settings(PlayKeys.playDefaultPort := 9319)
  .settings(coverageSettings *)
  .settings(
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(Seq(Test / parallelExecution := false))
