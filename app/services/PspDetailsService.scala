/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.amend.routes
import controllers.company.{routes => compRoutes}
import controllers.individual.{routes => indRoutes}
import controllers.partnership.{routes => partRoutes}
import connectors.{MinimalConnector, SubscriptionConnector}
import connectors.cache.UserAnswersCacheConnector
import models.SubscriptionType.Variation
import models.register.RegistrationCustomerType.{UK, NonUK}
import models.register.RegistrationLegalStatus._
import models.register.{RegistrationIdType, RegistrationDetails}
import models.{CheckMode, UserAnswers}
import pages.company.{CompanyPhonePage, CompanyAddressPage, CompanyEmailPage}
import pages.individual._
import pages.partnership._
import pages.register._
import pages.{SubscriptionTypePage, PspIdPage, RegistrationDetailsPage, company => comp}
import play.api.i18n.Messages
import play.api.libs.json.{Json, JsObject, JsValue}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.viewmodels.SummaryList.{Key, Value, Row, Action}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

class PspDetailsService @Inject()(
                                   appConfig: FrontendAppConfig,
                                   subscriptionConnector: SubscriptionConnector,
                                   userAnswersCacheConnector: UserAnswersCacheConnector,
                                   minimalConnector: MinimalConnector
                                 ) extends CYAService {

  val halfWidth: Seq[String] = Seq("govuk-!-width-one-half")
  val thirdWidth: Seq[String] = Seq("govuk-!-width-one-third")

  def nextPage: String = routes.DeclarationController.onPageLoad().url

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
    getUserAnswers(userAnswers, pspId).flatMap { ua =>
      ua.get(RegistrationDetailsPage).map { regInfo =>
        minimalConnector.getMinimalPspDetails(pspId).map { minDetails =>
          val (json, name) = regInfo.legalStatus match {
            case Individual =>
              val title: String = individualMessage("viewDetails.title").resolve
              val json = Json.obj(
                "pageTitle" -> title,
                "heading" -> ua.get(IndividualDetailsPage).fold(title)(name => heading(name.fullName)),
                "list" -> individualDetails(ua, pspId),
                "nextPage" -> nextPage
              )
              (json, ua.get(IndividualDetailsPage).map(_.fullName))
            case LimitedCompany =>
              val title: String = companyMessage("viewDetails.title").resolve
              val json = Json.obj(
                "pageTitle" -> title,
                "heading" -> ua.get(comp.BusinessNamePage).fold(title)(name => heading(name)),
                "list" -> companyDetails(ua, pspId),
                "nextPage" -> nextPage
              )
              (json, ua.get(BusinessNamePage))
            case Partnership =>
              val title: String = partnershipMessage("viewDetails.title").resolve
              val json = Json.obj(
                "pageTitle" -> title,
                "heading" -> ua.get(BusinessNamePage).fold(title)(name => heading(name)),
                "list" -> partnershipDetails(ua, pspId),
                "nextPage" -> nextPage
              )
              (json, ua.get(BusinessNamePage))
          }

          json ++ returnUrlAndLink(name, minDetails.rlsFlag)
        }
      }.getOrElse(Future.successful(Json.obj()))
    }

  def getUserAnswers(userAnswers: Option[UserAnswers], pspId: String)
                    (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[UserAnswers] =
    userAnswers match {
      case Some(ua) => Future.successful(ua)
      case _ =>
        subscriptionConnector.getSubscriptionDetails(pspId).flatMap { pspDetails =>
          for {
            ua1 <- Future.fromTry(uaWithUkAnswer(uaFromJsValue(pspDetails), pspId))
            ua2 <- Future.fromTry(ua1.set(SubscriptionTypePage, Variation))
            _ <- userAnswersCacheConnector.save(ua2.data)
          } yield ua2
        }
    }


  private def uaWithUkAnswer(userAnswers: UserAnswers, pspId: String): Try[UserAnswers] =
    userAnswers.get(RegistrationDetailsPage).map { regInfo =>
      val ua: UserAnswers = userAnswers.set(PspIdPage, pspId).getOrElse(userAnswers)
      (regInfo.customerType, regInfo.legalStatus) match {
        case (UK, Individual) => ua.set(AreYouUKResidentPage, true)
        case (NonUK, Individual) => ua.set(AreYouUKResidentPage, false)
        case (UK, _) => ua.set(AreYouUKCompanyPage, true)
        case (NonUK, _) => ua.set(AreYouUKCompanyPage, false)
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

  private def addressLink(href: Call)
                         (implicit messages: Messages): Seq[Action] = Seq(Action(
    content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
    href = href.url,
    visuallyHiddenText = Some(msg"cya.change.address")
  ))

  private def emailLink(href: Call, name: String)
                       (implicit messages: Messages): Seq[Action] = Seq(Action(
    content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
    href = href.url,
    visuallyHiddenText = Some(msg"cya.change.email".withArgs(name))
  ))

  private def phoneLink(href: Call, name: String)
                       (implicit messages: Messages): Seq[Action] = Seq(Action(
    content = Html(s"""<span aria-hidden="true">${msg"site.edit".resolve}</span>"""),
    href = href.url,
    visuallyHiddenText = Some(msg"cya.change.phone".withArgs(name))
  ))


  private def practitionerIdRow(pspId: String): Seq[Row] =
    Seq(Row(Key(msg"viewDetails.practitioner.id", halfWidth), Value(Literal(pspId), thirdWidth), Seq.empty))

  private def individualMessage(message: String): Text = msg"$message".withArgs(msg"viewDetails.individual")

  private def individualDetails(ua: UserAnswers, pspId: String)(implicit messages: Messages): Seq[Row] =
    (ua.get(IndividualDetailsPage), ua.get(RegistrationDetailsPage), ua.get(IndividualManualAddressPage),
      ua.get(IndividualEmailPage), ua.get(IndividualPhonePage)) match {
      case (Some(name), Some(regInfo), Some(address), Some(email), Some(phone)) =>
        practitionerIdRow(pspId) ++ Seq(
          Row(Key(individualMessage("viewDetails.name"), halfWidth), Value(Literal(name.fullName), thirdWidth),
            nameLink(indRoutes.IndividualNameController.onPageLoad(CheckMode), regInfo, name.fullName))) ++
          regDetailsRow(regInfo) ++ Seq(
          Row(Key(individualMessage("viewDetails.address"), halfWidth), Value(addressAnswer(address), thirdWidth),
            addressLink(indRoutes.IndividualPostcodeController.onPageLoad(CheckMode))),
          Row(Key(individualMessage("viewDetails.email"), halfWidth), Value(Literal(email), thirdWidth),
            emailLink(indRoutes.IndividualEmailController.onPageLoad(CheckMode), name.fullName)),
          Row(Key(individualMessage("viewDetails.phone"), halfWidth), Value(Literal(phone), thirdWidth),
            phoneLink(indRoutes.IndividualPhoneController.onPageLoad(CheckMode), name.fullName))
        )
      case _ => Seq.empty
    }

  private def regDetailsRow(registrationDetails: RegistrationDetails): Seq[Row] =
    (registrationDetails.idType, registrationDetails.idNumber) match {
      case (Some(RegistrationIdType.Nino), Some(nino)) =>
        Seq(Row(Key(msg"viewDetails.practitioner.nino", halfWidth), Value(Literal(nino), thirdWidth), Seq.empty))
      case (Some(RegistrationIdType.UTR), Some(utr)) =>
        Seq(Row(Key(msg"viewDetails.practitioner.utr", halfWidth), Value(Literal(utr), thirdWidth), Seq.empty))
      case _ => Seq.empty
    }

  private def companyMessage(message: String): Text = msg"$message".withArgs(msg"viewDetails.company")

  private def companyDetails(ua: UserAnswers, pspId: String)
                            (implicit messages: Messages): Seq[Row] =
    (ua.get(comp.BusinessNamePage), ua.get(RegistrationDetailsPage), ua.get(CompanyAddressPage),
      ua.get(CompanyEmailPage), ua.get(CompanyPhonePage)) match {
      case (Some(name), Some(regInfo), Some(address), Some(email), Some(phone)) =>
        practitionerIdRow(pspId) ++ regDetailsRow(regInfo) ++ Seq(
          Row(Key(companyMessage("viewDetails.name"), halfWidth), Value(Literal(name), thirdWidth),
            nameLink(compRoutes.CompanyNameController.onPageLoad(CheckMode), regInfo, name))) ++
          Seq(
            Row(Key(companyMessage("viewDetails.address"), halfWidth), Value(addressAnswer(address), thirdWidth),
              addressLink(compRoutes.CompanyPostcodeController.onPageLoad(CheckMode))),
            Row(Key(companyMessage("viewDetails.email"), halfWidth), Value(Literal(email), thirdWidth),
              emailLink(compRoutes.CompanyEmailController.onPageLoad(CheckMode), name)),
            Row(Key(companyMessage("viewDetails.phone"), halfWidth), Value(Literal(phone), thirdWidth),
              phoneLink(compRoutes.CompanyPhoneController.onPageLoad(CheckMode), name))
          )
      case _ => Seq.empty
    }

  private def partnershipMessage(message: String): Text = msg"$message".withArgs(msg"viewDetails.partnership")

  private def partnershipDetails(ua: UserAnswers, pspId: String)
                                (implicit messages: Messages): Seq[Row] =
    (ua.get(BusinessNamePage), ua.get(RegistrationDetailsPage), ua.get(PartnershipAddressPage),
      ua.get(PartnershipEmailPage), ua.get(PartnershipPhonePage)) match {
      case (Some(name), Some(regInfo), Some(address), Some(email), Some(phone)) =>
        practitionerIdRow(pspId) ++ regDetailsRow(regInfo) ++ Seq(
          Row(Key(partnershipMessage("viewDetails.name"), halfWidth), Value(Literal(name), thirdWidth),
            nameLink(partRoutes.PartnershipNameController.onPageLoad(CheckMode), regInfo, name))) ++
          Seq(
            Row(Key(partnershipMessage("viewDetails.address"), halfWidth), Value(addressAnswer(address), thirdWidth),
              addressLink(partRoutes.PartnershipPostcodeController.onPageLoad(CheckMode))),
            Row(Key(partnershipMessage("viewDetails.email"), halfWidth), Value(Literal(email), thirdWidth),
              emailLink(partRoutes.PartnershipEmailController.onPageLoad(CheckMode), name)),
            Row(Key(partnershipMessage("viewDetails.phone"), halfWidth), Value(Literal(phone), thirdWidth),
              phoneLink(partRoutes.PartnershipPhoneController.onPageLoad(CheckMode), name))
          )
      case _ => Seq.empty
    }


}
