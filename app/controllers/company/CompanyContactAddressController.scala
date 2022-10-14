/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{Mode, Address}
import navigators.CompoundNavigator
import pages.QuestionPage
import pages.company.{CompanyAddressPage, BusinessNamePage, CompanyAddressListPage}
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Call, AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions

import scala.concurrent.ExecutionContext

class CompanyContactAddressController @Inject()(
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

  override protected def addressPage: QuestionPage[Address] = CompanyAddressPage

  override protected val pageTitleEntityTypeMessageKey = Some("company")

  override protected val submitRoute: Mode => Call = mode => routes.CompanyContactAddressController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (AreYouUKCompanyPage and BusinessNamePage).retrieve.map {
        case areYouUKCompany ~ companyName =>
          get(mode, Some(companyName), CompanyAddressListPage, addressConfigurationForPostcodeAndCountry(areYouUKCompany))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (AreYouUKCompanyPage and BusinessNamePage).retrieve.map {
        case areYouUKCompany ~ companyName =>
          post(mode, Some(companyName), addressConfigurationForPostcodeAndCountry(areYouUKCompany))
      }
    }
}
