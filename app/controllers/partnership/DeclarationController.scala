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
import connectors.{EmailConnector, EmailStatus, EnrolmentConnector, SubscriptionConnector}
import controllers.actions._
import controllers.{DataRetrievals, Retrievals}
import models.requests.DataRequest
import models.{ExistingPSP, JourneyType, NormalMode}
import navigators.CompoundNavigator
import pages.PspIdPage
import pages.partnership.DeclarationPage
import pages.register.ExistingPSPPage
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.KnownFactsRetrieval
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.register.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       subscriptionConnector: SubscriptionConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       navigator: CompoundNavigator,
                                       @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       emailConnector: EmailConnector,
                                       knownFactsRetrieval: KnownFactsRetrieval,
                                       enrolment: EnrolmentConnector,
                                       config: FrontendAppConfig,
                                       declarationView: DeclarationView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData) { implicit request =>
      Ok(declarationView(routes.DeclarationController.onSubmit()))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      val ua = request.userAnswers
        .setOrException(ExistingPSPPage, ExistingPSP(request.user.isExistingPSP, request.user.existingPSPId))

      DataRetrievals.retrievePspNameAndEmail { (pspName, email) =>
        for {
          pspId <- subscriptionConnector.subscribePsp(ua, JourneyType.PSP_SUBSCRIPTION)
          updatedAnswers <- Future.fromTry(
            request.userAnswers.set(PspIdPage, pspId)
          )
          _ <- userAnswersCacheConnector.save(updatedAnswers.data)
          _ <- enrol(pspId)
          _ <- sendEmail(email, pspId, pspName)
        } yield
          Redirect(
            navigator.nextPage(DeclarationPage, NormalMode, updatedAnswers)
          )
      } recoverWith {
        case ex: UpstreamErrorResponse if is5xx(ex.statusCode) =>
          Future.successful(Redirect(controllers.routes.YourActionWasNotProcessedController.onPageLoad()))
        case ex: UpstreamErrorResponse if ex.message.contains("ACTIVE_PSPID") && is4xx(ex.statusCode) =>
          Future.successful(Redirect(controllers.routes.CannotRegisterPractitionerController.onPageLoad()))
        case ex =>
          logger.warn("Error - redirecting to session expired page", ex)
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
    }

  private def enrol(pspId: String)
                   (implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[HttpResponse] =
    knownFactsRetrieval.retrieve(pspId) map { knownFacts =>
      enrolment.enrol(pspId, knownFacts)
    } getOrElse Future.failed(KnownFactsRetrievalException())

  private val logger = Logger(classOf[DeclarationController])

  case class KnownFactsRetrievalException() extends Exception {
    def apply(): Unit = logger.error("Could not retrieve Known Facts")
  }

  private def sendEmail(email: String, pspId: String, pspName: String)
                       (implicit request: DataRequest[_], hc: HeaderCarrier): Future[EmailStatus] =
    emailConnector.sendEmail(
      requestId = hc.requestId.map(_.value).getOrElse(request.headers.get("X-Session-ID").getOrElse("")),
      pspId,
      journeyType = JourneyType.PSP_SUBSCRIPTION,
      email,
      templateName = config.emailPspSubscriptionTemplateId,
      templateParams = Map("pspName" -> pspName)
    )

}
