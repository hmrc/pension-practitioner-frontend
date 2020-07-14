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

package data

import models.{TolerantAddress, UserAnswers}
import pages.company.BusinessNamePage

object SampleData {
  //scalastyle.off: magic.number
  val userAnswersId = "id"
  val psaId = "A0000000"
  val pspName = "psp"

  def emptyUserAnswers: UserAnswers = UserAnswers()

  def userAnswersWithCompanyName: UserAnswers =
    UserAnswers().setOrException(BusinessNamePage, pspName)

  val addressUK = TolerantAddress(Some("addr1"), Some("addr2"), None, None, Some(""), Some(""))
}
