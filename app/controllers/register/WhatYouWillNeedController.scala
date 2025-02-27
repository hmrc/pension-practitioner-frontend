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

package controllers.register

import controllers.actions._
import models.NormalMode
import navigators.CompoundNavigator
import pages.register.WhatYouWillNeedPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.register.WhatYouWillNeedView

import javax.inject.Inject

class WhatYouWillNeedController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           @AuthMustHaveNoEnrolmentWithNoIV authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           navigator: CompoundNavigator,
                                           val controllerComponents: MessagesControllerComponents,
                                           whatYouWillNeedView: WhatYouWillNeedView
                                         ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData) { implicit request =>
    val ua = request.userAnswers
    val nextPage = navigator.nextPage(WhatYouWillNeedPage, NormalMode, ua)
    Ok(whatYouWillNeedView(nextPage.url))
  }
}
