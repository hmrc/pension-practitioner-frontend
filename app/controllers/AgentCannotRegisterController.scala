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

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TwirlMigration
import views.html.AgentCannotRegisterView

import scala.concurrent.ExecutionContext

class AgentCannotRegisterController @Inject()(
                                        val controllerComponents: MessagesControllerComponents,
                                        renderer: Renderer,
                                        config: FrontendAppConfig,
                                        agentCannotRegisterView: AgentCannotRegisterView,
                                        twirlMigration: TwirlMigration
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = Action.async { implicit request =>
    val json = Json.obj(
      "govUkUrl" -> config.govUkUrl
    )

    def template = twirlMigration.duoTemplate(
      renderer.render("agentCannotRegister.njk", json),
      agentCannotRegisterView()
    )

    template.map(Ok(_))
  }
}
