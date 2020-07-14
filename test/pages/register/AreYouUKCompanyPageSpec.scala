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

import models.{Address, TolerantAddress, UserAnswers}
import models.register.{BusinessType, RegistrationCustomerType, RegistrationInfo, RegistrationLegalStatus}
import pages.RegistrationInfoPage
import pages.behaviours.PageBehaviours
import pages.company.{CompanyAddressListPage, CompanyAddressPage, CompanyEmailPage, CompanyPhonePage, CompanyPostcodePage, CompanyUseSameAddressPage, BusinessNamePage => CompanyNamePage, BusinessUTRPage => CompanyUTRPage, ConfirmAddressPage => ConfirmCompanyAddressPage, ConfirmNamePage => ConfirmCompanyNamePage}
import pages.partnership.{BusinessNamePage => PartnershipNamePage, BusinessUTRPage => PartnershipUTRPage, ConfirmAddressPage => ConfirmPartnershipAddressPage, ConfirmNamePage => ConfirmPartnershipNamePage}


class AreYouUKCompanyPageSpec extends PageBehaviours {
  private val tolerantAddress = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))
  private val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  "AreYouUKCompanyPage" - {

    beRetrievable[Boolean](AreYouUKCompanyPage)

    beSettable[Boolean](AreYouUKCompanyPage)

    beRemovable[Boolean](AreYouUKCompanyPage)

    "when selected as non Uk" - {

      "must clean up the data for all the UK Company or partnership" in {
        val ua: UserAnswers = UserAnswers().setOrException(AreYouUKCompanyPage, value = true).
          setOrException(BusinessTypePage, BusinessType.LimitedPartnership).
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
          setOrException(PartnershipNamePage, "").
          setOrException(PartnershipUTRPage, "").
          setOrException(ConfirmPartnershipNamePage, true).
          setOrException(ConfirmPartnershipAddressPage, tolerantAddress).
          setOrException(RegistrationInfoPage, RegistrationInfo(RegistrationLegalStatus.Individual, "",
            noIdentifier = false, RegistrationCustomerType.UK, None, None
          ))
        val result = ua.set(AreYouUKCompanyPage, false).getOrElse(UserAnswers())

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
        result.get(PartnershipNamePage) mustNot be(defined)
        result.get(PartnershipUTRPage) mustNot be(defined)
        result.get(ConfirmPartnershipNamePage) mustNot be(defined)
        result.get(ConfirmPartnershipAddressPage) mustNot be(defined)
        result.get(RegistrationInfoPage) mustNot be(defined)
      }
    }
  }
}
