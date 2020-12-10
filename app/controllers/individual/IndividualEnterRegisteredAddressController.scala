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
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.NonUKManualAddressController
import forms.address.NonUKAddressFormProvider
import javax.inject.Inject
import models.register.RegistrationInfo
import models.requests.DataRequest
import models.{Mode, Address}
import navigators.CompoundNavigator
import pages.individual.{IndividualDetailsPage, IndividualAddressPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.annotations.AuthWithIVNoEnrolment
import utils.countryOptions.CountryOptions

import scala.concurrent.{Future, ExecutionContext}

class IndividualEnterRegisteredAddressController @Inject()(override val messagesApi: MessagesApi,
                                                 val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                 val navigator: CompoundNavigator,
                                                 authenticate: AuthAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 formProvider: NonUKAddressFormProvider,
                                                 registrationConnector: RegistrationConnector,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 val config: FrontendAppConfig,
                                                 val renderer: Renderer,
                                                 val countryOptions: CountryOptions
                                                )(implicit ec: ExecutionContext) extends NonUKManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override def viewTemplate: String = "individual/nonUKAddress.njk"

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        get(getFormToJson(mode), IndividualAddressPage)
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        IndividualDetailsPage.retrieve.right.map { individual =>
          (individual.firstName, individual.lastName) match {
            case (Some(firstName), Some(lastName)) =>
              val regCall: Address => Future[RegistrationInfo] = address =>
                registrationConnector.registerWithNoIdIndividual(firstName, lastName, address)
              post(mode, getFormToJson(mode), IndividualAddressPage, regCall)
            case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
        }
    }

  private def getFormToJson(mode: Mode)(implicit request: DataRequest[AnyContent]): Form[Address] => JsObject = {
    form =>
      Json.obj(
        "form" -> form,
        "submitUrl" -> routes.IndividualEnterRegisteredAddressController.onSubmit(mode).url,
        "countries" -> jsonCountries(form.data.get("country"), config)(request2Messages)
      )
  }
}
