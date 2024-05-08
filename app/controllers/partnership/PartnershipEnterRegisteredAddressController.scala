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
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.UKAddressFormProvider

import javax.inject.Inject
import models.Address
import models.AddressConfiguration
import models.Mode
import models.register.RegistrationLegalStatus
import navigators.CompoundNavigator
import pages.QuestionPage
import pages.RegistrationInfoPage
import pages.partnership.BusinessNamePage
import pages.partnership.{PartnershipAddressListPage, PartnershipRegisteredAddressPage}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PartnershipEnterRegisteredAddressController @Inject()(override val messagesApi: MessagesApi,
                                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                            val navigator: CompoundNavigator,
                                                            authenticate: AuthAction,
                                                            getData: DataRetrievalAction,
                                                            requireData: DataRequiredAction,
                                                            formProvider: UKAddressFormProvider,
                                                            val controllerComponents: MessagesControllerComponents,
                                                            val config: FrontendAppConfig,
                                                            val renderer: Renderer,
                                                            registrationConnector:RegistrationConnector
)(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override protected def addressPage: QuestionPage[Address] = PartnershipRegisteredAddressPage

  override protected val pageTitleEntityTypeMessageKey: Option[String] = Some("partnership")

  override protected val submitRoute: Mode => Call = mode => routes.PartnershipEnterRegisteredAddressController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      BusinessNamePage.retrieve.map { companyName =>
        get(mode, Some(companyName), PartnershipAddressListPage, AddressConfiguration.CountryOnly)
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      BusinessNamePage.retrieve.map { partnershipName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              renderer.render(viewTemplate,
                json(mode, Some(partnershipName), formWithErrors, AddressConfiguration.CountryOnly)).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(addressPage, value)
              val nextPage = navigator.nextPage(addressPage, mode, updatedUA)
              val futureUA =
                if (nextPage == controllers.partnership.routes.IsPartnershipRegisteredInUkController.onPageLoad()) {
                  Future(updatedUA)
                } else {
                  registrationConnector
                    .registerWithNoIdOrganisation(partnershipName, value, RegistrationLegalStatus.Partnership)
                    .map(updatedUA.setOrException(RegistrationInfoPage, _))
                }
              futureUA
                .flatMap(ua => userAnswersCacheConnector.save(ua.data))
                .map(_ => Redirect(nextPage))
            }
          )
      }
    }
}
