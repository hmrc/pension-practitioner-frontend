import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %%  "logback-json-logger"            % "5.2.0",
    "uk.gov.hmrc"                   %%  "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"                   %%  "bootstrap-frontend-play-28"     % "7.8.0",
    "uk.gov.hmrc"                   %%  "play-nunjucks"                  % "0.40.0-play-28",
    "uk.gov.hmrc"                   %%  "play-nunjucks-viewmodel"        % "0.16.0-play-28",
    "org.webjars.npm"               %   "govuk-frontend"                 % "3.7.0",
    "org.webjars.npm"               %   "hmrc-frontend"                  % "1.15.1",
    "com.google.inject.extensions"  %   "guice-multibindings"            % "4.2.3",
    "uk.gov.hmrc"                   %%  "domain"                         % "8.1.0-play-28"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"              % "3.2.14",
    "org.scalatestplus.play"      %% "scalatestplus-play"     % "5.1.0",
    "org.scalatestplus"           %% "mockito-3-4"            % "3.2.10.0",
    "org.scalatestplus"           %% "scalacheck-1-15"        % "3.2.11.0",
    "org.pegdown"                 %  "pegdown"                % "1.6.0",
    "com.typesafe.play"           %% "play-test"              % PlayVersion.current,
    "org.scalacheck"              %% "scalacheck"             % "1.17.0",
    "com.github.tomakehurst"      %  "wiremock-jre8"          % "2.26.0",
    "com.vladsch.flexmark"        %  "flexmark-all"           % "0.62.2",
    "org.scoverage"               %  "sbt-scoverage_2.12_1.0" % "2.0.5"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
