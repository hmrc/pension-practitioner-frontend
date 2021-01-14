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

import scala.concurrent.ExecutionContext
import play.api.data.Form
import connectors.cache.UserAnswersCacheConnector
import config.FrontendAppConfig
import javax.inject.Inject
import controllers.address.ManualAddressController
import navigators.CompoundNavigator
import forms.address.AddressFormProvider
import models.{Mode, Address}
import utils.countryOptions.CountryOptions
import controllers.Retrievals
import renderer.Renderer
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import controllers.actions.{DataRequiredAction, AuthAction, DataRetrievalAction}
import pages.QuestionPage
import pages.individual.{AreYouUKResidentPage, IndividualManualAddressPage}
import play.api.mvc.{Call, AnyContent, MessagesControllerComponents, Action}
import uk.gov.hmrc.viewmodels.NunjucksSupport


class IndividualContactAddressController @Inject()(
  override val messagesApi: MessagesApi,
  val userAnswersCacheConnector: UserAnswersCacheConnector,
  val navigator: CompoundNavigator,
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AddressFormProvider,
  countryOptions: CountryOptions,
  val controllerComponents: MessagesControllerComponents,
  val config: FrontendAppConfig,
  val renderer: Renderer
)(implicit ec: ExecutionContext)
  extends ManualAddressController
    with Retrievals
    with I18nSupport
    with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override protected def addressPage: QuestionPage[Address] = IndividualManualAddressPage

  override protected val pageTitleMessageKey: String = "individual.address.title"
  override protected val h1MessageKey: String = "individual.address.title"

  override protected val submitRoute: Mode => Call = mode => routes.IndividualContactAddressController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      AreYouUKResidentPage.retrieve.right.map { areYouUKResident =>
          get(mode, None, addressConfigurationForPostcodeAndCountry(areYouUKResident))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      AreYouUKResidentPage.retrieve.right.map { areYouUKResident =>
          post(mode, None, addressConfigurationForPostcodeAndCountry(areYouUKResident))
      }
    }
}
