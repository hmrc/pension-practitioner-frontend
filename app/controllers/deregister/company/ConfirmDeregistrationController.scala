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
import forms.deregister.ConfirmDeregistrationFormProvider
import models.{NormalMode, UserAnswers}
import navigators.CompoundNavigator
import pages.deregister.ConfirmDeregistrationCompanyPage
import pages.{PspEmailPage, PspNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveEnrolmentWithNoIV
import viewmodels.Radios
import views.html.deregister.company.ConfirmDeregistrationView

import javax.inject.Inject
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
                                                confirmDeregistrationView: ConfirmDeregistrationView
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController
                                                with I18nSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      request.user.alreadyEnrolledPspId.map { _ =>
        deregistrationConnector.canDeRegister.flatMap {
          case true =>
            minimalConnector.getMinimalPspDetails().flatMap { minimalDetails =>
              (minimalDetails.name, minimalDetails.email) match {
                case (Some(name), email) =>
                    val updatedAnswers = UserAnswers()
                      .setOrException(PspNamePage, name)
                      .setOrException(PspEmailPage, email)
                  userAnswersCacheConnector.save(updatedAnswers.data).map(_ =>
                    Ok(confirmDeregistrationView(routes.ConfirmDeregistrationController.onSubmit(),
                      form,
                      Radios.yesNo(form("value")),
                      name,
                      config.returnToPspDashboardUrl)))

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
            Future.successful(BadRequest(confirmDeregistrationView(routes.ConfirmDeregistrationController.onSubmit(),
              formWithErrors,
              Radios.yesNo(formWithErrors("value")),
              name,
              config.returnToPspDashboardUrl)))
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
