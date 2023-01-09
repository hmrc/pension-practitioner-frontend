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

package models.register

import play.api.data.Form
import uk.gov.hmrc.viewmodels.{Radios, _}
import utils.{InputOption, WithName, Enumerable}

sealed trait BusinessType

object BusinessType extends Enumerable.Implicits {

  case object LimitedCompany extends WithName("limitedCompany") with BusinessType

  case object BusinessPartnership extends WithName("businessPartnership") with BusinessType

  case object LimitedPartnership extends WithName("limitedPartnership") with BusinessType

  case object LimitedLiabilityPartnership extends WithName("limitedLiabilityPartnership") with BusinessType

  case object UnlimitedCompany extends WithName("unlimitedCompany") with BusinessType

  case object OverseasCompany extends WithName("overseasCompany") with BusinessType

  val values: Seq[BusinessType] = Seq(
    LimitedCompany,
    BusinessPartnership,
    LimitedPartnership,
    LimitedLiabilityPartnership,
    UnlimitedCompany,
    OverseasCompany
  )

  def options: Seq[InputOption] =
    Seq(
      LimitedCompany,
      BusinessPartnership,
      LimitedPartnership,
      LimitedLiabilityPartnership,
      UnlimitedCompany
    ) map { value =>
      InputOption(value.toString, s"businessType.${value.toString}")
    }

  def radios(form: Form[_]): Seq[Radios.Item] = {

    val field = form("value")
    val items = Seq(
      Radios.Radio(msg"whatTypeBusiness.limitedCompany", LimitedCompany.toString),
      Radios.Radio(msg"whatTypeBusiness.businessPartnership", BusinessPartnership.toString),
      Radios.Radio(msg"whatTypeBusiness.limitedPartnership", LimitedPartnership.toString),
      Radios.Radio(msg"whatTypeBusiness.limitedLiabilityPartnership", LimitedLiabilityPartnership.toString),
      Radios.Radio(msg"whatTypeBusiness.unlimitedCompany", UnlimitedCompany.toString)
    )

    Radios(field, items)
  }

  implicit val enumerable: Enumerable[BusinessType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
