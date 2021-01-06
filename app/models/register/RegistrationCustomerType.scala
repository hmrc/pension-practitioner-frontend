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

package models.register

import models.TolerantAddress
import utils.{Enumerable, WithName}

sealed trait RegistrationCustomerType

object RegistrationCustomerType extends Enumerable.Implicits {

  case object UK extends WithName("UK") with RegistrationCustomerType

  case object NonUK extends WithName("NonUK") with RegistrationCustomerType

  def fromAddress(address: TolerantAddress): RegistrationCustomerType = {
    address.country match {
      case Some("GB") | Some("UK") => UK
      case Some(_) => NonUK
      case _ => throw new IllegalArgumentException(s"Cannot determine RegistrationCustomerType for country: ${address.country}")
    }
  }

  val values = Seq(
    UK,
    NonUK
  )

  implicit val enumerable: Enumerable[RegistrationCustomerType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
