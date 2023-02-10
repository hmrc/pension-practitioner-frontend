import sbt._

object AppDependencies {

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %%  "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"                   %%  "bootstrap-frontend-play-28"     % "7.13.0",
    "uk.gov.hmrc"                   %%  "play-nunjucks"                  % "0.41.0-play-28",
    "uk.gov.hmrc"                   %%  "play-nunjucks-viewmodel"        % "0.17.0-play-28",
    "org.webjars.npm"               %   "govuk-frontend"                 % "4.3.1",
    "org.webjars.npm"               %   "hmrc-frontend"                  % "1.35.2",
    "com.google.inject.extensions"  %   "guice-multibindings"            % "4.2.3",
    "uk.gov.hmrc"                   %%  "domain"                         % "8.1.0-play-28",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"           % "2.14.2"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"              % "3.2.15",
    "org.scalatestplus.play"      %% "scalatestplus-play"     % "5.1.0",
    "org.scalatestplus"           %% "mockito-4-6"            % "3.2.15.0",
    "org.scalatestplus"           %% "scalacheck-1-17"        % "3.2.15.0",
    "org.pegdown"                 %  "pegdown"                % "1.6.0",
    "org.scalacheck"              %% "scalacheck"             % "1.17.0",
    "com.github.tomakehurst"      %  "wiremock-jre8"          % "2.35.0",
    "com.vladsch.flexmark"        %  "flexmark-all"           % "0.64.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
