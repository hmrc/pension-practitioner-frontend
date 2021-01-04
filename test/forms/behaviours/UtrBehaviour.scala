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

package forms.behaviours

import forms.FormSpec
import forms.mappings.UtrMapping
import generators.Generators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.api.data.FormError

trait UtrBehaviour extends FormSpec with UtrMapping with ScalaCheckPropertyChecks with Generators with StringFieldBehaviours {

  //  scalastyle:off magic.number

  def formWithUniqueTaxReference[A](testForm: Form[A],
                                    fieldName: String,
                                    requiredKey: String,
                                    invalidKey: String): Unit = {

    val utrRegex = """^\d{10}$"""

    "behave like form with UTR" must {

    "bind valid data" in {
          val result = testForm.bind(Map(fieldName -> "1234567890")).apply(fieldName)
          result.errors shouldBe empty
      }

      behave like mandatoryField(
        testForm,
        fieldName,
        FormError(fieldName, requiredKey)
      )

      behave like fieldWithRegex(
        testForm,
        fieldName,
        invalidString = "AB12344555",
        FormError(fieldName, invalidKey, Seq(utrRegex))
      )
    }
  }
}