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

package services

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{MinimalConnector, SubscriptionConnector}
import controllers.amend.routes
import controllers.company.{routes => compRoutes}
import controllers.individual.{routes => indRoutes}
import controllers.partnership.{routes => partRoutes}
import models.SubscriptionType.Variation
import models.register.RegistrationCustomerType.{NonUK, UK}
import models.register.RegistrationLegalStatus._
import models.register.{RegistrationDetails, RegistrationIdType}
import models.{CheckMode, UserAnswers}
import pages.company.{CompanyAddressPage, CompanyEmailPage, CompanyPhonePage}
import pages.individual._
import pages.partnership._
import pages.register._
import pages.{PspIdPage, RegistrationDetailsPage, SubscriptionTypePage, UnchangedPspDetailsPage, company => comp}
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.{Literal, Message}
import uk.gov.hmrc.viewmodels._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PspDetailsService @Inject()(
                                   appConfig: FrontendAppConfig,
                                   subscriptionConnector: SubscriptionConnector,
                                   userAnswersCacheConnector: UserAnswersCacheConnector,
                                   minimalConnector: MinimalConnector
                                 ) extends CYAService {

  val halfWidth: Seq[String] = Seq("govuk-!-width-one-half")
  val thirdWidth: Seq[String] = Seq("govuk-!-width-one-third")

  private def returnUrlAndLink(name: Option[String], rlsFlag: Boolean)
                              (implicit messages: Messages): JsObject = {
    if (rlsFlag) {
      Json.obj()
    } else {
      Json.obj(
        "returnUrl" -> appConfig.returnToPspDashboardUrl,
        "returnLink" -> name.fold(messages("site.return_to_dashboard"))(name => messages("site.return_to", name))
      )
    }
  }

  def getJson(userAnswers: Option[UserAnswers], pspId: String)
             (implicit messages: Messages, hc: HeaderCarrier, ec: ExecutionContext): Future[JsObject] =
    getUserAnswers(userAnswers, pspId).flatMap {
      ua =>
        ua.get(RegistrationDetailsPage).map {
          regInfo =>
            minimalConnector.getMinimalPspDetails(pspId).map {
              minDetails =>
                val (json, name) = regInfo.legalStatus match {
                  case Individual =>
                    val title: String = individualMessage("viewDetails.title").resolve
                    val json = Json.obj(
                      "pageTitle" -> title,
                      "heading" -> ua.get(IndividualDetailsPage).fold(title)(name => heading(name.fullName)),
                      "list" -> individualDetails(ua, pspId)
                    )
                    (json, ua.get(IndividualDetailsPage).map(_.fullName))
                  case LimitedCompany =>
                    val title: String = companyMessage("viewDetails.title").resolve
                    val json = Json.obj(
                      "pageTitle" -> title,
                      "heading" -> ua.get(comp.BusinessNamePage).fold(title)(name => heading(name)),
                      "list" -> companyDetails(ua, pspId)
                    )
                    (json, ua.get(BusinessNamePage))
                  case Partnership =>
                    val title: String = partnershipMessage("viewDetails.title").resolve
                    val json = Json.obj(
                      "pageTitle" -> title,
                      "heading" -> ua.get(BusinessNamePage).fold(title)(name => heading(name)),
                      "list" -> partnershipDetails(ua, pspId)
                    )
                    (json, ua.get(BusinessNamePage))
                }

                json ++ returnUrlAndLink(name, minDetails.rlsFlag) ++ Json.obj(
                  "displayContinueButton" -> amendmentsExist(ua),
                "nextPage" -> routes.DeclarationController.onPageLoad().url)
            }
        }.getOrElse(Future.successful(Json.obj()))
    }

  def getUserAnswers(userAnswers: Option[UserAnswers], pspId: String)
                    (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[UserAnswers] =
    userAnswers match {
      case Some(ua) if ua.get(UnchangedPspDetailsPage).nonEmpty =>
        Future.successful(ua)
      case _ =>
        for {
          pspDetails <- subscriptionConnector.getSubscriptionDetails(pspId)
          ua1 <- Future.fromTry(uaWithUkAnswer(uaFromJsValue(pspDetails), pspId))
          ua2 <- Future.fromTry(ua1.set(SubscriptionTypePage, Variation).flatMap(_.set(UnchangedPspDetailsPage, pspDetails)))
          _ <- userAnswersCacheConnector.save(ua2.data)
        } yield ua2
    }


  private def uaWithUkAnswer(userAnswers: UserAnswers, pspId: String): Try[UserAnswers] =
    userAnswers.get(RegistrationDetailsPage).map {
      val ua: UserAnswers = userAnswers.set(PspIdPage, pspId).getOrElse(userAnswers)
      regInfo =>
        (
          regInfo.customerType,
          regInfo.legalStatus
        ) match {
          case (UK, Individual) =>
            ua.set(AreYouUKResidentPage, true)
          case (NonUK, Individual) =>
            ua.set(AreYouUKResidentPage, false)
          case (UK, _) =>
            ua.set(AreYouUKCompanyPage, true)
          case (NonUK, _) =>
            ua.set(AreYouUKCompanyPage, false)
        }
    }.getOrElse(Try(userAnswers))

  private def uaFromJsValue(jsValue: JsValue): UserAnswers = UserAnswers(jsValue.as[JsObject])

  private def heading(name: String)
                     (implicit messages: Messages): String = messages("viewDetails.heading", name)

  private def nameLink(href: Call, regInfo: RegistrationDetails, name: String)
                      (implicit messages: Messages): Seq[Action] =
    if (regInfo.customerType == UK) {
      Seq.empty
    } else {
      Seq(Action(
        content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
        href = href.url,
        visuallyHiddenText = Some(msg"cya.viewDetails.name".withArgs(name))
      ))
    }

  private def actions(href: Call, msg: Message)
                     (implicit messages: Messages): Seq[Action] =
    Seq(Action(
      content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
      href = href.url,
      visuallyHiddenText = Some(msg)
    ))

  private def practitionerIdRow(pspId: String): Seq[Row] =
    Seq(Row(
      key = Key(msg"viewDetails.practitioner.id", halfWidth),
      value = Value(Literal(pspId), thirdWidth),
      actions = Seq.empty
    ))

  private def individualMessage(message: String): Text = msg"$message".withArgs(msg"viewDetails.individual")

  def amendmentsExist(ua: UserAnswers): Boolean = {
    val originalPspDetails = ua.get(UnchangedPspDetailsPage).getOrElse(Json.obj())
    val mismatches = ua.data.keys.filter(key => (originalPspDetails \ key) != (ua.data \ key))
    if (mismatches.nonEmpty && ua.get(UnchangedPspDetailsPage).nonEmpty) {
      ua.get(RegistrationDetailsPage).exists(_.legalStatus match {
        case Individual => mismatches.exists(List("individualDetails", "contactAddress", "email", "phone").contains)
        case _ => mismatches.exists(List("name", "contactAddress", "email", "phone").contains)
      })
    } else {
      false
    }
  }

  private def individualDetails(ua: UserAnswers, pspId: String)
                               (implicit messages: Messages): Seq[Row] =
    (
      ua.get(IndividualDetailsPage),
      ua.get(RegistrationDetailsPage),
      ua.get(IndividualManualAddressPage),
      ua.get(IndividualEmailPage),
      ua.get(IndividualPhonePage)
    ) match {
      case (Some(name), Some(regInfo), Some(address), Some(email), Some(phone)) =>
        practitionerIdRow(pspId) ++
          Seq(
            Row(
              key = Key(individualMessage("viewDetails.name"), halfWidth),
              value = Value(Literal(name.fullName), thirdWidth),
              actions = nameLink(
                href = indRoutes.IndividualNameController.onPageLoad(CheckMode),
                regInfo = regInfo,
                name = name.fullName
              )
            )
          ) ++
          regDetailsRow(regInfo) ++
          Seq(
            Row(
              key = Key(individualMessage("viewDetails.address"), halfWidth),
              value = Value(addressAnswer(address), thirdWidth),
              actions = actions(
                href = indRoutes.IndividualPostcodeController.onPageLoad(CheckMode),
                msg = msg"cya.change.address"
              )
            ),
            Row(
              key = Key(individualMessage("viewDetails.email"), halfWidth),
              value = Value(Literal(email), thirdWidth),
              actions = actions(
                href = indRoutes.IndividualEmailController.onPageLoad(CheckMode),
                msg = msg"cya.change.email".withArgs(name.fullName)
              )
            ),
            Row(
              key = Key(individualMessage("viewDetails.phone"), halfWidth),
              value = Value(Literal(phone), thirdWidth),
              actions = actions(
                href = indRoutes.IndividualPhoneController.onPageLoad(CheckMode),
                msg = msg"cya.change.phone".withArgs(name.fullName)
              )
            )
          )
      case _ => Seq.empty
    }

  private def regDetailsRow(registrationDetails: RegistrationDetails): Seq[Row] =
    (
      registrationDetails.idType,
      registrationDetails.idNumber
    ) match {
      case (Some(RegistrationIdType.Nino), Some(nino)) =>
        Seq()
      case (Some(RegistrationIdType.UTR), Some(utr)) =>
        Seq(Row(
          key = Key(msg"viewDetails.practitioner.utr", halfWidth),
          value = Value(Literal(utr), thirdWidth), actions = Seq.empty)
        )
      case _ => Seq.empty
    }

  private def companyMessage(message: String): Text = msg"$message".withArgs(msg"viewDetails.company")

  private def companyDetails(ua: UserAnswers, pspId: String)
                            (implicit messages: Messages): Seq[Row] =
    (
      ua.get(comp.BusinessNamePage),
      ua.get(RegistrationDetailsPage),
      ua.get(CompanyAddressPage),
      ua.get(CompanyEmailPage),
      ua.get(CompanyPhonePage)
    ) match {
      case (Some(name), Some(regInfo), Some(address), Some(email), Some(phone)) =>
        practitionerIdRow(pspId) ++
          regDetailsRow(regInfo) ++
          Seq(
            Row(
              key = Key(companyMessage("viewDetails.name"), halfWidth),
              value = Value(Literal(name), thirdWidth),
              actions = nameLink(
                href = compRoutes.CompanyNameController.onPageLoad(CheckMode),
                regInfo = regInfo,
                name = name
              )
            )
          ) ++
          Seq(
            Row(
              key = Key(companyMessage("viewDetails.address"), halfWidth),
              value = Value(addressAnswer(address), thirdWidth),
              actions = actions(
                href = compRoutes.CompanyPostcodeController.onPageLoad(CheckMode),
                msg = msg"cya.change.address"
              )
            ),
            Row(
              key = Key(companyMessage("viewDetails.email"), halfWidth),
              value = Value(Literal(email), thirdWidth),
              actions = actions(
                href = compRoutes.CompanyEmailController.onPageLoad(CheckMode),
                msg = msg"cya.change.email".withArgs(name)
              )
            ),
            Row(
              key = Key(companyMessage("viewDetails.phone"), halfWidth),
              value = Value(Literal(phone), thirdWidth),
              actions = actions(
                href = compRoutes.CompanyPhoneController.onPageLoad(CheckMode),
                msg = msg"cya.change.phone".withArgs(name)
              )
            )
          )
      case _ => Seq.empty
    }

  private def partnershipMessage(message: String): Text = msg"$message".withArgs(msg"viewDetails.partnership")

  private def partnershipDetails(ua: UserAnswers, pspId: String)
                                (implicit messages: Messages): Seq[Row] =
    (
      ua.get(BusinessNamePage),
      ua.get(RegistrationDetailsPage),
      ua.get(PartnershipAddressPage),
      ua.get(PartnershipEmailPage),
      ua.get(PartnershipPhonePage)
    ) match {
      case (Some(name), Some(regInfo), Some(address), Some(email), Some(phone)) =>
        practitionerIdRow(pspId) ++
          regDetailsRow(regInfo) ++
          Seq(
            Row(
              key = Key(partnershipMessage("viewDetails.name"), halfWidth),
              value = Value(Literal(name), thirdWidth),
              actions = nameLink(
                href = partRoutes.PartnershipNameController.onPageLoad(CheckMode),
                regInfo = regInfo,
                name = name
              )
            )
          ) ++
          Seq(
            Row(
              key = Key(partnershipMessage("viewDetails.address"), halfWidth),
              value = Value(addressAnswer(address), thirdWidth),
              actions = actions(
                href = partRoutes.PartnershipPostcodeController.onPageLoad(CheckMode),
                msg = msg"cya.change.address"
              )
            ),
            Row(
              key = Key(partnershipMessage("viewDetails.email"), halfWidth),
              value = Value(Literal(email), thirdWidth),
              actions = actions(
                href = partRoutes.PartnershipEmailController.onPageLoad(CheckMode),
                msg = msg"cya.change.email".withArgs(name)
              )
            ),
            Row(
              key = Key(partnershipMessage("viewDetails.phone"), halfWidth),
              value = Value(Literal(phone), thirdWidth),
              actions = actions(
                href = partRoutes.PartnershipPhoneController.onPageLoad(CheckMode),
                msg = msg"cya.change.phone".withArgs(name)
              )
            )
          )
      case _ => Seq.empty
    }

  def getPspName(ua: UserAnswers): Option[String] = ua.get(RegistrationDetailsPage).flatMap {
    regInfo =>
      regInfo.legalStatus match {
        case Individual =>
          ua.get(IndividualDetailsPage).map(_.fullName)
        case LimitedCompany =>
          ua.get(comp.BusinessNamePage)
        case Partnership =>
          ua.get(BusinessNamePage)
      }
  }
}
