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

package models.register

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.RadioItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.{Enumerable, WithName}

sealed trait BusinessRegistrationType

object BusinessRegistrationType extends Enumerable.Implicits {

  case object Company extends WithName("company") with BusinessRegistrationType
  case object Partnership extends WithName("partnership") with BusinessRegistrationType

  val values: Seq[BusinessRegistrationType] = Seq(
    Company,
    Partnership
  )

  def radios(form: Form[?])(implicit messages: Messages): Seq[RadioItem] = {
    val field = form("value")
    Seq(
      RadioItem(content = Text(Messages("businessRegistrationType.company")),
        value = Some(Company.toString), checked = field.value.contains(Company.toString), id = Some(field.id)),
      RadioItem(content = Text(Messages("businessRegistrationType.partnership")),
        value = Some(Partnership.toString), checked = field.value.contains(Partnership.toString), id = Some(field.id + "_1"))
    )
  }

  implicit val enumerable: Enumerable[BusinessRegistrationType] =
    Enumerable(values.map(v => v.toString -> v)*)
}
