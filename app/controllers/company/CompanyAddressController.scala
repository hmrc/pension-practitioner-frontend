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
import connectors.cache.UserAnswersCacheConnector

import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.AddressFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.Address
import models.Mode
import navigators.CompoundNavigator
import pages.company.CompanyAddressPage
import pages.company.BusinessNamePage
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
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.ExecutionContext

class CompanyAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val userAnswersCacheConnector: UserAnswersCacheConnector,
                                         val navigator: CompoundNavigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: AddressFormProvider,
                                         countryOptions: CountryOptions,
                                         val controllerComponents: MessagesControllerComponents,
                                         val config: FrontendAppConfig,
                                         val renderer: Renderer
                                         )(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.right.map(get)
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        getFormToJson(mode).retrieve.right.map(post(mode, _, CompanyAddressPage))
    }

  def getFormToJson(mode: Mode)(implicit request: DataRequest[AnyContent]): Retrieval[Form[Address] => JsObject] =
    Retrieval(
      implicit request =>
        (AreYouUKCompanyPage and BusinessNamePage).retrieve.right.map {
          case true ~ companyName =>
            form => Json.obj(
              "form" -> form,
              "viewmodel" -> CommonViewModel(
                "company",
                companyName,
                routes.CompanyAddressController.onSubmit(mode).url),
              "countries" -> jsonCountries(form.data.get("country"), config)(request2Messages))
          case false ~ companyName =>
            val optionAddress = request.userAnswers.get(CompanyAddressPage)
            val filledForm = optionAddress.fold(form)(form.fill)
            form =>
              Json.obj(
                "form" -> filledForm,
                "viewmodel" -> CommonViewModel(
                  "company",
                  companyName,
                  routes.CompanyAddressController.onSubmit(mode).url),
                "countries" -> jsonCountries(form.data.get("country"), config)(request2Messages))
        }
    )
}
