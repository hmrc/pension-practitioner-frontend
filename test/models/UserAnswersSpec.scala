/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import org.scalatest.FreeSpec
import org.scalatest.Matchers.convertToAnyShouldWrapper
import pages.QuestionPage
import pages.company.CompanyEmailPage
import play.api.libs.json.JsPath

import scala.util.{Success, Try}

class UserAnswersSpec extends FreeSpec {

  private case object DummyStringPage extends QuestionPage[String] {
    override def path: JsPath = JsPath \ toString
    override def toString: String = "xyz"
    override def cleanup(value: Option[String], userAnswers: UserAnswers): Try[UserAnswers] = {
      val result = userAnswers.remove(CompanyEmailPage) match {
        case Success(ua) => ua
        case _ => userAnswers
      }
      super.cleanup(value, result)
    }
  }

  private case object DummyBooleanPage extends QuestionPage[Boolean] {
    override def path: JsPath = JsPath \ toString
    override def toString: String = "abc"
    override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
      val result = userAnswers.remove(CompanyEmailPage) match {
        case Success(ua) => ua
        case _ => userAnswers
      }
      super.cleanup(value, result)
    }
  }

  "set with a string value" - {
    "must NOT cleanup relevant previous values when value not changed" in {

      val ua = UserAnswers()
        .setOrException(DummyStringPage, "hello")
        .setOrException(CompanyEmailPage, "email")

      val result = ua.set(DummyStringPage, "hello").toOption.get
      result.get(CompanyEmailPage) shouldBe Some("email")
    }

    "must cleanup relevant previous values when value changed" in {

      val ua = UserAnswers()
        .setOrException(DummyStringPage, "hello")
        .setOrException(CompanyEmailPage, "email")

      val result = ua.set(DummyStringPage, "goodbye").toOption.get
      result.get(CompanyEmailPage) shouldBe None
    }
  }

  "set with a boolean value" - {
    "must NOT cleanup relevant previous values when value not changed" in {

      val ua = UserAnswers()
        .setOrException(DummyBooleanPage, false)
        .setOrException(CompanyEmailPage, "email")

      val result = ua.set(DummyBooleanPage, false).toOption.get
      result.get(CompanyEmailPage) shouldBe Some("email")
    }

    "must cleanup relevant previous values when value changed" in {

      val ua = UserAnswers()
        .setOrException(DummyBooleanPage, false)
        .setOrException(CompanyEmailPage, "email")

      val result = ua.set(DummyBooleanPage, true).toOption.get
      result.get(CompanyEmailPage) shouldBe None
    }
  }
}
