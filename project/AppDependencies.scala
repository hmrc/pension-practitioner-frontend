import sbt._

object AppDependencies {
  private val bootstrapVersion = "10.4.0"
  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping-play-30"  % "3.3.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"             % "12.21.0",
    "org.webjars.npm"               %  "govuk-frontend"                         % "5.13.0",
    "com.google.inject.extensions"  %  "guice-multibindings"                    % "4.2.3",
    "uk.gov.hmrc"                   %% "domain-play-30"                         % "13.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                   % "2.20.1"
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.scalatest"               %% "scalatest"               % "3.2.19",
    "org.scalatestplus.play"      %% "scalatestplus-play"      % "7.0.2",
    "org.scalatestplus"           %% "mockito-4-6"             % "3.2.15.0",
    "org.scalatestplus"           %% "scalacheck-1-17"         % "3.2.18.0",
    "org.scalacheck"              %% "scalacheck"              % "1.19.0",
    "io.github.wolfendale"        %% "scalacheck-gen-regexp"   % "1.1.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
