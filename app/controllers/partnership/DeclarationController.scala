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

package controllers.partnership

import connectors.SubscriptionConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import javax.inject.Inject
import models.NormalMode
import navigators.CompoundNavigator
import pages.PspIdPage
import pages.partnership.DeclarationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(override val messagesApi: MessagesApi,
                                      subscriptionConnector: SubscriptionConnector,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      val controllerComponents: MessagesControllerComponents,
                                      renderer: Renderer
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController
                                      with Retrievals with I18nSupport with NunjucksSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      renderer.render("register/declaration.njk",
        Json.obj("submitUrl" -> routes.DeclarationController.onSubmit().url)).map(Ok(_))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
      implicit request =>
        for {
          pspId <- subscriptionConnector.subscribePsp(request.userAnswers)
          updatedAnswers <- Future.fromTry(request.userAnswers.set(PspIdPage, pspId))
          _ <- userAnswersCacheConnector.save(updatedAnswers.data)
        } yield Redirect(navigator.nextPage(DeclarationPage, NormalMode, updatedAnswers))
    }

}
