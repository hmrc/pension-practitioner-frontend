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

package controllers.deregister.individual

import java.time.LocalDate

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{DeregistrationConnector, EnrolmentConnector}
import controllers.Retrievals
import controllers.actions._
import forms.deregister.DeregistrationDateFormProvider
import javax.inject.Inject
import models.NormalMode
import navigators.CompoundNavigator
import pages.deregister.DeregistrationDatePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{DateInput, NunjucksSupport}

import scala.concurrent.{ExecutionContext, Future}

class DeregistrationDateController @Inject()(config: FrontendAppConfig,
                                             override val messagesApi: MessagesApi,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             navigator: CompoundNavigator,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: DeregistrationDateFormProvider,
                                             deregistrationConnector: DeregistrationConnector,
                                             enrolmentConnector: EnrolmentConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             renderer: Renderer
                                               )(implicit ec: ExecutionContext)
  extends FrontendBaseController with Retrievals with I18nSupport with NunjucksSupport {

  private def form(implicit messages: Messages): Form[LocalDate] = formProvider("individual")

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
          val preparedForm = request.userAnswers.get(DeregistrationDatePage).fold(form)(form.fill)
          val json = Json.obj(
                    "form" -> preparedForm,
                    "submitUrl" -> routes.DeregistrationDateController.onSubmit().url,
                    "date" -> DateInput.localDate(preparedForm("deregistrationDate")),
            "returnUrl" -> config.returnToPspDashboardUrl
                  )
          renderer.render("deregister/individual/deregistrationDate.njk", json).map(Ok(_))

  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

          form.bindFromRequest().fold(
            formWithErrors => {

              val json = Json.obj(
                "form" -> formWithErrors,
                "submitUrl" -> routes.DeregistrationDateController.onSubmit().url,
                "date" -> DateInput.localDate(formWithErrors("deregistrationDate"))
              )

              renderer.render("deregister/individual/deregistrationDate.njk", json).map(BadRequest(_))
            },
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(DeregistrationDatePage, value))
                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                _ <- deregistrationConnector.deregister(request.user.pspIdOrException, value)
                _ <- enrolmentConnector.deEnrol(request.user.userId, request.user.pspIdOrException, request.externalId)
              } yield Redirect(navigator.nextPage(DeregistrationDatePage, NormalMode, updatedAnswers))
          )
  }

}
