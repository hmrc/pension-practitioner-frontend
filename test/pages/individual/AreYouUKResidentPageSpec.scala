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

package pages.individual

import models.register.{RegistrationCustomerType, RegistrationInfo, RegistrationLegalStatus, TolerantIndividual}
import models.{Address, TolerantAddress, UserAnswers}
import pages.RegistrationInfoPage
import pages.behaviours.PageBehaviours

class AreYouUKResidentPageSpec extends PageBehaviours {

  private val tolerantAddress = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))
  private val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  private val ua: UserAnswers = UserAnswers().setOrException(UseAddressForContactPage, value = false).
  setOrException(IndividualDetailsPage, TolerantIndividual(Some("first"), None, Some("last"))).
  setOrException(AreYouUKResidentPage, true).
  setOrException(IndividualAddressPage, tolerantAddress).
  setOrException(IndividualEmailPage, "s@s.com").
  setOrException(RegistrationInfoPage, RegistrationInfo(RegistrationLegalStatus.Individual, "",
    noIdentifier = false, RegistrationCustomerType.UK,None, None
  )).
  setOrException(IndividualPhonePage, "1234").
  setOrException(IndividualPostcodePage, Seq(tolerantAddress)).
  setOrException(IsThisYouPage, true).
  setOrException(IndividualAddressListPage, 1).
  setOrException(IndividualManualAddressPage, address)

  "AreYouUKResidentPage" - {
    "must clean up the data for all the uk individual" in {
      val result = ua.set(AreYouUKResidentPage, value = false).getOrElse(UserAnswers())

      result.get(IndividualDetailsPage) mustNot be(defined)
      result.get(UseAddressForContactPage) mustNot be(defined)
      result.get(IndividualAddressPage) mustNot be(defined)
      result.get(IndividualEmailPage) mustNot be(defined)
      result.get(RegistrationInfoPage) mustNot be(defined)
      result.get(IndividualPhonePage) mustNot be(defined)
      result.get(IndividualPostcodePage) mustNot be(defined)
      result.get(IsThisYouPage) mustNot be(defined)
      result.get(IndividualAddressListPage) mustNot be(defined)
      result.get(IndividualManualAddressPage) mustNot be(defined)
    }
  }
}
