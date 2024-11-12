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

import play.api.data.validation.{Constraint, Invalid, Valid}
import utils.countryOptions.CountryOptions

import java.time.LocalDate

trait Constraints {
  private val regexPostcode = """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}$"""
  protected val nameRegex = """^[a-zA-Z &`\-\'\.^]{1,35}$"""
  val addressLineRegex = """^[A-Za-z0-9 \-,.&'\/]{1,35}$"""
  protected val utrRegex = """^\d{10}$"""
  protected val emailRestrictiveRegex: String = "^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
    "@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|" +
    "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$"

  protected val phoneNumberRegex = """^[0-9 ()+--]{1,24}$"""

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def postCode(errorKey: String): Constraint[String] = regexp(regexPostcode, errorKey)


  protected def inRange[A](minimum: A, maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum && input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, minimum, maximum)
        }
    }

  protected def regexp(regex: String, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.matches(regex) =>
        Valid
      case _ =>
        Invalid(errorKey, regex)
    }

  protected def maxLength(maximum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  protected def optionalMaxLength(maximum: Int, errorKey: String): Constraint[Option[String]] =
    Constraint {
      case Some(str) if str.length <= maximum =>
        Valid
      case None => Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def nonEmptySet(errorKey: String): Constraint[Set[_]] =
    Constraint {
      case set if set.nonEmpty =>
        Valid
      case _ =>
        Invalid(errorKey)
    }

  protected def emailAddressRestrictive(errorKey: String): Constraint[String] = regexp(emailRestrictiveRegex, errorKey)

  protected def phoneNumber(errorKey: String): Constraint[String] = regexp(phoneNumberRegex, errorKey)


  protected def validAddressLine(invalidKey: String): Constraint[String] = regexp(addressLineRegex, invalidKey)

  protected def optionalValidAddressLine(invalidKey: String): Constraint[Option[String]] = Constraint {
    case Some(str) if str.matches(addressLineRegex) =>
      Valid
    case None => Valid
    case _ =>
      Invalid(invalidKey, addressLineRegex)
  }

  protected def uniqueTaxReference(errorKey: String): Constraint[String] = regexp(utrRegex, errorKey)
  protected def country(countryOptions: CountryOptions, errorKey: String): Constraint[String] =
    Constraint {
      input =>
        countryOptions.options
          .find(_.value == input)
          .map(_ => Valid)
          .getOrElse(Invalid(errorKey))
    }

  protected def countryIsUK(errorKey: String): Constraint[String] =
    Constraint {
      code =>
        code match {
          case _ if code != "GB" => Invalid(errorKey)
          case _ => Valid
        }
    }

  protected def name(errorKey: String): Constraint[String] = regexp(nameRegex, errorKey)
}
