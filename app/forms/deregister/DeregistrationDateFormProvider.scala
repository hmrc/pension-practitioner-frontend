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

package forms.deregister

import java.time.LocalDate

import forms.mappings.{Constraints, Mappings}
import helpers.FormatHelper.dateContentFormatter
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.Messages
class DeregistrationDateFormProvider @Inject() extends Mappings with Constraints {

  def apply(pspType: String, registrationDate: LocalDate)(implicit messages: Messages): Form[LocalDate] =
    Form(
      "deregistrationDate" -> localDate(
        invalidKey = s"deregistrationDate.$pspType.error.invalid",
        allRequiredKey = s"deregistrationDate.$pspType.error.required.all",
        twoRequiredKey = s"deregistrationDate.$pspType.error.required.two",
        oneRequiredKey = s"deregistrationDate.$pspType.error.required.one",
        requiredKey = s"deregistrationDate.$pspType.error.required.all"
      ).verifying(
        maxDate(LocalDate.now(), messages(s"deregistrationDate.$pspType.error.futureDate")),
        minDate(registrationDate, messages(s"deregistrationDate.$pspType.error.minDate", registrationDate.format(dateContentFormatter)))
      )
    )
}
