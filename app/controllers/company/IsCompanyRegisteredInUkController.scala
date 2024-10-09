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
import forms.company.IsCompanyRegisteredInUkFormProvider
import models.NormalMode
import navigators.CompoundNavigator
import pages.company.IsCompanyRegisteredInUkPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.TwirlMigration
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.company.IsCompanyRegisteredInUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class IsCompanyRegisteredInUkController @Inject()(override val messagesApi: MessagesApi,
                                                  userAnswersCacheConnector: UserAnswersCacheConnector,
                                                  navigator: CompoundNavigator,
                                                  @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  formProvider: IsCompanyRegisteredInUkFormProvider,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  isCompanyRegisteredInUkView: IsCompanyRegisteredInUkView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with
  I18nSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get (IsCompanyRegisteredInUkPage) match {
        case None => form
        case Some (value) => form.fill (value)
      }

      Ok(isCompanyRegisteredInUkView(
        routes.IsCompanyRegisteredInUkController.onSubmit(),
        preparedForm,
        TwirlMigration.toTwirlRadios(Radios.yesNo(preparedForm("value")))
      ))
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(isCompanyRegisteredInUkView(
            routes.IsCompanyRegisteredInUkController.onSubmit(),
            formWithErrors,
            TwirlMigration.toTwirlRadios(Radios.yesNo(formWithErrors("value")))
          )))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsCompanyRegisteredInUkPage, value))
            _ <- userAnswersCacheConnector.save( updatedAnswers.data)
          } yield Redirect(navigator.nextPage(IsCompanyRegisteredInUkPage, NormalMode, updatedAnswers))
      )
  }
}
