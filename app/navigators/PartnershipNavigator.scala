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
import connectors.cache.UserAnswersCacheConnector
import models.UserAnswers
import pages.Page
import pages.partnership.{BusinessNamePage, BusinessUTRPage, ConfirmAddressPage, ConfirmNamePage}
import play.api.mvc.Call

class PartnershipNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessUTRPage => controllers.partnership.routes.PartnershipNameController.onPageLoad()
    case BusinessNamePage => controllers.partnership.routes.ConfirmNameController.onPageLoad()
    case ConfirmNamePage => ua.get(ConfirmNamePage) match {
      case Some(false) => controllers.partnership.routes.TellHMRCController.onPageLoad()
      case _ => controllers.partnership.routes.ConfirmAddressController.onPageLoad()
    }
    case ConfirmAddressPage => ua.get(ConfirmAddressPage) match {
      case None => controllers.partnership.routes.TellHMRCController.onPageLoad()
      case _ => controllers.partnership.routes.TellHMRCController.onPageLoad()
    }
  }
  //scalastyle:on cyclomatic.complexity

  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessUTRPage => controllers.routes.SessionExpiredController.onPageLoad()
  }
}
