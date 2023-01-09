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

package navigators

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.deregister.company.{routes => companyRoutes}
import controllers.deregister.individual.{routes => individualRoutes}
import models.UserAnswers
import pages.Page
import pages.deregister._
import play.api.mvc.Call

class DeregisterNavigator @Inject()(config: FrontendAppConfig) extends Navigator {

  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case ConfirmDeregistrationPage =>
      ua.get(ConfirmDeregistrationPage) match {
        case Some(true) => individualRoutes.DeregistrationDateController.onPageLoad()
        case _ => Call("GET", config.returnToPspDashboardUrl)
      }

    case DeregistrationDatePage => individualRoutes.SuccessController.onPageLoad()

    case ConfirmDeregistrationCompanyPage =>
      ua.get(ConfirmDeregistrationCompanyPage) match {
        case Some(true) => companyRoutes.DeregistrationDateController.onPageLoad()
        case _ => Call("GET", config.returnToPspDashboardUrl)
      }

    case DeregistrationDateCompanyPage => companyRoutes.SuccessController.onPageLoad()
  }

  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case ConfirmDeregistrationPage => controllers.routes.IndexController.onPageLoad()
  }
}
