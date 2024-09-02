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
import models.register.RegistrationIdType.Nino

import javax.inject.Inject
import models.register.TolerantIndividual
import models.{Address, Mode}
import navigators.CompoundNavigator
import pages.RegistrationInfoPage
import pages.individual.{IndividualAddressPage, IndividualDetailsPage, IsThisYouPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.TwirlMigration
import utils.annotations.AuthMustHaveNoEnrolmentWithIV
import utils.countryOptions.CountryOptions
import views.html.individual.IsThisYouView

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
                                    renderer: Renderer,
                                    isThisYouView: IsThisYouView
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
      request.user.nino match {
        case None => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
        case Some(nino) =>
          (ua.get(IndividualDetailsPage), ua.get(IndividualAddressPage), ua.get(RegistrationInfoPage)) match {
            case (Some(individual), Some(address), Some(info)) if info.idType.contains(Nino) && info.idNumber.contains(nino.value) =>
              val template = TwirlMigration.duoTemplate(
                renderer.render(template = "individual/isThisYou.njk",
                  json ++ jsonWithNameAndAddress(individual, address)),
                isThisYouView(
                  routes.IsThisYouController.onSubmit(mode),
                  preparedForm,
                  TwirlMigration.toTwirlRadios(Radios.yesNo(preparedForm("value"))),
                  individual.fullName,
                  address.lines(countryOptions)
                )
              )
              template.map(Ok(_))
            case _ =>
              registrationConnector.registerWithIdIndividual(nino).flatMap { registration =>
                Future.fromTry(ua.set(IndividualDetailsPage, registration.response.individual).flatMap(
                  _.set(IndividualAddressPage, registration.response.address.toPrepopAddress)).flatMap(
                  _.set(RegistrationInfoPage, registration.info)
                )).flatMap { uaWithRegInfo =>
                  userAnswersCacheConnector.save(uaWithRegInfo.data).flatMap { _ =>
                    val template = TwirlMigration.duoTemplate(
                      renderer.render(template = "individual/isThisYou.njk",
                        json ++ jsonWithNameAndAddress(registration.response.individual, registration.response.address.toPrepopAddress)),
                      isThisYouView(
                        routes.IsThisYouController.onSubmit(mode),
                        preparedForm,
                        TwirlMigration.toTwirlRadios(Radios.yesNo(preparedForm("value"))),
                        registration.response.individual.fullName,
                        registration.response.address.toPrepopAddress.lines(countryOptions)
                      )
                    )
                    template.map(Ok(_))
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
          val json = Json.obj(
            "form" -> formWithErrors,
            "submitUrl" -> routes.IsThisYouController.onSubmit(mode).url,
            "radios" -> Radios.yesNo(form("value"))
          )
          (IndividualDetailsPage and IndividualAddressPage).retrieve.map {
            case individual ~ address =>
              val template = TwirlMigration.duoTemplate(
                renderer.render("individual/isThisYou.njk", json ++ jsonWithNameAndAddress(individual, address)),
                isThisYouView(
                  routes.IsThisYouController.onSubmit(mode),
                  formWithErrors,
                  TwirlMigration.toTwirlRadios(Radios.yesNo(form("value"))),
                  individual.fullName,
                  address.lines(countryOptions)
                )
              )
              template.map(BadRequest(_))
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
