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

package controllers.individual

import connectors.RegistrationConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.individual.IsThisYouFormProvider
import models.Mode
import models.register.RegistrationIdType.Nino
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.individual.{IndividualAddressPage, IndividualDetailsPage, IsThisYouPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveNoEnrolmentWithIV
import utils.countryOptions.CountryOptions
import viewmodels.Radios
import views.html.individual.IsThisYouView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsThisYouController @Inject()(override val messagesApi: MessagesApi,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    navigator: CompoundNavigator,
                                    @AuthMustHaveNoEnrolmentWithIV authenticate: AuthAction,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    formProvider: IsThisYouFormProvider,
                                    registrationConnector: RegistrationConnector,
                                    countryOptions: CountryOptions,
                                    val controllerComponents: MessagesControllerComponents,
                                    isThisYouView: IsThisYouView
                                   )(implicit val executionContext: ExecutionContext
                                   ) extends FrontendBaseController with I18nSupport with Retrievals {

  private val form: Form[Boolean] = formProvider()


  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val ua = request.userAnswers
      val preparedForm = ua.get(IsThisYouPage).fold(form)(form.fill)

      request.user.nino match {
        case None => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
        case Some(nino) =>
          (ua.get(IndividualDetailsPage), ua.get(IndividualAddressPage), ua.get(RegistrationInfoPage)) match {
            case (Some(individual), Some(address), Some(info)) if info.idType.contains(Nino) && info.idNumber.contains(nino.value) =>
              Future.successful(Ok(isThisYouView(
                routes.IsThisYouController.onSubmit(mode),
                preparedForm,
                Radios.yesNo(preparedForm("value")),
                individual.fullName,
                address.lines(countryOptions)
              )))
            case _ =>
              registrationConnector.registerWithIdIndividual(nino).flatMap { registration =>
                Future.fromTry(ua.set(IndividualDetailsPage, registration.response.individual).flatMap(
                  _.set(IndividualAddressPage, registration.response.address.toPrepopAddress)).flatMap(
                  _.set(RegistrationInfoPage, registration.info)
                )).flatMap { uaWithRegInfo =>
                  userAnswersCacheConnector.save(uaWithRegInfo.data).flatMap { _ =>
                    Future.successful(Ok(isThisYouView(
                      routes.IsThisYouController.onSubmit(mode),
                      preparedForm,
                      Radios.yesNo(preparedForm("value")),
                      registration.response.individual.fullName,
                      registration.response.address.toPrepopAddress.lines(countryOptions)
                    )))
                  }
                }
              }
          }
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          (IndividualDetailsPage and IndividualAddressPage).retrieve.map {
            case individual ~ address =>
              Future.successful(BadRequest(isThisYouView(
                routes.IsThisYouController.onSubmit(mode),
                formWithErrors,
                Radios.yesNo(form("value")),
                individual.fullName,
                address.lines(countryOptions)
              )))
            case _ =>
              Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
          }
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYouPage, value))
            _ <- userAnswersCacheConnector.save(updatedAnswers.data)
          } yield Redirect(navigator.nextPage(IsThisYouPage, mode, updatedAnswers))
      )
  }
}
