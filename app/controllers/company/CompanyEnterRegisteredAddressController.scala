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
import models.AddressLocation
import models.AddressLocation.AddressLocation
import models.Mode
import models.register.RegistrationLegalStatus
import navigators.CompoundNavigator
import pages.QuestionPage
import pages.RegistrationInfoPage
import pages.company.CompanyRegisteredAddressPage
import pages.company.BusinessNamePage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport

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

  override protected def addressPage: QuestionPage[Address] = CompanyRegisteredAddressPage

  override protected val pageTitleEntityTypeMessageKey = Some("company")

  override protected val submitRoute: Mode => Call = mode => routes.CompanyEnterRegisteredAddressController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      BusinessNamePage.retrieve.right.map { companyName =>
          get(mode, companyName, AddressLocation.CountryOnly)
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      BusinessNamePage.retrieve.right.map { companyName =>
          post(mode, companyName, AddressLocation.CountryOnly)
      }
    }

  override protected def post(mode: Mode,
    name: String,
    addressLocation: AddressLocation)(
    implicit request: DataRequest[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val json = commonJson(mode, name, formWithErrors, addressLocation)
          renderer.render(viewTemplate, json).map(Ok(_))
        },
        value => {
            val updatedUA = request.userAnswers.setOrException(addressPage, value)
            val nextPage = navigator.nextPage(addressPage, mode, updatedUA)
            val futureUA =
              if (nextPage == IsCompanyRegisteredInUkController.onPageLoad()) {
                Future(updatedUA)
              } else {
                registrationConnector
                  .registerWithNoIdOrganisation(name, value, RegistrationLegalStatus.LimitedCompany)
                  .map(updatedUA.setOrException(RegistrationInfoPage, _))
              }
            futureUA
              .flatMap(ua => userAnswersCacheConnector.save(ua.data))
              .map(_ => Redirect(nextPage))
            }
      )
  }
}
