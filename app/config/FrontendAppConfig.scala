/*
 * Copyright 2020 HM Revenue & Customs
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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String): String = configuration.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def getConfigString(key: String) = servicesConfig.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  lazy val contactHost: String = servicesConfig.baseUrl("contact-frontend")
  lazy val addressLookUp = s"${servicesConfig.baseUrl("address-lookup")}"

  lazy val appName: String = configuration.get[String](path = "appName")
  val analyticsToken: String = configuration.get[String](s"google-analytics.token")
  val analyticsHost: String = configuration.get[String](s"google-analytics.host")

  val reportAProblemPartialUrl: String = getConfigString("contact-frontend.report-problem-url.with-js")
  val reportAProblemNonJSUrl: String = getConfigString("contact-frontend.report-problem-url.non-js")
  val betaFeedbackUrl: String = getConfigString("contact-frontend.beta-feedback-url.authenticated")
  val betaFeedbackUnauthenticatedUrl: String = getConfigString("contact-frontend.beta-feedback-url.unauthenticated")

  lazy val authUrl: String = configuration.get[Service]("auth").baseUrl
  lazy val signOutUrl: String = loadConfig("urls.logout")
  lazy val pspUrl: String = servicesConfig.baseUrl("pension-practitioner")

  lazy val timeoutSeconds: String = configuration.get[String]("session.timeoutSeconds")
  lazy val CountdownInSeconds: String = configuration.get[String]("session.CountdownInSeconds")

  lazy val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("microservice.services.features.welsh-translation")

  lazy val validCountryCodes: Seq[String] = configuration.get[String]("validCountryCodes").split(",").toSeq
  lazy val locationCanonicalList: String = loadConfig("location.canonical.list.all")
  lazy val locationCanonicalListEUAndEEA: String = loadConfig("location.canonical.list.EUAndEEA")

  lazy val pspSubscriptionUrl: String = s"$pspUrl${configuration.get[String]("urls.subscribePsp")}"
  lazy val subscriptionDetailsUrl: String = s"$pspUrl${configuration.get[String]("urls.pspDetails")}"

  lazy val emailApiUrl: String = servicesConfig.baseUrl("email")
  lazy val emailSendForce: Boolean = configuration.getOptional[Boolean]("email.force").getOrElse(false)
  lazy val emailPspSubscriptionTemplateId: String = configuration.get[String]("email.pspSubscriptionTemplateId")
  lazy val emailPspAmendmentTemplateId: String = configuration.get[String]("email.pspAmendmentTemplateId")

  def emailCallback(journeyType: String, requestId: String, encryptedEmail: String, encryptedPspId: String) =
    s"$pspUrl${configuration.get[String](path = "urls.emailCallback").format(journeyType, requestId, encryptedEmail, encryptedPspId)}"

  lazy val registerWithIdOrganisationUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithIdOrganisation")}"

  lazy val registerWithNoIdOrganisationUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithNoIdOrganisation")}"

  lazy val registerWithNoIdIndividualUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithNoIdIndividual")}"

  lazy val registerWithIdIndividualUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithIdIndividual")}"

  lazy val identityVerification: String = servicesConfig.baseUrl("identity-verification")

  lazy val identityVerificationFrontend: String = servicesConfig.baseUrl("identity-verification-frontend")

  lazy val identityVerificationProxy: String = servicesConfig.baseUrl("identity-verification-proxy")

  lazy val ivRegisterOrganisationAsIndividualUrl: String = s"$identityVerificationProxy${configuration.get[String]("urls.ivRegisterOrganisationAsIndividual")}"

  lazy val manualIvUrl: String = configuration.get[String]("urls.manualIvUrl")

  lazy val ukJourneyContinueUrl: String = configuration.get[String]("urls.ukJourneyContinue")

  lazy val companiesHouseFileChangesUrl: String = configuration.get[String]("urls.companiesHouseFileChanges")
  lazy val hmrcChangesMustReportUrl: String = configuration.get[String]("urls.hmrcChangesMustReport")
  lazy val hmrcTaxHelplineUrl: String = configuration.get[String]("urls.hmrcTaxHelpline")

  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")

  lazy val loginUrl: String = configuration.get[String]("urls.login")

  lazy val tellHMRCChangesUrl: String = configuration.get[String]("urls.tellHMRCChanges")

  lazy val registerAsPensionAdministratorUrl: String = configuration.get[String]("urls.registerAsPensionAdministrator")

  lazy val createGovGatewayUrl: String = configuration.get[String]("urls.createGovGateway")

  lazy val returnToPensionSchemesUrl: String = configuration.get[String]("urls.pensionSchemesList")
  lazy val returnToOverviewUrl: String = configuration.get[String]("urls.overview")
  lazy val returnToPspDashboardUrl: String = configuration.get[String]("urls.pspDashboard")

  lazy val govUkUrl: String = configuration.get[String]("urls.govUK")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )


  def routeToSwitchLanguage: String => Call =
    (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
}
