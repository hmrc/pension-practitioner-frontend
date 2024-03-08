/*
 * Copyright 2024 HM Revenue & Customs
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
import models.JourneyType
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String): String = configuration.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def getConfigString(key: String) = servicesConfig.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  def localFriendlyUrl(uri: String): String = loadConfig("host") + uri

  lazy val addressLookUp = s"${servicesConfig.baseUrl("address-lookup")}"

  lazy val appName: String = configuration.get[String](path = "appName")

  val reportAProblemPartialUrl: String = getConfigString("contact-frontend.report-problem-url.with-js")
  val reportAProblemNonJSUrl: String = getConfigString("contact-frontend.report-problem-url.non-js")
  val betaFeedbackUnauthenticatedUrl: String = getConfigString("contact-frontend.beta-feedback-url.unauthenticated")

  lazy val signOutUrl: String = loadConfig("urls.logout")
  lazy val pspUrl: String = servicesConfig.baseUrl("pension-practitioner")
  lazy val pensionAdministratorUrl: String = servicesConfig.baseUrl("pension-administrator")

  lazy val administratorOrPractitionerUrl: String = loadConfig("urls.administratorOrPractitioner")

  def cannotAccessPageAsAdministratorUrl(continueUrl: String): String =
    loadConfig("urls.cannotAccessPageAsAdministrator").format(continueUrl)

  lazy val timeoutSeconds: String = configuration.get[String]("session.timeoutSeconds")
  lazy val CountdownInSeconds: String = configuration.get[String]("session.CountdownInSeconds")

  lazy val validCountryCodes: Seq[String] = configuration.get[String]("validCountryCodes").split(",").toSeq
  lazy val locationCanonicalList: String = loadConfig("location.canonical.list.all")
  lazy val locationCanonicalListEUAndEEA: String = loadConfig("location.canonical.list.EUAndEEA")


  lazy val pspSubscriptionUrl: String = s"$pspUrl${configuration.get[String]("urls.subscribePsp")}"
  lazy val subscriptionDetailsUrl: String = s"$pspUrl${configuration.get[String]("urls.pspDetails")}"
  lazy val pspDeregistrationUrl: String = s"$pspUrl${configuration.get[String]("urls.deregisterPsp")}"
  lazy val canDeregisterUrl: String = s"$pspUrl${configuration.get[String]("urls.canDeregister")}"
  lazy val minimalDetailsUrl: String = s"$pspUrl${configuration.get[String]("urls.minimalDetails")}"

  lazy val emailApiUrl: String = servicesConfig.baseUrl("email")
  lazy val emailSendForce: Boolean = configuration.getOptional[Boolean]("email.force").getOrElse(false)
  lazy val emailPspSubscriptionTemplateId: String = configuration.get[String]("email.pspSubscriptionTemplateId")
  lazy val emailPspDeregistrationTemplateId: String = configuration.get[String]("email.pspDeregistrationTemplateId")
  lazy val emailPspAmendmentTemplateId: String = configuration.get[String]("email.pspAmendmentTemplateId")

  def emailCallback(journeyType: JourneyType.Name, requestId: String, encryptedEmail: String, encryptedPspId: String) =
    s"$pspUrl${configuration.get[String](path = "urls.emailCallback").format(journeyType.toString, requestId, encryptedEmail, encryptedPspId)}"

  lazy val registerWithIdOrganisationUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithIdOrganisation")}"

  lazy val registerWithNoIdOrganisationUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithNoIdOrganisation")}"

  lazy val registerWithNoIdIndividualUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithNoIdIndividual")}"

  lazy val registerWithIdIndividualUrl: String = s"$pspUrl${configuration.get[String]("urls.registration.registerWithIdIndividual")}"

  def identityValidationFrontEndEntry(relativeCompletionURL: RedirectUrl, relativeFailureURL: RedirectUrl): String = {
    val url = loadConfig("urls.iv-uplift-entry")
    val query = s"?origin=pods&confidenceLevel=250&completionURL=${relativeCompletionURL.get(OnlyRelative).url}&failureURL=${relativeFailureURL.get(OnlyRelative).url}"
    url + query
  }

  lazy val enrolmentBase: String = servicesConfig.baseUrl("tax-enrolments")

  lazy val ukJourneyContinueUrl: String = configuration.get[String]("urls.ukJourneyContinue")
  lazy val companiesHouseFileChangesUrl: String = configuration.get[String]("urls.companiesHouseFileChanges")
  lazy val hmrcChangesMustReportUrl: String = configuration.get[String]("urls.hmrcChangesMustReport")
  lazy val hmrcTaxHelplineUrl: String = configuration.get[String]("urls.hmrcTaxHelpline")

  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  lazy val loginContinueUrlRelative: String = configuration.get[String]("urls.loginContinueRelative")

  lazy val loginUrl: String = configuration.get[String]("urls.login")

  lazy val tellHMRCChangesUrl: String = configuration.get[String]("urls.tellHMRCChanges")

  lazy val registerAsPensionAdministratorUrl: String = configuration.get[String]("urls.registerAsPensionAdministrator")

  lazy val createGovGatewayUrl: String = configuration.get[String]("urls.createGovGateway")

  lazy val youMustContactHMRCUrl: String = configuration.get[String]("urls.youMustContactHMRC")
  lazy val returnToPspDashboardUrl: String = configuration.get[String]("urls.pspDashboard")
  lazy val pspListSchemesUrl: String = configuration.get[String]("urls.pspListSchemes")
  lazy val youNeedToRegisterUrl: String = configuration.get[String]("urls.youNeedToRegister")
  lazy val contactHmrcUrl: String = loadConfig("urls.contactHmrcLink")

  lazy val govUkUrl: String = configuration.get[String]("urls.govUK")
  lazy val taxEnrolmentsUrl: String = s"$enrolmentBase${configuration.get[String]("urls.tax-enrolments")}"
  lazy val taxDeEnrolmentUrl: String = s"$enrolmentBase${configuration.get[String]("urls.tax-de-enrolment")}"
  lazy val retryAttempts: Int = configuration.getOptional[Int]("retry.max.attempts").getOrElse(1)
  lazy val retryWaitMs: Int = configuration.getOptional[Int]("retry.initial.wait.ms").getOrElse(1)
  lazy val retryWaitFactor: Double = configuration.getOptional[Double]("retry.wait.factor").getOrElse(1)

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val gtmContainerId: String = configuration.get[String]("tracking-consent-frontend.gtm.container")
  lazy val trackingSnippetUrl: String = configuration.get[String]("tracking-consent-frontend.url")
}
