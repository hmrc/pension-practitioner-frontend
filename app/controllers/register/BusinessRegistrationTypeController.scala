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

package controllers.register

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import forms.register.BusinessRegistrationTypeFormProvider
import models.NormalMode
import models.register.BusinessRegistrationType
import navigators.CompoundNavigator
import pages.register.BusinessRegistrationTypePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.register.BusinessRegistrationTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegistrationTypeController @Inject()(override val messagesApi: MessagesApi,
                                                   userAnswersCacheConnector: UserAnswersCacheConnector,
                                                   navigator: CompoundNavigator,
                                                   @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   formProvider: BusinessRegistrationTypeFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   config: FrontendAppConfig,
                                                   businessRegistrationTypeView: BusinessRegistrationTypeView
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(BusinessRegistrationTypePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Ok(businessRegistrationTypeView(
        routes.BusinessRegistrationTypeController.onSubmit(),
        preparedForm,
        BusinessRegistrationType.radios(preparedForm)))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(businessRegistrationTypeView(
            routes.BusinessRegistrationTypeController.onSubmit(),
            formWithErrors,
            BusinessRegistrationType.radios(formWithErrors))))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BusinessRegistrationTypePage, value))
            _ <- userAnswersCacheConnector.save(updatedAnswers.data)
          } yield Redirect(navigator.nextPage(BusinessRegistrationTypePage, NormalMode, updatedAnswers))
      )

  }
}
