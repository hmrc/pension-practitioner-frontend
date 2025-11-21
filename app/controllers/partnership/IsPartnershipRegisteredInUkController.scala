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

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.partnership.IsPartnershipRegisteredInUkFormProvider
import models.NormalMode
import navigators.CompoundNavigator
import pages.partnership.IsPartnershipRegisteredInUkPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import viewmodels.Radios
import views.html.partnership.IsPartnershipRegisteredInUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsPartnershipRegisteredInUkController @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: IsPartnershipRegisteredInUkFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      config: FrontendAppConfig,
                                      isPartnershipRegisteredInUkView: IsPartnershipRegisteredInUkView
                                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals{

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get (IsPartnershipRegisteredInUkPage) match {
        case None => form
        case Some (value) => form.fill (value)
      }
      Ok(isPartnershipRegisteredInUkView(
        routes.IsPartnershipRegisteredInUkController.onSubmit(),
        preparedForm,
        Radios.yesNo(preparedForm("value"))
      ))
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(isPartnershipRegisteredInUkView(
            routes.IsPartnershipRegisteredInUkController.onSubmit(),
            formWithErrors,
            Radios.yesNo (formWithErrors("value"))
          )))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsPartnershipRegisteredInUkPage, value))
            _ <- userAnswersCacheConnector.save( updatedAnswers.data)
          } yield Redirect(navigator.nextPage(IsPartnershipRegisteredInUkPage, NormalMode, updatedAnswers))
      )
  }
}
