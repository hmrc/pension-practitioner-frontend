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
import forms.EmailFormProvider
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.AddressChange
import pages.individual.{AreYouUKResidentPage, IndividualEmailPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import utils.annotations.AuthWithIV
import views.html.individual.EmailView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndividualEmailController @Inject()(override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          navigator: CompoundNavigator,
                                          @AuthWithIV authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: EmailFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          renderer: Renderer,
                                          emailView: EmailView,
                                          twirlMigration: TwirlMigration
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport with Variation {

  private def form(implicit messages: Messages): Form[String] =
    formProvider(messages("individual.email.error.required"))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        request.userAnswers.get(AreYouUKResidentPage) match {
          case Some(true) =>
            val formFilled = request.userAnswers.get(IndividualEmailPage).fold(form)(form.fill)
            getJson(mode, formFilled) { json =>
              val template = twirlMigration.duoTemplate(
              renderer.render(template = "individual/email.njk", json),
                emailView(
                  routes.IndividualEmailController.onSubmit(mode),
                  formFilled
                )
              )
              template.map(Ok(_))
            }
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
            getJson(mode, formWithErrors) { json =>
              val template = twirlMigration.duoTemplate(
                renderer.render(template = "individual/email.njk", json),
                emailView(
                  routes.IndividualEmailController.onSubmit(mode),
                  formWithErrors
                )
              )
              template.map(BadRequest(_))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IndividualEmailPage, value))
              answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, AddressChange))
              _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
            } yield Redirect(navigator.nextPage(IndividualEmailPage, mode, answersWithChangeFlag))
        )

    }

  private def getJson(mode: Mode, form: Form[String])(block: JsObject => Future[Result])
                     (implicit request: DataRequest[AnyContent]): Future[Result] =
    block(Json.obj(
      "form" -> form,
      "submitUrl" -> routes.IndividualEmailController.onSubmit(mode).url
    ))
}
