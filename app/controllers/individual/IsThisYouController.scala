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
import controllers.actions._
import forms.individual.IsThisYouFormProvider
import javax.inject.Inject
import models.Mode
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.individual.{IndividualAddressPage, IndividualDetailsPage, IsThisYouPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}

import scala.concurrent.{ExecutionContext, Future}

class IsThisYouController @Inject()(override val messagesApi: MessagesApi,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    navigator: CompoundNavigator,
                                    identify: IdentifierAction,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    formProvider: IsThisYouFormProvider,
                                    registrationConnector: RegistrationConnector,
                                    val controllerComponents: MessagesControllerComponents,
                                    config: FrontendAppConfig,
                                    renderer: Renderer
                                   )(implicit val executionContext: ExecutionContext
                                   ) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val ua = request.userAnswers
      val preparedForm = ua.get(IsThisYouPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      val json = Json.obj(
        "form" -> preparedForm,
        "submitUrl" -> routes.AreYouUKResidentController.onSubmit().url,
        "radios" -> Radios.yesNo(preparedForm("value"))
      )
      (ua.get(IndividualDetailsPage), ua.get(IndividualAddressPage), ua.get(RegistrationInfoPage)) match {
        case (Some(individual), Some(address), Some(_)) =>
          renderer.render("individual/isThisYou.njk", json).map(Ok(_))
        case _ =>
          request.user.nino match {
            case Some(nino) =>
              (for {
                registration <- registrationConnector.registerWithIdIndividual(nino)
                uaWithIndvDetails <- Future.fromTry(ua.set(IndividualDetailsPage, registration.response.individual))
                uaWithIndvAddress <- Future.fromTry(uaWithIndvDetails.set(IndividualAddressPage, registration.response.address))
                uaWithIndvRegInfo <- Future.fromTry(uaWithIndvAddress.set(RegistrationInfoPage, registration.info))
                _ <- userAnswersCacheConnector.save(uaWithIndvRegInfo.data)
              } yield {
                renderer.render("individual/isThisYou.njk", json)
              }).map(Ok(_))
            case _ =>
              Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
          }
      }

  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val ua = request.userAnswers
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          val json = Json.obj(
            "form" -> formWithErrors,
            "submitUrl" -> routes.AreYouUKResidentController.onSubmit().url,
            "radios" -> Radios.yesNo(form("value"))
          )
          (ua.get(IndividualDetailsPage), ua.get(IndividualAddressPage)) match {
            case (Some(individualDetails), Some(individualAddress)) =>
              renderer.render("individual/isThisYou.njk", json).map(BadRequest(_))
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
