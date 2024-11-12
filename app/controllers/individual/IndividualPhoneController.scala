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

import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.{Retrievals, Variation}
import forms.PhoneFormProvider
import models.Mode
import navigators.CompoundNavigator
import pages.AddressChange
import pages.individual.{AreYouUKResidentPage, IndividualPhonePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthWithIV
import views.html.individual.PhoneView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndividualPhoneController @Inject()(override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          navigator: CompoundNavigator,
                                          @AuthWithIV authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: PhoneFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          phoneView: PhoneView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with Variation {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        request.userAnswers.get(AreYouUKResidentPage) match {
          case Some(true) =>
            val formFilled = request.userAnswers.get(IndividualPhonePage).fold(form)(form.fill)
            Future.successful(Ok(phoneView(
              routes.IndividualPhoneController.onSubmit(mode),
              formFilled
            )))
          case _ => Future.successful(
            Redirect(controllers.individual.routes.AreYouUKResidentController.onPageLoad(mode))
          )
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(phoneView(
              routes.IndividualPhoneController.onSubmit(mode),
              formWithErrors
            ))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IndividualPhonePage, value))
              answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
              _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
            } yield Redirect(navigator.nextPage(IndividualPhonePage, mode, answersWithChangeFlag))
        )

    }

  private def form(implicit messages: Messages): Form[String] =
    formProvider(messages("individual.phone.error.required"))
}
