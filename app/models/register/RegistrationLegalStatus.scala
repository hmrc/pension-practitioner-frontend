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

import utils.{Enumerable, WithName}

sealed trait RegistrationLegalStatus

object RegistrationLegalStatus extends Enumerable.Implicits {

  case object Individual extends WithName("Individual") with RegistrationLegalStatus

  case object Partnership extends WithName("Partnership") with RegistrationLegalStatus

  case object LimitedCompany extends WithName("Company") with RegistrationLegalStatus

  val values = Seq(
    Individual,
    Partnership,
    LimitedCompany
  )

  implicit val enumerable: Enumerable[RegistrationLegalStatus] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
