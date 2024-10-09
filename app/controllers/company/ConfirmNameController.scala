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

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.ConfirmNameFormProvider
import models.NormalMode
import navigators.CompoundNavigator
import pages.company.{BusinessNamePage, ConfirmNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.Radios
import utils.TwirlMigration
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.ConfirmNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmNameController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: ConfirmNameFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      confirmNameView: ConfirmNameView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      BusinessNamePage.retrieve.map { pspName =>
        val preparedForm = request.userAnswers.get(ConfirmNamePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(confirmNameView(
          "company",
          preparedForm,
          routes.ConfirmNameController.onSubmit(),
          pspName,
          Radios.yesNo(preparedForm("value")))
        ))
    }
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      BusinessNamePage.retrieve.map { pspName =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(confirmNameView(
              "company",
              formWithErrors,
              routes.ConfirmNameController.onSubmit(),
              pspName,
              Radios.yesNo(formWithErrors("value")))
            ))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmNamePage, value))
              _ <- userAnswersCacheConnector.save( updatedAnswers.data)
            } yield Redirect(navigator.nextPage(ConfirmNamePage, NormalMode, updatedAnswers))
        )
      }
  }
}
