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

package controllers.partnership

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.UseAddressForContactFormProvider
import models.{Address, NormalMode, TolerantAddress, UserAnswers}
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.company.CompanyAddressPage
import pages.partnership.{BusinessNamePage, ConfirmAddressPage, PartnershipRegisteredAddressPage, PartnershipUseSameAddressPage}
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import utils.countryOptions.CountryOptions
import viewmodels.{CommonViewModel, Radios}
import views.html.address.UseAddressForContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PartnershipUseSameAddressController @Inject()(override val messagesApi: MessagesApi,
                                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                                    navigator: CompoundNavigator,
                                                    authenticate: AuthAction,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    formProvider: UseAddressForContactFormProvider,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    countryOptions: CountryOptions,
                                                    useAddressForContactView: UseAddressForContactView,
                                                    twirlMigration: TwirlMigration
                                                   )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with NunjucksSupport with Retrievals {

  private def form(implicit messages: Messages): Form[Boolean] =
    formProvider(messages("useAddressForContact.error.required", messages("company")))

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(PartnershipUseSameAddressPage).fold(form)(form.fill)
      getJson(preparedForm) { json =>
        Future.successful(Ok(useAddressForContactView(
          routes.PartnershipUseSameAddressController.onSubmit(),
          preparedForm,
          Radios.yesNo(preparedForm("value")),
          (json \ "viewmodel" \ "entityType").asOpt[String].getOrElse(""),
          (json \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
          (json \ "address").asOpt[Seq[String]].getOrElse(Seq.empty[String])
        )))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          getJson(formWithErrors) { json =>
            Future.successful(BadRequest(useAddressForContactView(
              routes.PartnershipUseSameAddressController.onSubmit(),
              formWithErrors,
              Radios.yesNo(formWithErrors("value")),
              (json \ "viewmodel" \ "entityType").asOpt[String].getOrElse(""),
              (json \ "viewmodel" \ "entityName").asOpt[String].getOrElse(""),
              (json \ "address").asOpt[Seq[String]].getOrElse(Seq.empty[String]))))
          }
        },
        value => {
          retrieveTolerantAddress match {
            case Some(address) =>
              val updatedUserAnswersTry: Try[UserAnswers] =
                if (value) {
                  getResolvedAddress(address) match {
                    case None =>
                      request.userAnswers.set(PartnershipUseSameAddressPage, false)
                    case Some(resolvedAddress) =>
                      request.userAnswers.set(PartnershipUseSameAddressPage, value)
                        .flatMap(_.set(CompanyAddressPage, resolvedAddress))
                  }
                } else {
                  request.userAnswers.set(PartnershipUseSameAddressPage, value)
                }
              for {
                updatedAnswers <- Future.fromTry(updatedUserAnswersTry)
                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
              } yield Redirect(navigator.nextPage(PartnershipUseSameAddressPage, NormalMode, updatedAnswers))
            case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
        }
      )
  }

  private def retrieveTolerantAddress(implicit request: DataRequest[_]): Option[TolerantAddress] = {
    (request.userAnswers.get(AreYouUKCompanyPage),
      request.userAnswers.get(ConfirmAddressPage),
      request.userAnswers.get(PartnershipRegisteredAddressPage)) match {
      case (Some(true), Some(address), _) =>
        Some(address)
      case (Some(false), _, Some(address)) =>
        Some(address.toTolerantAddress)
      case _ => None
    }
  }

  private def getJson(form: Form[Boolean])(block: JsObject => Future[Result])(implicit request: DataRequest[AnyContent]): Future[Result] = {
    retrieveTolerantAddress match {
      case Some(tolerantAddress) =>
        BusinessNamePage.retrieve.map { companyName =>
          val json = Json.obj(
            "form" -> form,
            "viewmodel" -> CommonViewModel("partnership", companyName, routes.PartnershipUseSameAddressController.onSubmit().url),
            "radios" -> Radios.yesNo(form("value")),
            "address" -> tolerantAddress.lines(countryOptions),
            "entity" -> "Partnership"
          )
          block(json)
        }
      case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    }
  }

  protected def getResolvedAddress(tolerantAddress: TolerantAddress): Option[Address] = {
    tolerantAddress.addressLine1 match {
      case None => None
      case Some(aLine1) =>
        tolerantAddress.addressLine2 match {
          case None => None
          case Some(aLine2) => Some(Address(
            aLine1,
            aLine2,
            tolerantAddress.addressLine3,
            tolerantAddress.addressLine4,
            tolerantAddress.postcode,
            tolerantAddress.countryOpt.getOrElse("GB")
          ))
        }
    }
  }
}
