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

package controllers.deregister.individual

import audit.{AuditService, PSPDeregistration, PSPDeregistrationEmail}
import config.FrontendAppConfig
import connectors._
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.deregister.DeregistrationDateFormProvider
import helpers.FormatHelper.dateContentFormatter
import models.requests.DataRequest
import models.{JourneyType, NormalMode}
import navigators.CompoundNavigator
import pages.deregister.DeregistrationDatePage
import pages.{PspEmailPage, PspNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.deregister.individual.DeregistrationDateView

import java.time.LocalDate
import javax.inject.Inject
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
                                             subscriptionConnector: SubscriptionConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             emailConnector: EmailConnector,
                                             auditService: AuditService,
                                             deregistrationDateView: DeregistrationDateView
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController with Retrievals with I18nSupport {

  private def form(date: LocalDate)(implicit messages: Messages): Form[LocalDate] = formProvider("individual", date)

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      getDate.flatMap { date =>
        val preparedForm = request.userAnswers.get(DeregistrationDatePage).fold(form(date))(form(date).fill)
        Future.successful(Ok(deregistrationDateView(routes.DeregistrationDateController.onSubmit(),
          preparedForm,
          config.returnToPspDashboardUrl,
          getDateString(date))))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val pspId = request.user.pspIdOrException
      getDate.flatMap { date =>
        (PspNamePage and PspEmailPage).retrieve.map { case pspName ~ email =>
          form(date).bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(deregistrationDateView(routes.DeregistrationDateController.onSubmit(),
                formWithErrors,
                config.returnToPspDashboardUrl,
                getDateString(date))))
            },
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(DeregistrationDatePage, value))
                _ <- userAnswersCacheConnector.save(updatedAnswers.data)
                _ <- deregistrationConnector.deregister(request.user.pspIdOrException, value)
                _ <- Future(auditService.sendEvent(PSPDeregistration(pspId)))
                _ <- enrolmentConnector.deEnrol(request.user.groupIdentifier, request.user.pspIdOrException, request.externalId)
                _ <- sendEmail(email, pspId, pspName)
              } yield Redirect(navigator.nextPage(DeregistrationDatePage, NormalMode, updatedAnswers))
          )
        }
      }
  }

  private def sendEmail(email: String, pspId: String, pspName: String)
    (implicit request: DataRequest[_], hc: HeaderCarrier ): Future[EmailStatus] =
    emailConnector.sendEmail(
      requestId = hc.requestId .map(_.value) .getOrElse(request.headers.get("X-Session-ID").getOrElse("")),
      pspId,
      journeyType = JourneyType.PSP_DEREGISTRATION,
      email,
      templateName = config.emailPspDeregistrationTemplateId,
      templateParams = Map("pspName" -> pspName)
    ).map { status =>
        auditService.sendEvent(PSPDeregistrationEmail(pspId, email))
        status
    }

  private def getDate(implicit request: DataRequest[AnyContent]): Future[LocalDate] =
    subscriptionConnector.getPspApplicationDate(request.user.pspIdOrException)

  private def getDateString(date: LocalDate) = date.format(dateContentFormatter)

}
