/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.actions.{AuthAction, DataRetrievalAction}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.PspDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class YourActionWasNotProcessedController @Inject()(appConfig: FrontendAppConfig,
                                                     override val messagesApi: MessagesApi,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     authenticate: AuthAction,
                                                     getData: DataRetrievalAction,
                                                     pspDetailsService: PspDetailsService,
                                                     renderer: Renderer
                                                   )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData).async {
      implicit request =>

        val pspName: Option[String] = request.userAnswers.flatMap(pspDetailsService.getPspName)

        val json: JsObject = if(request.user.alreadyEnrolledPspId.nonEmpty && pspName.nonEmpty) {
          Json.obj("returnLink" -> appConfig.returnToPspDashboardUrl, "pspName" -> pspName.get)
        } else {
          Json.obj()
        }
        
        renderer.render("yourActionWasNotProcessed.njk", json).map(Ok(_))
    }
}