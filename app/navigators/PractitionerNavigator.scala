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
import connectors.cache.UserAnswersCacheConnector
import models.{NormalMode, WhatTypeBusiness, UserAnswers}
import models.register.{BusinessRegistrationType, BusinessType}
import pages.register._
import pages.{WhatTypeBusinessPage, Page}
import play.api.mvc.Call

class PractitionerNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case WhatTypeBusinessPage => ua.get(WhatTypeBusinessPage) match {
      case Some(WhatTypeBusiness.Companyorpartnership) =>
        controllers.register.routes.WhatYouWillNeedController.onPageLoad()
      case Some(WhatTypeBusiness.Yourselfasindividual) =>
        controllers.individual.routes.WhatYouWillNeedController.onPageLoad()
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
    case WhatYouWillNeedPage => controllers.register.routes.AreYouUKCompanyController.onPageLoad()
    case AreYouUKCompanyPage =>
      ua.get(AreYouUKCompanyPage) match {
        case Some(true) =>
          controllers.register.routes.BusinessTypeController.onPageLoad()
        case _ => controllers.register.routes.BusinessRegistrationTypeController.onPageLoad()
      }
    case BusinessTypePage=> businessTypeNavigation(ua)
    case BusinessDetailsNotFoundPage => controllers.routes.WhatTypeBusinessController.onPageLoad()
    case BusinessRegistrationTypePage =>
      ua.get(BusinessRegistrationTypePage) match {
        case Some(BusinessRegistrationType.Company) => controllers.company.routes.CompanyNameController.onPageLoad(NormalMode)
        case _ => controllers.partnership.routes.PartnershipNameController.onPageLoad(NormalMode)
      }

  }

  private def businessTypeNavigation(ua:UserAnswers):Call = {
    ua.get(BusinessTypePage) match {
      case Some(BusinessType.LimitedCompany) =>
        controllers.company.routes.BusinessUTRController.onPageLoad()
      case Some(BusinessType.UnlimitedCompany) =>
        controllers.company.routes.BusinessUTRController.onPageLoad()
      case Some(BusinessType.BusinessPartnership) =>
        controllers.partnership.routes.BusinessUTRController.onPageLoad()
      case Some(BusinessType.LimitedPartnership) =>
        controllers.partnership.routes.BusinessUTRController.onPageLoad()
      case Some(BusinessType.LimitedLiabilityPartnership) =>
        controllers.partnership.routes.BusinessUTRController.onPageLoad()
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case WhatTypeBusinessPage => controllers.routes.SessionExpiredController.onPageLoad()
  }
}
