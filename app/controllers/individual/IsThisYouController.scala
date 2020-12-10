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
import forms.individual.IsThisYouFormProvider
import javax.inject.Inject
import models.register.TolerantIndividual
import models.{Mode, Address}
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.individual.{IndividualDetailsPage, IndividualAddressPage, IsThisYouPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.annotations.AuthWithIVNoEnrolment
import utils.countryOptions.CountryOptions

import scala.concurrent.{Future, ExecutionContext}

class IsThisYouController @Inject()(override val messagesApi: MessagesApi,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    navigator: CompoundNavigator,
                                    @AuthWithIVNoEnrolment authenticate: AuthAction,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    formProvider: IsThisYouFormProvider,
                                    registrationConnector: RegistrationConnector,
                                    countryOptions: CountryOptions,
                                    val controllerComponents: MessagesControllerComponents,
                                    config: FrontendAppConfig,
                                    renderer: Renderer
                                   )(implicit val executionContext: ExecutionContext
                                   ) extends FrontendBaseController with I18nSupport with NunjucksSupport with Retrievals {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val ua = request.userAnswers
      val preparedForm = ua.get(IsThisYouPage).fold(form)(form.fill)

      val json = Json.obj(
        fields = "form" -> preparedForm,
        "submitUrl" -> routes.IsThisYouController.onSubmit(mode).url,
        "radios" -> Radios.yesNo(preparedForm("value"))
      )
      (ua.get(IndividualDetailsPage), ua.get(IndividualAddressPage), ua.get(RegistrationInfoPage)) match {
        case (Some(individual), Some(address), Some(_)) =>
          renderer.render(template = "individual/isThisYou.njk", json ++ jsonWithNameAndAddress(individual, address)).map(Ok(_))
        case _ =>
          request.user.nino match {
            case Some(nino) =>
              registrationConnector.registerWithIdIndividual(nino).flatMap { registration =>
                Future.fromTry(ua.set(IndividualDetailsPage, registration.response.individual).flatMap(
                  _.set(IndividualAddressPage, registration.response.address.toAddress)).flatMap(
                  _.set(RegistrationInfoPage, registration.info)
                )).flatMap { uaWithRegInfo =>
                  userAnswersCacheConnector.save(uaWithRegInfo.data).flatMap { _ =>
                    renderer.render(template = "individual/isThisYou.njk", json ++
                      jsonWithNameAndAddress(registration.response.individual, registration.response.address.toAddress)).map(Ok(_))
                  }
                }
              }
            case _ =>
              Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
          }
      }

  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          val json = Json.obj(
            "form" -> formWithErrors,
            "submitUrl" -> routes.IsThisYouController.onSubmit(mode).url,
            "radios" -> Radios.yesNo(form("value"))
          )
          (IndividualDetailsPage and IndividualAddressPage).retrieve.right.map {
            case individual ~ address =>
              renderer.render("individual/isThisYou.njk", json ++ jsonWithNameAndAddress(individual, address)).map(BadRequest(_))
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

  private def jsonWithNameAndAddress(individual: TolerantIndividual, address: Address): JsObject = {
    Json.obj(
      "name" -> individual.fullName,
      "address" -> address.lines(countryOptions)
    )
  }

}
