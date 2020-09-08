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

import config.FrontendAppConfig
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.RegisteredAddressFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.Address
import models.Mode
import models.register.RegistrationLegalStatus
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.partnership.BusinessNamePage
import pages.partnership.PartnershipRegisteredAddressPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PartnershipEnterRegisteredAddressController @Inject()(override val messagesApi: MessagesApi,
  val userAnswersCacheConnector: UserAnswersCacheConnector,
  val navigator: CompoundNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RegisteredAddressFormProvider,
  val controllerComponents: MessagesControllerComponents,
  val config: FrontendAppConfig,
  val renderer: Renderer,
  registrationConnector:RegistrationConnector
)(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        BusinessNamePage.retrieve.right.map { companyName =>
          val filledForm = request.userAnswers.get(PartnershipRegisteredAddressPage).fold(form)(form.fill)
          val json = commonJson( mode, companyName, filledForm)
          renderer.render(viewTemplate, json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        BusinessNamePage.retrieve.right.map { companyName =>
          form.bindFromRequest().fold(
            formWithErrors => {
              val json = commonJson(mode, companyName, formWithErrors)
              renderer.render(viewTemplate, json).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(PartnershipRegisteredAddressPage, value)
              val nextPage = navigator.nextPage(PartnershipRegisteredAddressPage, mode, updatedUA)
              val futureUA =
                if (nextPage == controllers.partnership.routes.IsPartnershipRegisteredInUkController.onPageLoad()) {
                  Future(updatedUA)
                } else {
                  registrationConnector
                    .registerWithNoIdOrganisation(companyName, value, RegistrationLegalStatus.Partnership)
                    .map(updatedUA.setOrException(RegistrationInfoPage, _))
                }
              futureUA
                .flatMap(ua => userAnswersCacheConnector.save(ua.data))
                .map(_ => Redirect(nextPage))
            }
          )
        }
    }

  private def commonJson(
    mode: Mode,
    companyName: String,
    form: Form[Address]
  )(implicit request: DataRequest[AnyContent]): JsObject = {
    val messages = request2Messages

    val pageTitle = messages("address.title", companyName)
    val h1 = messages("address.title", companyName)

    Json.obj(
      "submitUrl" -> routes.PartnershipEnterRegisteredAddressController.onSubmit(mode).url,
      "form" -> form,
      "countries" -> jsonCountries(form.data.get("country"), config)(messages),
      "pageTitle" -> pageTitle,
      "h1" -> h1
    )
  }
}
