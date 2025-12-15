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
    scalacOptions ++= Seq(
      "-feature",
      "-Xfatal-warnings",
      "-Wconf:msg=Flag.*repeatedly:silent",
      "-Wconf:src=routes/.*:silent",
      "-Wconf:src=twirl/.*:silent",
      "-Wconf:src=target/.*:silent",
      "-Wconf:msg=.*unused.*:silent",
      "-Wconf:msg=Implicit.*:s"
    ),

    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.Implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "viewmodels.govuk.all._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
    ),
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    // Removed uglify due to node 20 compile issues.
    // Suspected cause minification of already minified location-autocomplete.min.js -Pavel Vjalicin
    Assets / pipelineStages := Seq(concat)
  )
  .settings(new CodeCoverageSettings().apply() *)
  .settings(scalaVersion := "3.7.1")
  .settings(inConfig(Test)(testSettings) *)
  .settings(resolvers += Resolver.jcenterRepo)

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)
