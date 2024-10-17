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

package controllers.company

import config.FrontendAppConfig
import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import controllers.company.routes.IsCompanyRegisteredInUkController
import forms.address.UKAddressFormProvider
import models.register.RegistrationLegalStatus
import models.{Address, AddressConfiguration, Mode}
import navigators.CompoundNavigator
import pages.company.{BusinessNamePage, CompanyAddressListPage, CompanyRegisteredAddressPage}
import pages.{QuestionPage, RegistrationInfoPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.address.ManualAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyEnterRegisteredAddressController @Inject()(override val messagesApi: MessagesApi,
                                                        val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                        val navigator: CompoundNavigator,
                                                        authenticate: AuthAction,
                                                        getData: DataRetrievalAction,
                                                        requireData: DataRequiredAction,
                                                        formProvider: UKAddressFormProvider,
                                                        val controllerComponents: MessagesControllerComponents,
                                                        val config: FrontendAppConfig,
                                                        registrationConnector:RegistrationConnector,
                                                        manualAddressView: ManualAddressView
)(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override protected def addressPage: QuestionPage[Address] = CompanyRegisteredAddressPage

  override protected val pageTitleEntityTypeMessageKey: Option[String] = Some("company")

  override protected val submitRoute: Mode => Call = mode => routes.CompanyEnterRegisteredAddressController.onSubmit(mode)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      BusinessNamePage.retrieve.map { companyName =>
          get(mode, Some(companyName), CompanyAddressListPage, AddressConfiguration.CountryOnly, manualAddressView)
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      BusinessNamePage.retrieve.map { companyName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val jsonValue = json(mode, Some(companyName), formWithErrors, AddressConfiguration.CountryOnly)
              Future.successful(BadRequest(manualAddressView(
                (jsonValue \ "pageTitle").asOpt[String].getOrElse(""),
                (jsonValue \ "h1").asOpt[String].getOrElse(""),
                (jsonValue \ "postcodeEntry").asOpt[Boolean].getOrElse(false),
                (jsonValue \ "postcodeFirst").asOpt[Boolean].getOrElse(false),
                (jsonValue \ "countries").asOpt[Array[models.Country]].getOrElse(Array.empty[models.Country]),
                submitRoute(mode),
                formWithErrors)))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(addressPage, value)
              val nextPage = navigator.nextPage(addressPage, mode, updatedUA)
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
}
