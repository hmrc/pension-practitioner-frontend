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

import play.api.libs.json.{JsObject, JsValue, Json, Reads, __}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import scala.language.postfixOps

case class PSPAmendment(
                         pspId: String,
                         originalSubscriptionDetails: JsValue,
                         updatedSubscriptionDetails: JsValue
                       ) extends AuditEvent {

  override def auditType: String = "PensionSchemePractitionerAmendment"

  val from: JsObject =
    originalSubscriptionDetails.as[JsObject]
      .-("subscriptionType")

  val to: JsObject =
    updatedSubscriptionDetails.as[JsObject]
      .-("pspId")
      .-("subscriptionType")
      .-("areYouUKResident")

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  private val expandAcronymTransformer: JsValue => JsObject =
    json => json.as[JsObject].transform(
      __.json.update(
        (
          (__ \ "existingPensionSchemePractitioner").json.copyFrom(
            (__ \ "existingPSP").json.pick
          ) and
            (__ \ "existingPensionSchemePractitioner" \ "isExistingPensionSchemePractitioner").json.copyFrom(
                (__ \ "existingPSP" \ "isExistingPSP").json.pick
            ) and
            ((__ \ "existingPensionSchemePractitioner" \ "existingPensionSchemePractitionerId").json.copyFrom(
              (__ \ "existingPSP" \ "existingPSPId").json.pick) orElse doNothing)
          ) reduce
      ) andThen
        (__ \ "existingPSP").json.prune andThen
        (__ \ "existingPensionSchemePractitioner" \ "isExistingPSP").json.prune andThen
        (__ \ "existingPensionSchemePractitioner" \ "existingPSPId").json.prune
    ).getOrElse(throw ExpandAcronymTransformerFailed)

  case object ExpandAcronymTransformerFailed extends Exception

  def returnMismatches(left: JsObject, right: JsObject): collection.Set[String] =
    left.keys.filter(key => (left \ key) != (right \ key))

  override def details: Map[String, String] =
    Map(
      "pensionSchemePractitionerId" -> pspId,
      "from" -> s"${if (from == to) "-" else s"${Json.prettyPrint(expandAcronymTransformer(from))}"}",
      "to" -> s"${if (from == to) "-" else s"${Json.prettyPrint(expandAcronymTransformer(to))}"}"
    )

  println(s"\n\n\n\n$details\n\n\n\n\n")
  println(s"\n\n\nmismatches\n${returnMismatches(from, to)}\n\n\n\n\n")
}

