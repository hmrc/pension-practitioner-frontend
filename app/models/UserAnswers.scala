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

import pages._
import play.api.libs.json._

import scala.util.Failure
import scala.util.Success
import scala.util.Try

final case class UserAnswers(data: JsObject = Json.obj()) {

  def get[A](page: QuestionPage[A])(implicit rds: Reads[A]): Option[A] = Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def get(path: JsPath)(implicit rds: Reads[JsValue]): Option[JsValue] = Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  def getOrException[A](page: QuestionPage[A])(implicit rds: Reads[A]): A =
    get(page).getOrElse(throw new RuntimeException("Expected a value but none found for " + page))

  def set[A](page: QuestionPage[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) => Success(jsValue)
      case JsError(errors) => Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      (page.path.asSingleJsResult(data).asOpt, page.path.asSingleJsResult(d).asOpt) match {
        case (Some(o), Some(n)) if o == n => Try(updatedAnswers)
        case _ => page.cleanup(Some(value), updatedAnswers)
      }
    }
  }

  def set(path: JsPath, value: JsValue): Try[UserAnswers] = {

    val updatedData = data.setObject(path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) => Success(jsValue)
      case JsError(errors) => Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      Success(updatedAnswers)
    }
  }

  def setOrException(path: JsPath, value: JsValue): UserAnswers = set(path, value) match {
    case Success(ua) => ua
    case Failure(ex) => throw ex
  }


  def setOrException[A](page: QuestionPage[A], value: A)(implicit writes: Writes[A], rds: Reads[A]): UserAnswers = {
    set(page, value) match {
      case Success(ua) => ua
      case Failure(ex) => throw ex
    }
  }

  def removeWithPath(path: JsPath): UserAnswers = {
    data.removeObject(path) match {
      case JsSuccess(jsValue, _) => UserAnswers(jsValue)
      case JsError(_) => throw new RuntimeException("Unable to remove with path: " + path)
    }
  }

  def removeOrException[A](page: QuestionPage[A]): UserAnswers = {
    remove(page) match {
      case Success(ua) => ua
      case Failure(ex) => throw ex
    }
  }

  def remove[A](page: QuestionPage[A]): Try[UserAnswers] = {
    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) => Success(jsValue)
      case JsError(_) => throw new RuntimeException("Unable to remove page: " + page)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(None, updatedAnswers)
    }
  }

}


