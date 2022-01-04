/*
 * Copyright 2022 HM Revenue & Customs
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

package models.register

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.FreeSpec
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import play.api.libs.json.JsError
import play.api.libs.json.JsString
import play.api.libs.json.Json

class BusinessRegistrationTypeSpec extends FreeSpec with MustMatchers with ScalaCheckPropertyChecks with OptionValues {

  "BusinessRegistrationType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(BusinessRegistrationType.values)

      forAll(gen) {
        businessRegistrationType =>

          JsString(businessRegistrationType.toString).validate[BusinessRegistrationType].asOpt.value mustEqual businessRegistrationType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!BusinessRegistrationType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[BusinessRegistrationType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(BusinessRegistrationType.values)

      forAll(gen) {
        businessRegistrationType =>

          Json.toJson(businessRegistrationType) mustEqual JsString(businessRegistrationType.toString)
      }
    }
  }
}
