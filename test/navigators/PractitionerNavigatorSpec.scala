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

import data.SampleData
import models.NormalMode
import models.UserAnswers
import models.WhatTypeBusiness
import models.register.BusinessType
import org.scalatest.prop.TableFor3
import pages._
import pages.register.BusinessTypePage
import pages.register.{AreYouUKCompanyPage, WhatYouWillNeedPage}
import play.api.mvc.Call

class PractitionerNavigatorSpec extends NavigatorBehaviour {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  private val uaCompanyOrPartnership = SampleData.emptyUserAnswers.setOrException(WhatTypeBusinessPage, WhatTypeBusiness.Companyorpartnership)
  private val uaInUk = SampleData.emptyUserAnswers.setOrException(AreYouUKCompanyPage, true)
  private val uaBusinessTypeLimitedCompany = SampleData.emptyUserAnswers.setOrException(BusinessTypePage, BusinessType.LimitedCompany)
  private val uaBusinessTypeUnlimitedCompany = SampleData.emptyUserAnswers.setOrException(BusinessTypePage, BusinessType.UnlimitedCompany)


  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(WhatTypeBusinessPage)(controllers.register.routes.WhatYouWillNeedController.onPageLoad(), Some(uaCompanyOrPartnership)),
        row(WhatYouWillNeedPage)(controllers.register.routes.AreYouUKCompanyController.onPageLoad()),
        row(AreYouUKCompanyPage)(controllers.register.routes.BusinessTypeController.onPageLoad(), Some(uaInUk)),
        row(BusinessTypePage)(controllers.register.company.routes.BusinessUTRController.onPageLoad(), Some(uaBusinessTypeLimitedCompany)),
        row(BusinessTypePage)(controllers.register.company.routes.BusinessUTRController.onPageLoad(), Some(uaBusinessTypeUnlimitedCompany))
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }
}
