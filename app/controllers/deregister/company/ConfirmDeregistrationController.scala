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

package controllers.deregister.company

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{DeregistrationConnector, MinimalConnector}
import controllers.Retrievals
import controllers.actions._
import controllers.deregister.company.routes
import forms.deregister.ConfirmDeregistrationFormProvider

import javax.inject.Inject
import models.{NormalMode, UserAnswers}
import navigators.CompoundNavigator
import pages.{PspEmailPage, PspNamePage}
import pages.deregister.ConfirmDeregistrationCompanyPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.TwirlMigration
import utils.annotations.AuthMustHaveEnrolmentWithNoIV
import views.html.deregister.company.ConfirmDeregistrationView

import scala.concurrent.{ExecutionContext, Future}

class ConfirmDeregistrationController @Inject()(config: FrontendAppConfig,
                                                override val messagesApi: MessagesApi,
                                                userAnswersCacheConnector: UserAnswersCacheConnector,
                                                navigator: CompoundNavigator,
                                                @AuthMustHaveEnrolmentWithNoIV authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: ConfirmDeregistrationFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                deregistrationConnector: DeregistrationConnector,
                                                minimalConnector: MinimalConnector,
                                                renderer: Renderer,
                                                confirmDeregistrationView: ConfirmDeregistrationView
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController
                                                with I18nSupport with NunjucksSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      request.user.alreadyEnrolledPspId.map { pspId =>
        deregistrationConnector.canDeRegister(pspId).flatMap {
          case true =>
            minimalConnector.getMinimalPspDetails(pspId).flatMap { minimalDetails =>
              (minimalDetails.name, minimalDetails.email) match {
                case (Some(name), email) =>
                    val json = Json.obj(
                      "form" -> form,
                      "pspName" -> name,
                      "submitUrl" -> routes.ConfirmDeregistrationController.onSubmit().url,
                      "radios" -> Radios.yesNo(form("value")),
                      "returnUrl" -> config.returnToPspDashboardUrl
                    )

                    val updatedAnswers = UserAnswers()
                      .setOrException(PspNamePage, name)
                      .setOrException(PspEmailPage, email)

                  TwirlMigration.duoTemplate(
                    renderer.render("deregister/company/confirmDeregistration.njk", json),
                    confirmDeregistrationView(routes.ConfirmDeregistrationController.onSubmit(),
                    form,
                    TwirlMigration.toTwirlRadios(Radios.yesNo(form("value"))),
                      name,
                      config.returnToPspDashboardUrl)
                  ).flatMap( view => userAnswersCacheConnector.save(updatedAnswers.data).map( _ => Ok(view)))

                case _ => sessionExpired
              }
            }
          case false =>
            Future.successful(Redirect(controllers.deregister.routes.CannotDeregisterController.onPageLoad()))
        }
      }.getOrElse(sessionExpired)
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      PspNamePage.retrieve.map { name =>
        form.bindFromRequest().fold(
          formWithErrors => {

            val json = Json.obj(
              "form" -> formWithErrors,
              "pspName" -> name,
              "submitUrl" -> routes.ConfirmDeregistrationController.onSubmit().url,
              "radios" -> Radios.yesNo(formWithErrors("value"))
            )

            TwirlMigration.duoTemplate(
              renderer.render("deregister/company/confirmDeregistration.njk", json),
              confirmDeregistrationView(routes.ConfirmDeregistrationController.onSubmit(),
                formWithErrors,
                TwirlMigration.toTwirlRadios(Radios.yesNo(formWithErrors("value"))),
                name,
                config.returnToPspDashboardUrl)
            ).map(Ok(_))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmDeregistrationCompanyPage, value))
              _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            } yield Redirect(navigator.nextPage(ConfirmDeregistrationCompanyPage, NormalMode, updatedAnswers))
        )
      }
  }

  private val sessionExpired: Future[Result] = Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
}
