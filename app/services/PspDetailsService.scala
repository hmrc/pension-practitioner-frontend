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
import models.{CheckMode, PspDetailsData, UserAnswers}
import pages.company.{CompanyAddressPage, CompanyEmailPage, CompanyPhonePage}
import pages.individual._
import pages.partnership._
import pages.register._
import pages.{PspIdPage, RegistrationDetailsPage, SubscriptionTypePage, UnchangedPspDetailsPage, company => comp}
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PspDetailsService @Inject()(
                                   appConfig: FrontendAppConfig,
                                   subscriptionConnector: SubscriptionConnector,
                                   userAnswersCacheConnector: UserAnswersCacheConnector,
                                   minimalConnector: MinimalConnector
                                 ) extends CYAService {

  val halfWidth: String = "govuk-!-width-one-half"
  val thirdWidth: String = "govuk-!-width-one-third"

  private def returnUrlAndLink(name: Option[String], rlsFlag: Boolean)
                              (implicit messages: Messages): JsObject = {
    returnUrlAndLinkData(name, rlsFlag) match {
      case Some((returnUrl, returnLink)) =>
        Json.obj(
          "returnUrl" -> returnUrl,
          "returnLink" -> returnLink
        )
      case None => Json.obj()
    }
  }

  private def returnUrlAndLinkData(name: Option[String], rlsFlag: Boolean)
                              (implicit messages: Messages): Option[(String, String)] = {
    if(rlsFlag) None else {
      Some(appConfig.returnToPspDashboardUrl -> name.fold(messages("site.return_to_dashboard"))(name => messages("site.return_to", name)))
    }
  }

  def getData(userAnswers: Option[UserAnswers], pspId: String)
             (implicit messages: Messages, hc: HeaderCarrier, ec: ExecutionContext): Future[PspDetailsData] =
    getUserAnswers(userAnswers, pspId).flatMap {
      ua =>
        ua.get(RegistrationDetailsPage).map {
          regInfo =>
            minimalConnector.getMinimalPspDetails(pspId).map {
              minDetails =>
                val pspDetailsData = (name: Option[String]) => PspDetailsData(
                  _,
                  _,
                  _,
                  returnUrlAndLinkData(name, minDetails.rlsFlag),
                  displayContinueButton = amendmentsExist(ua),
                  nextPage = routes.DeclarationController.onPageLoad().url
                )
                regInfo.legalStatus match {
                  case Individual =>
                    val title: String = individualMessage("viewDetails.title").value
                    pspDetailsData(ua.get(IndividualDetailsPage).map(_.fullName))(
                      title,
                      ua.get(IndividualDetailsPage).fold(title)(name => heading(name.fullName)),
                      individualDetails(ua, pspId)
                    )
                  case LimitedCompany =>
                    val title: String = companyMessage("viewDetails.title").value
                    pspDetailsData(ua.get(BusinessNamePage))(
                      title,
                      ua.get(comp.BusinessNamePage).fold(title)(name => heading(name)),
                      companyDetails(ua, pspId)
                    )
                  case Partnership =>
                    val title: String = partnershipMessage("viewDetails.title").value
                    pspDetailsData(ua.get(BusinessNamePage))(
                      title,
                      ua.get(BusinessNamePage).fold(title)(name => heading(name)),
                      partnershipDetails(ua, pspId)
                    )
                }
            }
        }.getOrElse(throw new RuntimeException("registration detail user answers not available"))
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
                      (implicit messages: Messages): Option[Actions] =
    if (regInfo.customerType == UK) {
      None
    } else {
      Some(
        Actions(
          items = Seq(ActionItem(
        content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
        href = href.url,
        visuallyHiddenText = Some(Text(Messages("cya.viewDetails.name",name)).value)
      ))))
    }

  private def actions(href: Call, msg: String)
                     (implicit messages: Messages): Some[Actions] =
    Some(
      Actions(
        items = Seq(ActionItem(
      content = HtmlContent(s"""<span aria-hidden="true">${Messages("site.edit")}</span>"""),
      href = href.url,
      visuallyHiddenText = Some(msg)
    ))))

  private def practitionerIdRow(pspId: String)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(SummaryListRow(
      key = Key(Text(Messages("viewDetails.practitioner.id")), halfWidth),
      value = Value(Text(pspId), thirdWidth),
      actions = None
    ))

  private def individualMessage(message: String)(implicit messages: Messages): Text =
    Text(Messages(message, Text(Messages("viewDetails.individual")).value))

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
                               (implicit messages: Messages): Seq[SummaryListRow] =
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
            SummaryListRow(
              key = Key(individualMessage("viewDetails.name"), halfWidth),
              value = Value(Text(name.fullName), thirdWidth),
              actions = nameLink(
                href = indRoutes.IsThisYouController.onPageLoad(CheckMode),
                regInfo = regInfo,
                name = name.fullName
              )
            )
          ) ++
          regDetailsRow(regInfo) ++
          Seq(
            SummaryListRow(
              key = Key(individualMessage("viewDetails.address"), halfWidth),
              value = Value(addressAnswer(address), thirdWidth),
              actions = actions(
                href = indRoutes.IndividualPostcodeController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.address")).value
              )
            ),
            SummaryListRow(
              key = Key(individualMessage("viewDetails.email"), halfWidth),
              value = Value(Text(email), thirdWidth),
              actions = actions(
                href = indRoutes.IndividualEmailController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.email", name.fullName)).value
              )
            ),
            SummaryListRow(
              key = Key(individualMessage("viewDetails.phone"), halfWidth),
              value = Value(Text(phone), thirdWidth),
              actions = actions(
                href = indRoutes.IndividualPhoneController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.phone",name.fullName)).value
              )
            )
          )
      case _ => Seq.empty
    }

  private def regDetailsRow(registrationDetails: RegistrationDetails)(implicit messages: Messages): Seq[SummaryListRow] =
    (
      registrationDetails.idType,
      registrationDetails.idNumber
    ) match {
      case (Some(RegistrationIdType.Nino), Some(nino)) =>
        Seq()
      case (Some(RegistrationIdType.UTR), Some(utr)) =>
        Seq(SummaryListRow(
          key = Key(Text(Messages("viewDetails.practitioner.utr")), halfWidth),
          value = Value(Text(utr), thirdWidth), actions = None)
        )
      case _ => Seq.empty
    }

  private def companyMessage(message: String)(implicit messages: Messages): Text =
    Text(Messages(message, Text(Messages("viewDetails.company")).value))

  private def companyDetails(ua: UserAnswers, pspId: String)
                            (implicit messages: Messages): Seq[SummaryListRow] =
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
            SummaryListRow(
              key = Key(companyMessage("viewDetails.name"), halfWidth),
              value = Value(Text(name), thirdWidth),
              actions = nameLink(
                href = compRoutes.CompanyNameController.onPageLoad(CheckMode),
                regInfo = regInfo,
                name = name
              )
            )
          ) ++
          Seq(
            SummaryListRow(
              key = Key(companyMessage("viewDetails.address"), halfWidth),
              value = Value(addressAnswer(address), thirdWidth),
              actions = actions(
                href = compRoutes.CompanyPostcodeController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.address")).value
              )
            ),
            SummaryListRow(
              key = Key(companyMessage("viewDetails.email"), halfWidth),
              value = Value(Text(email), thirdWidth),
              actions = actions(
                href = compRoutes.CompanyEmailController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.email",name)).value
              )
            ),
            SummaryListRow(
              key = Key(companyMessage("viewDetails.phone"), halfWidth),
              value = Value(Text(phone), thirdWidth),
              actions = actions(
                href = compRoutes.CompanyPhoneController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.phone",name)).value
              )
            )
          )
      case _ => Seq.empty
    }

  private def partnershipMessage(message: String)(implicit messages: Messages): Text =
    Text(Messages(message, Text(Messages("viewDetails.partnership")).value))

  private def partnershipDetails(ua: UserAnswers, pspId: String)
                                (implicit messages: Messages): Seq[SummaryListRow] =
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
            SummaryListRow(
              key = Key(partnershipMessage("viewDetails.name"), halfWidth),
              value = Value(Text(name), thirdWidth),
              actions = nameLink(
                href = partRoutes.PartnershipNameController.onPageLoad(CheckMode),
                regInfo = regInfo,
                name = name
              )
            )
          ) ++
          Seq(
            SummaryListRow(
              key = Key(partnershipMessage("viewDetails.address"), halfWidth),
              value = Value(addressAnswer(address), thirdWidth),
              actions = actions(
                href = partRoutes.PartnershipPostcodeController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.address")).value
              )
            ),
            SummaryListRow(
              key = Key(partnershipMessage("viewDetails.email"), halfWidth),
              value = Value(Text(email), thirdWidth),
              actions = actions(
                href = partRoutes.PartnershipEmailController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.email",name)).value
              )
            ),
            SummaryListRow(
              key = Key(partnershipMessage("viewDetails.phone"), halfWidth),
              value = Value(Text(phone), thirdWidth),
              actions = actions(
                href = partRoutes.PartnershipPhoneController.onPageLoad(CheckMode),
                msg = Text(Messages("cya.change.phone",name)).value
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
