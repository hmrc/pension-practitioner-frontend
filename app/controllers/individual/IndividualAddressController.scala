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

package controllers.individual

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.AddressFormProvider
import javax.inject.Inject
import models.requests.DataRequest
import models.{Address, Mode}
import navigators.CompoundNavigator
import pages.individual.IndividualManualAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.ExecutionContext

class IndividualAddressController @Inject()(override val messagesApi: MessagesApi,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val navigator: CompoundNavigator,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: AddressFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            val config: FrontendAppConfig,
                                            val renderer: Renderer
                                           )(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override def viewTemplate: String = "individual/manualAddress.njk"

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        get(getFormToJson(mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>
        post(mode, getFormToJson(mode), IndividualManualAddressPage)
    }

  private def getFormToJson(mode: Mode)(implicit request: DataRequest[AnyContent]): Form[Address] => JsObject = {
        println("\n\n\n countruses: "+jsonCountries(form.data.get("country"), config)(request2Messages))
    form =>
      Json.obj(
        "form" -> form,
        "submitUrl" -> routes.IndividualAddressController.onSubmit(mode).url,
        "countries" -> jsonCountries(form.data.get("country"), config)(request2Messages)

      )
  }
}
