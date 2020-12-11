/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.Retrievals
import controllers.Variation
import forms.individual.IndividualNameFormProvider
import javax.inject.Inject
import models.Mode
import models.register.TolerantIndividual
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.NameChange
import pages.individual._
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class IndividualNameController @Inject()(override val messagesApi: MessagesApi,
                                         userAnswersCacheConnector: UserAnswersCacheConnector,
                                         navigator: CompoundNavigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: IndividualNameFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         renderer: Renderer
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport with Variation {


  private def form(implicit messages: Messages): Form[TolerantIndividual] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        val formFilled = request.userAnswers.get(IndividualDetailsPage).fold(form)(form.fill)
        getJson(mode, formFilled) { json =>
          renderer.render(template = "individual/name.njk", json).map(Ok(_))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        form.bindFromRequest().fold(
          formWithErrors =>
            getJson(mode, formWithErrors) { json =>
              renderer.render(template = "individual/name.njk", json).map(BadRequest(_))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IndividualDetailsPage, value))
              answersWithChangeFlag <- Future.fromTry(setChangeFlag(updatedAnswers, NameChange))
              _ <- userAnswersCacheConnector.save(answersWithChangeFlag.data)
            } yield Redirect(navigator.nextPage(IndividualDetailsPage, mode, answersWithChangeFlag))
        )

    }



  private def getJson(mode: Mode, form: Form[TolerantIndividual])(block: JsObject => Future[Result])
                     (implicit request: DataRequest[AnyContent]): Future[Result] =
    block(Json.obj(
      "form" -> form,
      "submitUrl" -> routes.IndividualNameController.onSubmit(mode).url
    ))

}
