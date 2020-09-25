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

package controllers.company

import config.FrontendAppConfig
import connectors.EmailConnector
import connectors.EmailStatus
import connectors.SubscriptionConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.DataRetrievals
import controllers.Retrievals
import controllers.actions._
import javax.inject.Inject
import models.ExistingPSP
import models.NormalMode
import models.requests.DataRequest
import navigators.CompoundNavigator
import pages.PspIdPage
import pages.company.DeclarationPage
import pages.register.ExistingPSPPage
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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  subscriptionConnector: SubscriptionConnector,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  navigator: CompoundNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  renderer: Renderer,
  emailConnector: EmailConnector,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Retrievals
    with I18nSupport
    with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      renderer
        .render(
          "register/declaration.njk",
          Json.obj("submitUrl" -> routes.DeclarationController.onSubmit().url)
        )
        .map(Ok(_))
    }

  def onSubmit: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val ua = request.userAnswers
      .setOrException(ExistingPSPPage, ExistingPSP(request.user.isExistingPSP, request.user.existingPSPId))

      println( "\n>>>UU" + ua)

      DataRetrievals.retrievePspNameAndEmail { (pspName, email) =>
        for {
          pspId <- subscriptionConnector.subscribePsp(ua)
          updatedAnswers <- Future.fromTry(
            request.userAnswers.set(PspIdPage, pspId)
          )
          _ <- userAnswersCacheConnector.save(updatedAnswers.data)
          _ <- sendEmail(email, pspId, pspName)
        } yield
          Redirect(
            navigator.nextPage(DeclarationPage, NormalMode, updatedAnswers)
          )
      }
    }

  private def sendEmail(email: String, pspId: String, pspName: String)(
    implicit request: DataRequest[_],
    hc: HeaderCarrier,
    messages: Messages
  ): Future[EmailStatus] =
    emailConnector.sendEmail(
      requestId = hc.requestId
        .map(_.value)
        .getOrElse(request.headers.get("X-Session-ID").getOrElse("")),
      pspId,
      journeyType = "PSPSubscription",
      email,
      templateName = config.emailPspSubscriptionTemplateId,
      templateParams = Map("pspName" -> pspName)
    )

}
