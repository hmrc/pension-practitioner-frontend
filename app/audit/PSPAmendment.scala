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

package audit

import play.api.libs.json.Reads._
import play.api.libs.json._

case class PSPAmendment(
                         pspId: String,
                         originalSubscriptionDetails: JsValue,
                         updatedSubscriptionDetails: JsValue
                       ) extends ExtendedAuditEvent {

  override def auditType: String = "PensionSchemePractitionerAmendment"

  private val original: JsObject =
    originalSubscriptionDetails.as[JsObject] - "subscriptionType"

  private val updates: JsObject =
    updatedSubscriptionDetails.as[JsObject] - "pspId" - "subscriptionType" - "areYouUKResident"

  private def amendedKeys(
                           left: JsObject,
                           right: JsObject
                         ): collection.Set[String] =
    left.keys filter {
      key => (left \ key) != (right \ key)
    }

  private def fromToJson(
                          json: JsObject,
                          amendedKeys: collection.Set[String]
                        ): JsValue =
    Json.toJson(amendedKeys map {
      key =>
        Json.obj(key -> json.value(key))
    })

  override def details: JsObject = Json.obj(
    "pensionSchemePractitionerId" -> pspId,
    "from" -> fromToJson(original, amendedKeys(original, updates)),
    "to" -> fromToJson(updates, amendedKeys(original, updates))
  )
}

