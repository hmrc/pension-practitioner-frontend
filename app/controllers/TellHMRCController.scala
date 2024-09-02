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

package controllers

import config.FrontendAppConfig
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TwirlMigration
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.TellHMRCView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TellHMRCController @Inject()(
    override val messagesApi: MessagesApi,
    @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
  config: FrontendAppConfig,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer,
    tellHMRCView: TellHMRCView,
    twirlMigration: TwirlMigration
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(entityType: String): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val json = Json.obj(
        "entityType" -> entityType,
        "companiesHouseUrl" -> config.companiesHouseFileChangesUrl,
        "hmrcUrl" -> config.hmrcChangesMustReportUrl
      )

      val template = twirlMigration.duoTemplate(
        renderer.render("tellHMRC.njk", json),
        tellHMRCView(entityType, config.companiesHouseFileChangesUrl, config.hmrcChangesMustReportUrl)
      )
      template.map(Ok(_))
  }
}
