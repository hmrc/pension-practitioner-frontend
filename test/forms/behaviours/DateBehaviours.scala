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

package forms.behaviours

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalacheck.Gen
import play.api.data.{Form, FormError}

trait DateBehaviours extends FieldBehaviours {

  def dateField(form: Form[_], key: String, validData: Gen[LocalDate]): Unit = {

    "should bind valid data" in {

      forAll(validData -> "valid date") {
        date =>

          val data = Map(
            s"$key.day" -> date.getDayOfMonth.toString,
            s"$key.month" -> date.getMonthValue.toString,
            s"$key.year" -> date.getYear.toString
          )

          val result = form.bind(data)

          result.value.value shouldEqual date
      }
    }
  }

  def dateFieldInvalid(form: Form[_], key: String, formError: FormError): Unit = {
    s"should fail to bind dates with invalid day" in {
      val genInt = intsAboveValue(31)
      forAll(genInt -> "invalid") { i =>
        val data = Map(
          s"$key.day" -> i.toString,
          s"$key.month" -> "12",
          s"$key.year" -> "2000"
        )

        val result = form.bind(data)

        result.errors should contain(formError)
      }
    }

    s"should fail to bind dates with invalid month" in {
      val genInt = intsAboveValue(12)
      forAll(genInt -> "invalid") { i =>
        val data = Map(
          s"$key.day" -> "1",
          s"$key.month" -> i.toString,
          s"$key.year" -> "2000"
        )

        val result = form.bind(data)

        result.errors should contain(formError)
      }
    }
  }

  def dateFieldDayMonthMissing(form: Form[_], key: String, formError: FormError): Unit = {
    s"should fail to bind a date with two date components missing" in {
      val data = Map(
        s"$key.year" -> "2000"
      )

      val result = form.bind(data)

      result.errors should contain(formError)
    }
  }

  def dateFieldYearNot4Digits(form: Form[_], key: String, formError: FormError): Unit = {
    s"should fail to bind a date where year less than 4 digits" in {
      val data = Map(
        s"$key.day" -> "20",
        s"$key.month" -> "12",
        s"$key.year" -> "200"
      )

      val result = form.bind(data)

      result.errors should contain(formError)
    }
  }

  def dateFieldWithMax(form: Form[_], key: String, max: LocalDate, formError: FormError): Unit = {

    s"should fail to bind a date greater than ${max.format(DateTimeFormatter.ISO_LOCAL_DATE)}" in {

      val generator = datesBetween(max.plusDays(1), max.plusYears(10))

      forAll(generator -> "invalid dates") {
        date =>

          val data = Map(
            s"$key.day" -> date.getDayOfMonth.toString,
            s"$key.month" -> date.getMonthValue.toString,
            s"$key.year" -> date.getYear.toString
          )

          val result = form.bind(data)

          result.errors should contain(formError)
      }
    }
  }

  def dateFieldWithMin(form: Form[_], key: String, min: LocalDate, formError: FormError): Unit = {

    s"should fail to bind a date earlier than ${min.format(DateTimeFormatter.ISO_LOCAL_DATE)}" in {

      val generator = datesBetween(min.minusYears(10), min.minusDays(1))

      forAll(generator -> "invalid dates") {
        date =>

          val data = Map(
            s"$key.day" -> date.getDayOfMonth.toString,
            s"$key.month" -> date.getMonthValue.toString,
            s"$key.year" -> date.getYear.toString
          )

          val result = form.bind(data)

          result.errors should contain(formError)
      }
    }
  }

  def mandatoryDateField(form: Form[_], key: String, requiredAllKey: String, errorArgs: Seq[String] = Seq.empty): Unit = {

    "should fail to bind an empty date" in {

      val result = form.bind(Map.empty[String, String])

      result.errors shouldBe List(FormError(key, requiredAllKey, Seq("day")),
        FormError(s"$key.month" , requiredAllKey, Seq("month")),
        FormError(s"$key.year", requiredAllKey, Seq("year")))
    }
  }
}
