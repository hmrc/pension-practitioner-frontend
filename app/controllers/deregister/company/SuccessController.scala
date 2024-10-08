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

package controllers.deregister.company

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import pages.PspNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import views.html.deregister.company.SuccessView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SuccessController @Inject()(override val messagesApi: MessagesApi,
                                  userAnswersCacheConnector: UserAnswersCacheConnector,
                                  authenticate: AuthAction,
                                  getData: DataRetrievalAction,
                                  requireData: DataRequiredAction,
                                  val controllerComponents: MessagesControllerComponents,
                                  renderer: Renderer,
                                  successView: SuccessView,
                                  twirlMigration: TwirlMigration
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController
  with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      PspNamePage.retrieve.map { name =>
          val json: JsObject = Json.obj(
            "pspName" -> name,
            "submitUrl" -> controllers.routes.SignOutController.signOut().url
          )
          userAnswersCacheConnector.removeAll.flatMap { _ =>
            twirlMigration.duoTemplate(
              renderer.render("deregister/company/success.njk", json),
              successView(
                name,
                controllers.routes.SignOutController.signOut().url
              )
            ).map(Ok(_))
          }
      }
  }

}
