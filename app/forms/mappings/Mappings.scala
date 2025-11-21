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

package forms.mappings

import play.api.data.FieldMapping
import play.api.data.Forms.of
import utils.Enumerable

import java.time.LocalDate

trait Mappings extends Formatters with Constraints with Transforms {

  protected def optionalText(): FieldMapping[Option[String]] =
    of(using optionalStringFormatter)

  protected def text(errorKey: String = "error.required"): FieldMapping[String] =
    of(using stringFormatter(errorKey))

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric",
                    min: Option[(String, Int)] = None,
                    max: Option[(String, Int)] = None
                   ): FieldMapping[Int] =
    of(using intFormatter(requiredKey, wholeNumberKey, nonNumericKey, min, max))

  protected def bigDecimal(requiredKey: String = "error.required",
                           invalidKey: String = "error.invalid"
                          ): FieldMapping[BigDecimal] =
    of(using bigDecimalFormatter(requiredKey, invalidKey))

  protected def boolean(requiredKey: String = "error.required",
                        invalidKey: String = "error.boolean"): FieldMapping[Boolean] =
    of(using booleanFormatter(requiredKey, invalidKey))


  protected def enumerable[A](requiredKey: String = "error.required",
                              invalidKey: String = "error.invalid")(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(using enumerableFormatter[A](requiredKey, invalidKey)(using ev))

  protected def localDate(invalidKey: String,
                          allRequiredKey: String,
                          twoRequiredKey: String,
                          oneRequiredKey: String,
                          requiredKey: String,
                          args: Seq[String] = Seq.empty): FieldMapping[LocalDate] =
    of(using new LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, oneRequiredKey, requiredKey, args))
}
