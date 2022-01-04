/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.individual

import forms.mappings.{Transforms, Mappings}
import javax.inject.Inject
import models.register.TolerantIndividual
import play.api.data.Form
import play.api.data.Forms.mapping

class IndividualNameFormProvider @Inject() extends Mappings with Transforms {
  val nameLength: Int = 35

  def apply(): Form[TolerantIndividual] = Form(
    mapping(
      "firstName" ->
        text("individual.firstName.error.required")
          .verifying(
            firstError(
              maxLength(nameLength,
                "individual.firstName.error.length"
              ),
              name("individual.firstName.error.invalid")
            )
          ),
      "lastName" ->
        text("individual.lastName.error.required")
          .verifying(
            firstError(
              maxLength(nameLength,
                "individual.lastName.error.length"
              ),
              name("individual.lastName.error.invalid")
            )
          )
    )(TolerantIndividual.applyNonUK)(TolerantIndividual.unapplyNonUK)
  )
}
