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

package audit

import base.SpecBase
import play.api.libs.json.{JsArray, JsValue, Json}

class PSPAmendmentSpec extends SpecBase {

  private val inputJson: JsValue = Json.obj(
    "email" -> "abc@hmrc.gsi.gov.uk",
    "contactAddress" -> Json.obj(
      "country" -> "GB",
      "postcode" -> "TF3 4ER",
      "addressLine1" -> "24 Trinity Street",
      "addressLine2" -> "Telford",
      "addressLine3" -> "Shropshire"
    ),
    "individualDetails" -> Json.obj(
      "firstName" -> "Arthur Conan",
      "lastName" -> "Doyle"
    ),
    "phone" -> "0044-09876542312",
    "name" -> "Abc Private Ltd"
  )

  private val updateJson: JsValue = Json.obj(
    "email" -> "def@hmrc.gsi.gov.uk",
    "contactAddress" -> Json.obj(
      "country" -> "GB",
      "postcode" -> "TF3 4ER",
      "addressLine1" -> "123 Other Street",
      "addressLine2" -> "New Town",
      "addressLine3" -> "County"
    ),
    "individualDetails" -> Json.obj(
      "firstName" -> "Arthur Conan",
      "lastName" -> "Doyle"
    ),
    "phone" -> "0044-1234567890",
    "name" -> "Def Private Ltd"
  )

  private val amendment: PSPAmendment = PSPAmendment(
    pspId = "pspId",
    originalSubscriptionDetails = inputJson,
    updatedSubscriptionDetails = updateJson
  )

  private val amendmentNoChange: PSPAmendment = PSPAmendment(
    pspId = "pspId",
    originalSubscriptionDetails = inputJson,
    updatedSubscriptionDetails = inputJson
  )

  "Amendment audit" must {
    "detail from and to values when updates have been made" in {

      amendment.auditType mustBe "PensionSchemePractitionerAmendment"
      amendment.details("pensionSchemePractitionerId").as[String] mustBe "pspId"

      val fromJson: JsArray = amendment.details("from").as[JsArray]
      val toJson: JsArray = amendment.details("to").as[JsArray]

      (fromJson.value(3) \ "name").as[String] mustBe "Abc Private Ltd"
      (fromJson.head \ "email").as[String] mustBe "abc@hmrc.gsi.gov.uk"
      (fromJson.value(1) \ "contactAddress" \ "addressLine1").as[String] mustBe "24 Trinity Street"
      (fromJson.value(2) \ "phone").as[String] mustBe "0044-09876542312"

      (toJson.value(3) \ "name").as[String] mustBe "Def Private Ltd"
      (toJson.head \ "email").as[String] mustBe "def@hmrc.gsi.gov.uk"
      (toJson.value(1) \ "contactAddress" \ "addressLine1").as[String] mustBe "123 Other Street"
      (toJson.value(2) \ "phone").as[String] mustBe "0044-1234567890"
    }

    "audit \"no changes made\" when no updates are made" in {
      amendmentNoChange.auditType mustBe "PensionSchemePractitionerAmendment"
      amendmentNoChange.details("pensionSchemePractitionerId").as[String] mustBe "pspId"

      amendmentNoChange.details("from").as[JsArray] mustBe JsArray.empty
      amendmentNoChange.details("to").as[JsArray] mustBe JsArray.empty
    }
  }
}
