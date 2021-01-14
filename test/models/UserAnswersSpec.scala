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

package models

import org.scalatest.FreeSpec
import org.scalatest.Matchers.convertToAnyShouldWrapper
import pages.QuestionPage
import pages.company.CompanyEmailPage
import play.api.libs.json.{Format, JsPath, Json}

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

  private case class DummyModel(a: String, b: Int, c: BigDecimal)

  private object DummyModel {
    implicit lazy val formats: Format[DummyModel] = Json.format[DummyModel]
  }

  private case object DummyModelPage extends QuestionPage[DummyModel] {
    override def path: JsPath = JsPath \ toString

    override def toString: String = "def"

    override def cleanup(value: Option[DummyModel], userAnswers: UserAnswers): Try[UserAnswers] = {
      val result = userAnswers.remove(CompanyEmailPage) match {
        case Success(ua) => ua
        case _ => userAnswers
      }
      super.cleanup(value, result)
    }
  }

  //scalastyle.off magic.number
  private val dummyModelA = new DummyModel("", 8, BigDecimal(5.55))
  private val dummyModelB = new DummyModel("", 8, BigDecimal(5.55))
  private val dummyModelC = new DummyModel("", 9, BigDecimal(5.55))

  private val email = "x@x.com"
  private val stringValue = "aaaa"
  private val stringValueNew = "bbbbb"

  "set with a string value" - {
    "must NOT cleanup relevant previous values when value not changed" in {
      val ua = UserAnswers().setOrException(DummyStringPage, stringValue).setOrException(CompanyEmailPage, email)

      val result = ua.set(DummyStringPage, stringValue).toOption.get
      result.get(CompanyEmailPage) shouldBe Some(email)
    }

    "must cleanup relevant previous values when value changed" in {
      val ua = UserAnswers().setOrException(DummyStringPage, stringValue).setOrException(CompanyEmailPage, email)

      val result = ua.set(DummyStringPage, stringValueNew).toOption.get
      result.get(CompanyEmailPage) shouldBe None
    }
  }

  "set with a boolean value" - {
    "must NOT cleanup relevant previous values when value not changed" in {
      val ua = UserAnswers().setOrException(DummyBooleanPage, false).setOrException(CompanyEmailPage, email)

      val result = ua.set(DummyBooleanPage, false).toOption.get
      result.get(CompanyEmailPage) shouldBe Some(email)
    }

    "must cleanup relevant previous values when value changed" in {
      val ua = UserAnswers().setOrException(DummyBooleanPage, false).setOrException(CompanyEmailPage, email)

      val result = ua.set(DummyBooleanPage, true).toOption.get
      result.get(CompanyEmailPage) shouldBe None
    }
  }

  "set with a composite object value" - {
    "must NOT cleanup relevant previous values when value not changed" in {
      val ua = UserAnswers().setOrException(DummyModelPage, dummyModelA).setOrException(CompanyEmailPage, email)

      val result = ua.set(DummyModelPage, dummyModelB).toOption.get
      result.get(CompanyEmailPage) shouldBe Some(email)
    }

    "must cleanup relevant previous values when value changed" in {
      val ua = UserAnswers().setOrException(DummyModelPage, dummyModelA).setOrException(CompanyEmailPage, email)

      val result = ua.set(DummyModelPage, dummyModelC).toOption.get
      result.get(CompanyEmailPage) shouldBe None
    }
  }

  "removeAllPages" - {
    "must remove all the pages specified" in {
      val result = UserAnswers()
        .setOrException(DummyStringPage, stringValue)
        .setOrException(DummyBooleanPage, true)
        .setOrException(DummyModelPage, dummyModelA)
        .removeAllPages(Set(DummyStringPage, DummyModelPage))
      result.get(DummyStringPage) shouldBe None
      result.get(DummyBooleanPage) shouldBe Some(true)
      result.get(DummyModelPage) shouldBe None
    }
  }
}
