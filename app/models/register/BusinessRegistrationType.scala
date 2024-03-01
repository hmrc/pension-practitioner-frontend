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
import uk.gov.hmrc.viewmodels._
import utils.{Enumerable, WithName}

sealed trait BusinessRegistrationType

object BusinessRegistrationType extends Enumerable.Implicits {

  case object Company extends WithName("company") with BusinessRegistrationType
  case object Partnership extends WithName("partnership") with BusinessRegistrationType

  val values: Seq[BusinessRegistrationType] = Seq(
    Company,
    Partnership
  )

  def radios(form: Form[_]): Seq[Radios.Item] = {

    val field = form("value")
    val items = Seq(
      Radios.Radio(msg"businessRegistrationType.company", Company.toString),
      Radios.Radio(msg"businessRegistrationType.partnership", Partnership.toString)
    )

    Radios(field, items)
  }

  implicit val enumerable: Enumerable[BusinessRegistrationType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
