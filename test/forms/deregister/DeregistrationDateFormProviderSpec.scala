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

package forms.deregister

import java.time.LocalDate

import forms.behaviours._
import play.api.data.FormError

class DeregistrationDateFormProviderSpec extends DateBehaviours {

  private val minDate: LocalDate = LocalDate.of(2020,2, 1)
  private val dynamicErrorMsg: String = messages("deregistrationDate.company.error.futureDate")
  private val minDateErrorMsg: String = messages("deregistrationDate.company.error.minDate", "1 February 2020")

  val form = new DeregistrationDateFormProvider()("company", minDate)
  val deRegDateMsgKey = "deregistrationDate.company"
  val deRegDateKey = "deregistrationDate"

  "deregistrationDate" must {

    behave like dateFieldWithMax(
      form = form,
      key = deRegDateKey,
      max = LocalDate.now.plusDays(1),
      formError = FormError(deRegDateKey, dynamicErrorMsg)
    )

    behave like dateFieldWithMin(
      form = form,
      key = deRegDateKey,
      min = minDate,
      formError = FormError(deRegDateKey, minDateErrorMsg)
    )

    behave like mandatoryDateField(
      form = form,
      key = deRegDateKey,
      requiredAllKey = s"$deRegDateMsgKey.error.required.all")
  }
}
