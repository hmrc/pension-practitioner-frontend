import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "pension-practitioner-frontend"

lazy val root = (project in file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq("models._"),
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    PlayKeys.playDefaultPort := 8208,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*TestController;.*LanguageSwitchController",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq("-feature"),
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    Concat.groups := Seq(
      "javascripts/application.js" -> group(Seq("lib/govuk-frontend/govuk/all.js", "lib/hmrc-frontend/hmrc/all.js",
        "javascripts/psp.js"
      ))
    ),
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    Assets / pipelineStages := Seq(concat, uglify)
  )
  .settings(scalaVersion := "2.13.8")
  .settings(inConfig(Test)(testSettings): _*)
  .settings(resolvers += Resolver.jcenterRepo)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)
