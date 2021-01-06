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

package controllers.individual

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.UseAddressForContactFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.{Mode, NormalMode, TolerantAddress, Address}
import navigators.CompoundNavigator
import pages.individual.{IndividualAddressPage, IndividualManualAddressPage, UseAddressForContactPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{Result, AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.annotations.AuthMustHaveNoEnrolmentWithIV
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.{Future, ExecutionContext}

class UseAddressForContactController @Inject()(override val messagesApi: MessagesApi,
                                               userAnswersCacheConnector: UserAnswersCacheConnector,
                                               navigator: CompoundNavigator,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: UseAddressForContactFormProvider,
                                               val controllerComponents: MessagesControllerComponents,
                                               config: FrontendAppConfig,
                                               countryOptions: CountryOptions,
                                               renderer: Renderer
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with NunjucksSupport with Retrievals {

  private def form(implicit messages: Messages): Form[Boolean] =
    formProvider(messages("useAddressForContact.error.required", messages("individual.you")))

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(UseAddressForContactPage).fold(form)(form.fill)
      getJson(preparedForm) { json =>
        renderer.render("address/useAddressForContact.njk", json).map(Ok(_))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          getJson(formWithErrors) { json =>
            renderer.render("address/useAddressForContact.njk", json).map(BadRequest(_))
          }
        },
        value => {
          IndividualAddressPage.retrieve.right.map { address =>
            val ua = request.userAnswers.setOrException(UseAddressForContactPage, value)
            val updatedAnswers = if (value) {
              ua.setOrException(IndividualManualAddressPage, address)
            } else {
              ua
            }
            userAnswersCacheConnector.save(updatedAnswers.data).map { _ =>
              Redirect(navigator.nextPage(UseAddressForContactPage, NormalMode, updatedAnswers))
            }
          }
        }
      )
  }

  private def getJson(form: Form[Boolean])(block: JsObject => Future[Result])
                     (implicit request: DataRequest[AnyContent]): Future[Result] =
    IndividualAddressPage.retrieve.right.map { address =>
      val json = Json.obj(
        "form" -> form,
        "viewmodel" -> CommonViewModel("individual.you", "individual.you", routes.UseAddressForContactController.onSubmit().url),
        "radios" -> Radios.yesNo(form("value")),
        "address" -> address.lines(countryOptions)
      )
      block(json)
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
