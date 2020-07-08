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

package navigators

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import models.UserAnswers
import pages.Page
import pages.register.company.ConfirmAddressPage
import pages.register.company.BusinessUTRPage
import pages.register.company.CompanyNamePage
import pages.register.company.ConfirmNamePage
import play.api.mvc.Call

class CompanyNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector, config: FrontendAppConfig)
  extends Navigator {

  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessUTRPage =>
      controllers.register.company.routes.CompanyNameController.onPageLoad()
    case CompanyNamePage =>
      controllers.register.company.routes.ConfirmNameController.onPageLoad()
    case ConfirmNamePage =>
      ua.get(ConfirmNamePage) match {
        case Some(false) => controllers.register.company.routes.TellHMRCController.onPageLoad()
        case _ => controllers.register.company.routes.ConfirmAddressController.onPageLoad()
      }
    case ConfirmAddressPage =>
      ua.get(ConfirmAddressPage) match {
        case None => controllers.register.company.routes.TellHMRCController.onPageLoad()
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }


  }

  override protected def editRouteMap(userAnswers: UserAnswers) = ???
}
