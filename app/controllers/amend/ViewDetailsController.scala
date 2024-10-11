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

import com.google.inject.Inject
import controllers.actions.{AuthAction, DataRetrievalAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PspDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveEnrolmentWithNoIV

import scala.concurrent.{ExecutionContext, Future}

class ViewDetailsController @Inject()(@AuthMustHaveEnrolmentWithNoIV authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      pspDetailsService: PspDetailsService,
                                      val controllerComponents: MessagesControllerComponents,
                                      viewDetailsView: views.html.amend.ViewDetailsView,
                                    )(implicit val executionContext: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      request.user.alreadyEnrolledPspId.map { pspId =>
          pspDetailsService.getData(request.userAnswers, pspId).flatMap { data =>
            Future.successful(Ok(viewDetailsView(
              data.pageTitle,
              data.heading,
              data.list,
              data.displayContinueButton,
              data.nextPage,
              data.returnLinkAndUrl
            )))
        }
      }.getOrElse(
        Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      )
  }

}
