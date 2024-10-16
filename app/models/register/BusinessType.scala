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
import utils.{Enumerable, InputOption, WithName}

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

  def radios(form: Form[_])(implicit messages: Messages): Seq[RadioItem] = {

    val field = form("value")
    Seq(
      RadioItem(content = Text(Messages("whatTypeBusiness.limitedCompany")),
        value = Some(LimitedCompany.toString), checked = field.value.contains(LimitedCompany.toString), id = Some(field.id)),
      RadioItem(content = Text(Messages("whatTypeBusiness.businessPartnership")),
        value = Some(BusinessPartnership.toString), checked = field.value.contains(BusinessPartnership.toString), id = Some(field.id + "_1")),
      RadioItem(content = Text(Messages("whatTypeBusiness.limitedPartnership")),
        value = Some(LimitedPartnership.toString), checked = field.value.contains(LimitedPartnership.toString), id = Some(field.id + "_2")),
      RadioItem(content = Text(Messages("whatTypeBusiness.limitedLiabilityPartnership")),
        value = Some(LimitedLiabilityPartnership.toString), checked = field.value.contains(LimitedLiabilityPartnership.toString), id = Some(field.id + "_3")),
      RadioItem(content = Text(Messages("whatTypeBusiness.unlimitedCompany")),
        value = Some(UnlimitedCompany.toString), checked = field.value.contains(UnlimitedCompany.toString), id = Some(field.id + "_4"))
    )
  }

  implicit val enumerable: Enumerable[BusinessType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
