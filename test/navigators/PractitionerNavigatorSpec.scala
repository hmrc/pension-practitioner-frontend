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

package navigators

import data.SampleData
import models.NormalMode
import models.UserAnswers
import models.WhatTypeBusiness
import models.register.BusinessRegistrationType
import models.register.BusinessType
import org.scalatest.prop.TableFor3
import pages._
import pages.register.BusinessTypePage
import pages.register.AreYouUKCompanyPage
import pages.register.BusinessDetailsNotFoundPage
import pages.register.BusinessRegistrationTypePage
import pages.register.WhatYouWillNeedPage
import play.api.mvc.Call

class PractitionerNavigatorSpec extends NavigatorBehaviour {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  private val uaCompanyOrPartnership = SampleData.emptyUserAnswers.setOrException(WhatTypeBusinessPage, WhatTypeBusiness.Companyorpartnership)
  private def uaInUk(v:Boolean):UserAnswers = SampleData.emptyUserAnswers.setOrException(AreYouUKCompanyPage, v)
  private val uaBusinessTypeLimitedCompany = SampleData.emptyUserAnswers.setOrException(BusinessTypePage, BusinessType.LimitedCompany)
  private val uaBusinessTypeUnlimitedCompany = SampleData.emptyUserAnswers.setOrException(BusinessTypePage, BusinessType.UnlimitedCompany)
  private val uaIndividual = SampleData.emptyUserAnswers.setOrException(WhatTypeBusinessPage, WhatTypeBusiness.Yourselfasindividual)
  private val uaBusinessRegistrationTypeCompany = SampleData.emptyUserAnswers.setOrException(BusinessRegistrationTypePage, BusinessRegistrationType.Company)

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(WhatTypeBusinessPage)(controllers.register.routes.WhatYouWillNeedController.onPageLoad(), Some(uaCompanyOrPartnership)),
        row(WhatTypeBusinessPage)(controllers.individual.routes.WhatYouWillNeedController.onPageLoad(), Some(uaIndividual)),
        row(WhatYouWillNeedPage)(controllers.register.routes.AreYouUKCompanyController.onPageLoad()),
        row(AreYouUKCompanyPage)(controllers.register.routes.BusinessTypeController.onPageLoad(), Some(uaInUk(true))),
        row(AreYouUKCompanyPage)(controllers.register.routes.BusinessRegistrationTypeController.onPageLoad(), Some(uaInUk(false))),
        row(BusinessTypePage)(controllers.company.routes.BusinessUTRController.onPageLoad(), Some(uaBusinessTypeLimitedCompany)),
        row(BusinessTypePage)(controllers.company.routes.BusinessUTRController.onPageLoad(), Some(uaBusinessTypeUnlimitedCompany)),
        row(BusinessDetailsNotFoundPage)(controllers.routes.WhatTypeBusinessController.onPageLoad()),
        row(BusinessRegistrationTypePage)(controllers.company.routes.CompanyNameController.onPageLoad(NormalMode), Some(uaBusinessRegistrationTypeCompany)),
        row(BusinessRegistrationTypePage)(controllers.partnership.routes.PartnershipNameController.onPageLoad(NormalMode))
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }
}
