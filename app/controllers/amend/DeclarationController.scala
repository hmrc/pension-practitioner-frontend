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

package controllers.amend

import audit.{AuditService, PSPAmendment}
import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{EmailConnector, EmailStatus, SubscriptionConnector}
import controllers.actions._
import controllers.{DataRetrievals, Retrievals}
import models.requests.DataRequest
import models.{JourneyType, UserAnswers}
import pages.{PspIdPage, UnchangedPspDetailsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PspDetailsService
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveEnrolmentWithNoIV

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       subscriptionConnector: SubscriptionConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       @AuthMustHaveEnrolmentWithNoIV authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       emailConnector: EmailConnector,
                                       auditService: AuditService,
                                       pspDetailsService: PspDetailsService,
                                       config: FrontendAppConfig,
                                       declarationView: views.html.amend.DeclarationView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>

        if (pspDetailsService.amendmentsExist(request.userAnswers)) {
          Future.successful(Ok(declarationView(routes.DeclarationController.onSubmit())))
        } else {
          Future.successful(Redirect(routes.ViewDetailsController.onPageLoad()))
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        DataRetrievals.retrievePspNameAndEmail { (pspName, email) =>
          for {
            originalSubscriptionDetails <- getOriginalPspDetails(request.userAnswers)
            pspId <- subscriptionConnector.subscribePsp(request.userAnswers, JourneyType.PSP_AMENDMENT)
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PspIdPage, pspId))
            _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            _ <- audit(pspId, originalSubscriptionDetails, request.userAnswers.data)
            _ <- sendEmail(email, pspId, pspName)
          } yield Redirect(routes.ConfirmationController.onPageLoad())

        } recoverWith {
          case ex: UpstreamErrorResponse if is5xx(ex.statusCode) =>
            Future.successful(Redirect(controllers.routes.YourActionWasNotProcessedController.onPageLoad()))
          case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        }
    }
  private def getOriginalPspDetails(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[JsValue] =
    ua.get(UnchangedPspDetailsPage).fold(subscriptionConnector.getSubscriptionDetails)(Future.successful(_))

  private def audit(
                     pspId: String,
                     originalSubscriptionDetails: JsValue,
                     updatedSubscriptionDetails: JsValue
                   )(
                     implicit request: DataRequest[_]
                   ): Future[Unit] =

    Future.successful(auditService.sendExtendedEvent(
      PSPAmendment(
        pspId = pspId,
        originalSubscriptionDetails = originalSubscriptionDetails,
        updatedSubscriptionDetails = updatedSubscriptionDetails
      )
    ))

  private def sendEmail(
                         email: String,
                         pspId: String,
                         pspName: String
                       )(
                         implicit request: DataRequest[_],
                         hc: HeaderCarrier
                       ): Future[EmailStatus] = {
    val requestId: String =
      hc.requestId.map(_.value).getOrElse(request.headers.get("X-Session-ID").getOrElse(""))

    emailConnector.sendEmail(
      requestId = requestId,
      pspId = pspId,
      journeyType = JourneyType.PSP_AMENDMENT,
      emailAddress = email,
      templateName = config.emailPspAmendmentTemplateId,
      templateParams = Map("pspName" -> pspName)
    )
  }
}
