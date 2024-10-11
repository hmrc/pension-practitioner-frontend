/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json, OWrites}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow

case class PspDetailsData(
                           pageTitle: String,
                           heading: String,
                           list: Seq[SummaryListRow],
                           returnLinkAndUrl: Option[(String, String)],
                           displayContinueButton: Boolean,
                           nextPage: String
                         ) {
  def toJson(implicit messages: Messages): JsObject = {
    implicit val rowWrites: OWrites[SummaryListRow] = SummaryListRow.jsonFormats.writes
    implicit val pspJsonFormat: OWrites[PspDetailsDataJsonObject] = Json.writes[PspDetailsDataJsonObject]
    Json.toJson(PspDetailsDataJsonObject(
      pageTitle,
      heading,
      list,
      returnLinkAndUrl.map(_._1),
      returnLinkAndUrl.map(_._2),
      displayContinueButton,
      nextPage
    )).as[JsObject]
  }
}

private case class PspDetailsDataJsonObject(
                                             pageTitle: String,
                                             heading: String,
                                             list: Seq[SummaryListRow],
                                             returnLink: Option[String],
                                             returnUrl: Option[String],
                                             displayContinueButton: Boolean,
                                             nextPage: String
                                           )