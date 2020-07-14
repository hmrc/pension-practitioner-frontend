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

package pages.register

import models.{TolerantAddress, UserAnswers}
import models.register._
import pages.RegistrationInfoPage
import pages.behaviours.PageBehaviours
import pages.company.{BusinessNamePage => CompanyNamePage, BusinessUTRPage => CompanyUTRPage, ConfirmAddressPage => ConfirmCompanyAddressPage, ConfirmNamePage => ConfirmCompanyNamePage}
import pages.partnership.{BusinessNamePage => PartnershipNamePage, BusinessUTRPage => PartnershipUTRPage, ConfirmAddressPage => ConfirmPartnershipAddressPage, ConfirmNamePage => ConfirmPartnershipNamePage}

class BusinessTypePageSpec extends PageBehaviours {
  private val tolerantAddress = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))

  "BusinessTypePage" - {

    beRetrievable[BusinessType](BusinessTypePage)

    beSettable[BusinessType](BusinessTypePage)

    beRemovable[BusinessType](BusinessTypePage)

    "when selected as company" - {
      "must clean up the data for all the partnership" in {
        val ua: UserAnswers = UserAnswers().setOrException(BusinessTypePage, value = BusinessType.LimitedPartnership).
          setOrException(PartnershipNamePage, "test-partnership").
          setOrException(PartnershipUTRPage, "1234").
          setOrException(ConfirmPartnershipNamePage, true).
          setOrException(ConfirmPartnershipAddressPage, tolerantAddress).
          setOrException(RegistrationInfoPage, RegistrationInfo(RegistrationLegalStatus.Individual, "",
            noIdentifier = false, RegistrationCustomerType.UK,None, None
          ))
        val result = ua.set(BusinessTypePage, BusinessType.LimitedCompany).getOrElse(UserAnswers())

        result.get(PartnershipNamePage) mustNot be(defined)
        result.get(PartnershipUTRPage) mustNot be(defined)
        result.get(ConfirmPartnershipNamePage) mustNot be(defined)
        result.get(ConfirmPartnershipAddressPage) mustNot be(defined)
        result.get(RegistrationInfoPage) mustNot be(defined)
      }
    }

    "when selected as partnership" - {
      "must clean up the data for all the company" in {
        val ua: UserAnswers = UserAnswers().setOrException(BusinessTypePage, value = BusinessType.LimitedCompany).
          setOrException(CompanyNamePage, "test-partnership").
          setOrException(CompanyUTRPage, "1234").
          setOrException(ConfirmCompanyNamePage, true).
          setOrException(ConfirmCompanyAddressPage, tolerantAddress).
          setOrException(RegistrationInfoPage, RegistrationInfo(RegistrationLegalStatus.Individual, "",
            noIdentifier = false, RegistrationCustomerType.UK, None, None
          ))
        val result = ua.set(BusinessTypePage, BusinessType.LimitedPartnership).getOrElse(UserAnswers())

        result.get(CompanyNamePage) mustNot be(defined)
        result.get(CompanyUTRPage) mustNot be(defined)
        result.get(ConfirmCompanyNamePage) mustNot be(defined)
        result.get(ConfirmCompanyAddressPage) mustNot be(defined)
        result.get(RegistrationInfoPage) mustNot be(defined)
      }
    }
  }
}
