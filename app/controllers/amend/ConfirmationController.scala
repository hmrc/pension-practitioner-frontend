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

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._

import javax.inject.Inject
import pages.PspIdPage
import pages.company.CompanyEmailPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import utils.annotations.AuthMustHaveEnrolmentWithNoIV

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       @AuthMustHaveEnrolmentWithNoIV authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer,
                                       confirmationView: views.html.amend.ConfirmationView,
                                       twirlMigration: TwirlMigration
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (CompanyEmailPage and PspIdPage).retrieve.map {
        case email ~ pspid =>
          val json: JsObject = Json.obj(
            "panelHtml" -> confirmationPanelText(pspid).toString(),
            "email" -> email,
            "submitUrl" -> appConfig.returnToPspDashboardUrl
          )

          userAnswersCacheConnector.removeAll.flatMap { _ =>
            val template = twirlMigration.duoTemplate(
              renderer.render("amend/confirmation.njk", json),
              confirmationView(email, confirmationPanelText(pspid).toString(), appConfig.returnToPspDashboardUrl)
            )
            template.map(Ok(_))
          }
        case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  private def confirmationPanelText(pspId: String)(implicit messages: Messages): Html = {
    Html(s"""<p>${{ messages("confirmation.psp.id") }}</p>
            |<span class="heading-large govuk-!-font-weight-bold">$pspId</span>""".stripMargin)
  }

}
