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

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.UKAddressFormProvider
import models.{Address, Mode}
import navigators.CompoundNavigator
import pages.QuestionPage
import pages.partnership.{BusinessNamePage, PartnershipAddressListPage, PartnershipAddressPage}
import pages.register.AreYouUKCompanyPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.address.ManualAddressView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PartnershipContactAddressController @Inject()(
                                                     override val messagesApi: MessagesApi,
                                                     val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                     val navigator: CompoundNavigator,
                                                     authenticate: AuthAction,
                                                     getData: DataRetrievalAction,
                                                     requireData: DataRequiredAction,
                                                     formProvider: UKAddressFormProvider,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     val config: FrontendAppConfig,
                                                     manualAddressView: ManualAddressView
                                                   )(implicit ec: ExecutionContext)
  extends ManualAddressController
    with Retrievals
    with I18nSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override protected def addressPage: QuestionPage[Address] = PartnershipAddressPage
  private val isUkHintText = true
  override protected val pageTitleEntityTypeMessageKey: Option[String] = Some("partnership")

  override protected val submitRoute: Mode => Call = mode => routes.PartnershipContactAddressController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (AreYouUKCompanyPage and BusinessNamePage).retrieve.map {
        case areYouUKCompany ~ companyName =>
          get(mode, Some(companyName), PartnershipAddressListPage, addressConfigurationForPostcodeAndCountry(areYouUKCompany), manualAddressView, isUkHintText)
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (AreYouUKCompanyPage and BusinessNamePage).retrieve.map {
        case areYouUKCompany ~ partnershipName =>
          post(mode, Some(partnershipName), addressConfigurationForPostcodeAndCountry(areYouUKCompany), manualAddressView, isUkHintText)
      }
    }
}
