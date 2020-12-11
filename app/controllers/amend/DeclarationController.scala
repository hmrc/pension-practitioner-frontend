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

package controllers.amend

import audit.AuditService
import audit.PSPAmendment
import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.EmailConnector
import connectors.SubscriptionConnector
import controllers.actions._
import controllers.DataRetrievals
import controllers.Retrievals
import javax.inject.Inject
import models.requests.DataRequest
import pages.PspIdPage
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.annotations.AuthMustHaveEnrolment

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
    extends FrontendBaseController with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        renderer.render(
            "amend/declaration.njk",
            Json.obj("submitUrl" -> routes.DeclarationController.onSubmit().url)
          ).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        DataRetrievals.retrievePspNameAndEmail { (pspName, email) =>
          for {
            pspId <- subscriptionConnector.subscribePsp(request.userAnswers)
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PspIdPage, pspId))
            _ <- userAnswersCacheConnector.save(updatedAnswers.data)
            _ <- sendEmail(email, pspId, pspName)
          } yield  Redirect(routes.ConfirmationController.onPageLoad())

        }
    }

  private def sendEmail(email: String, pspId: String, pspName: String)(implicit request: DataRequest[_],
                                                                       hc: HeaderCarrier,
                                                                       messages: Messages): Future[Unit] = {
    val requestId: String = hc.requestId.map(_.value).getOrElse(request.headers.get("X-Session-ID").getOrElse(""))
    emailConnector.sendEmail(requestId, pspId, "PSPAmendment",
      email, config.emailPspAmendmentTemplateId, Map("pspName" -> pspName)).map { _ =>
      auditService.sendEvent(PSPAmendment(pspId, email))
    }
  }

}
