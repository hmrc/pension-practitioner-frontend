/*
 * Copyright 2021 HM Revenue & Customs
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
import models.JourneyType
import models.requests.DataRequest
import pages.PspIdPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.annotations.AuthMustHaveEnrolment

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       subscriptionConnector: SubscriptionConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       @AuthMustHaveEnrolment authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer,
                                       emailConnector: EmailConnector,
                                       auditService: AuditService,
                                       config: FrontendAppConfig
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport
    with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        renderer.render(
          template = "amend/declaration.njk",
          ctx = Json.obj("submitUrl" -> routes.DeclarationController.onSubmit().url)
        ).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        DataRetrievals.retrievePspNameAndEmail { (pspName, email) =>
          for {
            originalSubscriptionDetails <- subscriptionConnector.getSubscriptionDetails(request.user.pspIdOrException)
            pspId <- subscriptionConnector.subscribePsp(request.userAnswers, JourneyType.PSP_AMENDMENT)
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PspIdPage, pspId))
            _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            _ <- Future.successful(audit(pspId, originalSubscriptionDetails, request.userAnswers.data))
            _ <- sendEmail(email, pspId, pspName)
          } yield Redirect(routes.ConfirmationController.onPageLoad())

        }
    }

  private def audit(
                     pspId: String,
                     originalSubscriptionDetails: JsValue,
                     updatedSubscriptionDetails: JsValue
                   )(
                     implicit request: DataRequest[_]
                   ): Unit =

    auditService.sendEvent(
      PSPAmendment(
        pspId = pspId,
        originalSubscriptionDetails = originalSubscriptionDetails,
        updatedSubscriptionDetails = updatedSubscriptionDetails
      )
    )

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
