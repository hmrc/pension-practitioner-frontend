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

package controllers.company

import config.FrontendAppConfig
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import controllers.company.routes.IsCompanyRegisteredInUkController
import forms.address.RegisteredAddressFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.Address
import models.Mode
import models.register.RegistrationLegalStatus
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.company.CompanyRegisteredAddressPage
import pages.company.BusinessNamePage
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

class CompanyEnterRegisteredAddressController @Inject()(override val messagesApi: MessagesApi,
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
            val filledForm = request.userAnswers.get(CompanyRegisteredAddressPage).fold(form)(form.fill)
            val json = commonJson(companyName, mode, filledForm.data.get("country")) ++
              Json.obj("form" -> filledForm)
          renderer.render(viewTemplate, json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        BusinessNamePage.retrieve.right.map { companyName =>
          form.bindFromRequest().fold(
            formWithErrors => {
              val json = commonJson(companyName, mode, formWithErrors.data.get("country")) ++
                Json.obj("form" -> formWithErrors)
              renderer.render(viewTemplate, json).map(BadRequest(_))
            },
            value => {
              println( "\n<<<<" + request.body)
              println( "\n>>>>" + value)
              val updatedUA = request.userAnswers.setOrException(CompanyRegisteredAddressPage, value)
              val nextPage = navigator.nextPage(CompanyRegisteredAddressPage, mode, updatedUA)
              val futureUA =
                if (nextPage == IsCompanyRegisteredInUkController.onPageLoad()) {
                  Future(updatedUA)
                } else {
                  registrationConnector
                    .registerWithNoIdOrganisation(companyName, value, RegistrationLegalStatus.LimitedCompany)
                    .map(updatedUA.setOrException(RegistrationInfoPage, _))
                }
              futureUA
                .flatMap(ua => userAnswersCacheConnector.save(ua.data))
                .map(_ => Redirect(nextPage))
            }
          )
        }
    }

  private def commonJson(companyName: String, mode: Mode, country:Option[String])(implicit request: DataRequest[AnyContent]) = {
    Json.obj(
      "viewmodel" -> CommonViewModel(
        "company",
        companyName,
        routes.CompanyEnterRegisteredAddressController.onSubmit(mode).url),
      "countries" -> jsonCountries(country, config)(request2Messages)
    )
  }
}
