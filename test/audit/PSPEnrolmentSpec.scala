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

package audit

import base.SpecBase

class PSPEnrolmentSpec extends SpecBase {

  private val userId = "user"
  private val pspId = "psp"

  "details" should {
    "return the correct values" in {
      val result = PSPEnrolment(userId, pspId)

      result.auditType mustBe "PensionSchemePractitionerEnrolment"

      result.details mustBe Map(
        "userId" -> userId,
        "pensionSchemePractitionerId" -> pspId
      )
    }
  }
}
