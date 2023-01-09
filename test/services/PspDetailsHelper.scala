/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import play.api.libs.json.{JsObject, Json}

object PspDetailsHelper {

  private val individualDetails: JsObject = Json.obj(
    "individualDetails" -> Json.obj(
      "firstName" -> "Stephen",
      "lastName" -> "Wood"
    )
  )

  private val uaAddress: JsObject = Json.obj( "contactAddress" -> Json.obj(
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "postcode" -> "ZZ1 1ZZ",
    "country" -> "GB"
  ))

  private val uaAddressNonUk: JsObject = Json.obj( "contactAddress" -> Json.obj(
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "country" -> "FR"
  ))

  private val uaContactDetails: JsObject = Json.obj(
    "email" -> "sdd@ds.sd",
    "phone" -> "3445"
  )

  private val existingPsp: JsObject = Json.obj(
    "existingPSP" -> Json.obj(
      "existingPSPId" -> "A2345678",
      "isExistingPSP" -> "Yes"
    )
  )

  val uaIndividualUK: JsObject = Json.obj(
    "registrationInfo" -> Json.obj(
      "legalStatus" -> "Individual",
      "customerType" -> "UK",
      "idType" -> "NINO",
      "idNumber" -> "AB123456C"
    )
  ) ++ individualDetails ++ uaAddress ++ uaContactDetails ++ existingPsp

  val uaIndividualNonUk: JsObject = Json.obj(
    "registrationInfo"  -> Json.obj(
      "legalStatus"  ->  "Individual",
      "customerType"  ->  "NonUK"
    )
  ) ++ individualDetails ++ uaAddressNonUk ++ uaContactDetails ++ existingPsp

  val uaCompanyUk: JsObject = Json.obj(
    "registrationInfo"  ->  Json.obj(
      "legalStatus"  ->  "Company",
      "customerType"  ->  "UK",
      "idType"  ->  "UTR",
      "idNumber"  ->  "1234567890"
    ),
    "name"  ->  "Test Ltd"
  ) ++ uaAddress ++ uaContactDetails ++ existingPsp

  val uaPartnershipNonUK: JsObject = Json.obj(
    "registrationInfo"  ->  Json.obj(
      "legalStatus"  ->  "Partnership",
      "customerType"  ->  "NonUK"
    ),
    "name"  ->  "Testing Ltd"
  ) ++ uaAddressNonUk ++ uaContactDetails ++ existingPsp

}
