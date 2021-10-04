/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{Address, TolerantAddress, UserAnswers}
import pages.behaviours.PageBehaviours

class UseAddressForContactPageSpec extends PageBehaviours {

  private val tolerantAddress = Seq(TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB")))
  private val tolerantAddress1 = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))
  private val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  private val ua: UserAnswers = UserAnswers().set(UseAddressForContactPage, value = false).
    flatMap(_.set(IndividualPostcodePage, tolerantAddress)).
    flatMap(_.set(IndividualAddressListPage, tolerantAddress1)
    flatMap(_.set(IndividualManualAddressPage, address))).getOrElse(UserAnswers())

  "UseAddressForContactPage" - {
    "must clean up the data for address if selected yes" in {
      val result = ua.set(UseAddressForContactPage, value = true).getOrElse(UserAnswers())

      result.get(IndividualPostcodePage) mustNot be(defined)
      result.get(IndividualAddressListPage) mustNot be(defined)

      result.get(IndividualManualAddressPage) must be(defined)
    }

    "must not clean up the data if selected no" in {
      val result = ua.set(UseAddressForContactPage, value = false).getOrElse(UserAnswers())

      result.get(IndividualPostcodePage) must be(defined)
      result.get(IndividualAddressListPage) must be(defined)
      result.get(IndividualManualAddressPage) must be(defined)
    }
  }
}
