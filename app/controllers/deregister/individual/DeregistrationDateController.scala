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
import connectors.EmailConnector
import connectors.EmailStatus
import connectors.cache.UserAnswersCacheConnector
import connectors.{EnrolmentConnector, DeregistrationConnector}
import controllers.Retrievals
import controllers.actions._
import forms.deregister.DeregistrationDateFormProvider
import javax.inject.Inject
import models.NormalMode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.PspEmailPage
import pages.PspNamePage
import pages.deregister.DeregistrationDatePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, DateInput}

import scala.concurrent.{Future, ExecutionContext}

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
                                             renderer: Renderer,
                                             emailConnector: EmailConnector
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
      val pspId = request.user.pspIdOrException
      (PspNamePage and PspEmailPage).retrieve.right.map { case pspName ~ email =>
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
                _ <- sendEmail(email, pspId, pspName)
              } yield Redirect(navigator.nextPage(DeregistrationDatePage, NormalMode, updatedAnswers))
          )
     }
  }

  private def sendEmail(email: String, pspId: String, pspName: String)
    (implicit request: DataRequest[_], hc: HeaderCarrier, messages: Messages ): Future[EmailStatus] =
    emailConnector.sendEmail(
      requestId = hc.requestId .map(_.value) .getOrElse(request.headers.get("X-Session-ID").getOrElse("")),
      pspId,
      journeyType = "PSPDeregistration",
      email,
      templateName = config.emailPspDeregistrationTemplateId,
      templateParams = Map("pspName" -> pspName)
    )

}
