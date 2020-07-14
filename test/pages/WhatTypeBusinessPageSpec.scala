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

package pages

import models.WhatTypeBusiness.{Companyorpartnership, Yourselfasindividual}
import models.register._
import models.{Address, TolerantAddress, UserAnswers, WhatTypeBusiness}
import pages.behaviours.PageBehaviours
import pages.register.{AreYouUKCompanyPage, BusinessTypePage}
import pages.company.{CompanyAddressListPage, CompanyAddressPage, CompanyEmailPage, CompanyPhonePage, CompanyPostcodePage, CompanyUseSameAddressPage, BusinessNamePage => CompanyNamePage, BusinessUTRPage => CompanyUTRPage, ConfirmAddressPage => ConfirmCompanyAddressPage, ConfirmNamePage => ConfirmCompanyNamePage}
import pages.individual._

class WhatTypeBusinessPageSpec extends PageBehaviours {
  private val tolerantAddress = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))
  private val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  "WhatTypeBusinessPage" - {

    beRetrievable[WhatTypeBusiness](WhatTypeBusinessPage)

    beSettable[WhatTypeBusiness](WhatTypeBusinessPage)

    beRemovable[WhatTypeBusiness](WhatTypeBusinessPage)

    "when selected as individual" - {

      "must clean up the data for all the Companyorpartnership" in {
        val ua: UserAnswers = UserAnswers().setOrException(WhatTypeBusinessPage, value = Companyorpartnership).
          setOrException(AreYouUKCompanyPage, true).
          setOrException(AreYouUKResidentPage, true).
          setOrException(BusinessTypePage, BusinessType.LimitedCompany).
          setOrException(CompanyUTRPage, "").
          setOrException(CompanyNamePage, "").
          setOrException(ConfirmCompanyNamePage, true).
          setOrException(ConfirmCompanyAddressPage, tolerantAddress).
          setOrException(CompanyAddressListPage, 0).
          setOrException(CompanyAddressPage, address).
          setOrException(CompanyEmailPage, "").
          setOrException(CompanyPhonePage, "").
          setOrException(CompanyPostcodePage, Seq(tolerantAddress)).
          setOrException(CompanyUseSameAddressPage, true).
          setOrException(RegistrationInfoPage, RegistrationInfo(RegistrationLegalStatus.Individual, "",
            noIdentifier = false, RegistrationCustomerType.UK,None, None
          ))
        val result = ua.set(WhatTypeBusinessPage, Yourselfasindividual).getOrElse(UserAnswers())

        result.get(AreYouUKCompanyPage) mustNot be(defined)
        result.get(BusinessTypePage) mustNot be(defined)
        result.get(CompanyUTRPage) mustNot be(defined)
        result.get(CompanyNamePage) mustNot be(defined)
        result.get(ConfirmCompanyNamePage) mustNot be(defined)
        result.get(ConfirmCompanyAddressPage) mustNot be(defined)
        result.get(CompanyAddressListPage) mustNot be(defined)
        result.get(CompanyAddressPage) mustNot be(defined)
        result.get(CompanyEmailPage) mustNot be(defined)
        result.get(CompanyPhonePage) mustNot be(defined)
        result.get(CompanyPostcodePage) mustNot be(defined)
        result.get(CompanyUseSameAddressPage) mustNot be(defined)
        result.get(RegistrationInfoPage) mustNot be(defined)

        result.get(AreYouUKResidentPage) must be(defined)
      }
    }

    "when selected as Companyorpartnership" - {
      "must clean up the data for all the individual" in {
        val ua: UserAnswers = UserAnswers().setOrException(WhatTypeBusinessPage, value = Yourselfasindividual).
          setOrException(AreYouUKResidentPage, true).
          setOrException(AreYouUKCompanyPage, true).
          setOrException(IndividualDetailsPage, TolerantIndividual(Some("first"), None, Some("last"))).
          setOrException(IndividualAddressPage, tolerantAddress).
          setOrException(IndividualEmailPage, "s@s.com").
          setOrException(IndividualPhonePage, "").
          setOrException(IndividualPostcodePage, Seq(tolerantAddress)).
          setOrException(IsThisYouPage, true).
          setOrException(IndividualAddressListPage, 1).
          setOrException(IndividualManualAddressPage, address).
          setOrException(RegistrationInfoPage, RegistrationInfo(RegistrationLegalStatus.Individual, "",
            noIdentifier = false, RegistrationCustomerType.UK,None, None
          ))

        val result = ua.set(WhatTypeBusinessPage, Companyorpartnership).getOrElse(UserAnswers())

        result.get(AreYouUKResidentPage) mustNot be(defined)
        result.get(IndividualDetailsPage) mustNot be(defined)
        result.get(IndividualAddressPage) mustNot be(defined)
        result.get(IndividualEmailPage) mustNot be(defined)
        result.get(IndividualPhonePage) mustNot be(defined)
        result.get(IndividualPostcodePage) mustNot be(defined)
        result.get(IsThisYouPage) mustNot be(defined)
        result.get(IndividualAddressListPage) mustNot be(defined)
        result.get(IndividualManualAddressPage) mustNot be(defined)
        result.get(RegistrationInfoPage) mustNot be(defined)

        result.get(AreYouUKCompanyPage) must be(defined)
      }
    }
  }
}
