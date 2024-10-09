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

package controllers.partnership

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.BusinessUTRFormProvider
import models.NormalMode
import navigators.CompoundNavigator
import pages.partnership.BusinessUTRPage
import pages.register.BusinessTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.BusinessUTRView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessUTRController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: BusinessUTRFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      businessUTRView: BusinessUTRView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  protected def form: Form[String] = formProvider.apply(
    "businessUTR.partnership.error.required", "businessUTR.partnership.error.invalid")

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      BusinessTypePage.retrieve.map { businessType =>
        val preparedForm = request.userAnswers.get(BusinessUTRPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(businessUTRView(s"whatTypeBusiness.$businessType",
          preparedForm, routes.BusinessUTRController.onSubmit())))
      }
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      BusinessTypePage.retrieve.map { businessType =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(businessUTRView(s"whatTypeBusiness.$businessType",
              formWithErrors, routes.BusinessUTRController.onSubmit())))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(BusinessUTRPage, value))
              _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            } yield Redirect(navigator.nextPage(BusinessUTRPage, NormalMode, updatedAnswers))
        )
      }
  }
}
