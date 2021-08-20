import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %%  "simple-reactivemongo"           % "8.0.0-play-28",
    "uk.gov.hmrc"                   %%  "logback-json-logger"            % "5.1.0",
    "uk.gov.hmrc"                   %%  "play-conditional-form-mapping"  % "1.9.0-play-28",
    "uk.gov.hmrc"                   %%  "bootstrap-frontend-play-28"     % "5.12.0",
    "uk.gov.hmrc"                   %%  "play-whitelist-filter"          % "3.4.0-play-27",
    "uk.gov.hmrc"                   %%  "play-nunjucks"                  % "0.29.0-play-27",
    "uk.gov.hmrc"                   %%  "play-nunjucks-viewmodel"        % "0.14.0-play-27",
    "org.webjars.npm"               %   "govuk-frontend"                 % "3.7.0",
    "org.webjars.npm"               %   "hmrc-frontend"                  % "1.15.1",
    "com.google.inject.extensions"  %   "guice-multibindings"            % "4.2.2",
    "uk.gov.hmrc"                   %%  "domain"                         % "6.2.0-play-28"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"              % "3.0.8",
    "org.scalatestplus.play"      %% "scalatestplus-play"     % "3.1.2",
    "org.pegdown"                 %  "pegdown"                % "1.6.0",
    "org.jsoup"                   %  "jsoup"                  % "1.10.3",
    "com.typesafe.play"           %% "play-test"              % PlayVersion.current,
    "org.scalacheck"              %% "scalacheck"             % "1.14.0",
    "com.github.tomakehurst"      %  "wiremock-jre8"          % "2.26.0",
    "org.mockito"                 %  "mockito-core" % "3.7.7"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
