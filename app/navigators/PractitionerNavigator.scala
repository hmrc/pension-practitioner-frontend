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
import models.WhatTypeBusiness
import models.register.BusinessType
import pages.Page
import pages.WhatTypeBusinessPage
import pages.register.company.BusinessUTRPage
import pages.register.{AreYouUKCompanyPage, WhatYouWillNeedPage, BusinessTypePage}
import play.api.mvc.Call

class PractitionerNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector, config: FrontendAppConfig)
  extends Navigator {

  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case WhatTypeBusinessPage => ua.get(WhatTypeBusinessPage) match {
      case Some(WhatTypeBusiness.Companyorpartnership) => controllers.register.routes
        .WhatYouWillNeedController.onPageLoad()
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
    case WhatYouWillNeedPage => controllers.register.routes.AreYouUKCompanyController.onPageLoad()
    case AreYouUKCompanyPage =>
      ua.get(AreYouUKCompanyPage) match {
        case Some(true) =>
          controllers.register.routes.BusinessTypeController.onPageLoad()
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    case BusinessTypePage=>
      ua.get(BusinessTypePage) match {
        case Some(BusinessType.LimitedCompany) =>
          controllers.register.company.routes.BusinessUTRController.onPageLoad()
        case Some(BusinessType.UnlimitedCompany) =>
          controllers.register.company.routes.BusinessUTRController.onPageLoad()
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    case BusinessUTRPage =>
      controllers.register.company.routes.CompanyNameController.onPageLoad()

  }

  override protected def editRouteMap(userAnswers: UserAnswers) = ???
}
