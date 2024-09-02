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

package controllers.individual

import com.google.inject.Inject
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.IndividualCYAService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.TwirlMigration
import utils.annotations.AuthMustHaveNoEnrolmentWithIV
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @AuthMustHaveNoEnrolmentWithIV authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            individualCYAService: IndividualCYAService,
                                            renderer: Renderer,
                                            checkYourAnswersView: CheckYourAnswersView,
                                            twirlMigration: TwirlMigration
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>

      val json = Json.obj(
        "redirectUrl" -> controllers.individual.routes.DeclarationController.onPageLoad().url,
        "list" -> individualCYAService.individualCya(request.userAnswers)
      )

      def template = twirlMigration.duoTemplate(
        renderer.render("check-your-answers.njk", json),
        checkYourAnswersView(
          controllers.individual.routes.DeclarationController.onPageLoad(),
          TwirlMigration.summaryListRow(
            individualCYAService.individualCya(request.userAnswers)
          )
        )
      )

      template.map(Ok(_))
    }
}
