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

package controllers.partnership

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.UseAddressForContactFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.Address
import models.NormalMode
import models.TolerantAddress
import models.UserAnswers
import navigators.CompoundNavigator
import pages.company.CompanyAddressPage
import pages.partnership.BusinessNamePage
import pages.partnership.ConfirmAddressPage
import pages.partnership.PartnershipRegisteredAddressPage
import pages.partnership.PartnershipUseSameAddressPage
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

class PartnershipUseSameAddressController @Inject()(override val messagesApi: MessagesApi,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  navigator: CompoundNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: UseAddressForContactFormProvider,
  val controllerComponents: MessagesControllerComponents,
  countryOptions: CountryOptions,
  renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with NunjucksSupport with Retrievals {

  private def form(implicit messages: Messages): Form[Boolean] =
    formProvider(messages("useAddressForContact.error.required", messages("company")))

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(PartnershipUseSameAddressPage).fold(form)(form.fill)
      getJson(preparedForm) { json =>
        renderer.render("address/useAddressForContact.njk", json).map(Ok(_))
      }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          getJson(formWithErrors) { json =>
            renderer.render("address/useAddressForContact.njk", json).map(BadRequest(_))
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

  private def retrieveTolerantAddress(implicit request:DataRequest[_]):Option[TolerantAddress] = {
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
        BusinessNamePage.retrieve.right.map{ companyName =>
          val json = Json.obj(
            "form" -> form,
            "viewmodel" -> CommonViewModel("company", companyName, routes.PartnershipUseSameAddressController.onSubmit().url),
            "radios" -> Radios.yesNo(form("value")),
            "address" -> tolerantAddress.lines(countryOptions)
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
            tolerantAddress.country.getOrElse("GB")
          ))
        }
    }
  }
}
