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

package forms

import forms.behaviours.UtrBehaviour

class BusinessUTRFormProviderSpec extends UtrBehaviour {
  val requiredKey = "businessUTR.company.error.required"
  val invalidKey = "businessUTR.company.error.invalid"

  private val fieldName: String = "value"

  "A form with a Utr" should {
    val testForm = (new BusinessUTRFormProvider).apply()

    behave like formWithUniqueTaxReference[String](
      testForm,
      fieldName = fieldName,
      requiredKey: String,
      invalidKey: String
    )

    "remove spaces for valid value" in {
      val actual = testForm.bind(Map(fieldName -> "  123 456 7890 "))
      actual.errors.isEmpty shouldBe true
      actual.value shouldBe Some("1234567890")
    }
  }
}
