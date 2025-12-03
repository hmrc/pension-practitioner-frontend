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

package pages.register

import models.UserAnswers
import models.register.BusinessRegistrationType
import models.register.BusinessRegistrationType.{Company, Partnership}
import pages.{PageConstants, QuestionPage}
import play.api.libs.json.JsPath
import queries.Gettable

import scala.util.Try

case object BusinessRegistrationTypePage
    extends QuestionPage[BusinessRegistrationType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "businessRegistrationType"

  private val pagesNotToRemove =
    Set[Gettable[?]](AreYouUKCompanyPage, BusinessRegistrationTypePage)

  override def cleanup(value: Option[BusinessRegistrationType],
                       userAnswers: UserAnswers): Try[UserAnswers] = {
    val result = {
      value match {
        case Some(Company | Partnership) =>
          userAnswers
            .removeAllPages(
              PageConstants.pagesFullJourneyAll -- pagesNotToRemove
            )
        case _ => userAnswers
      }
    }
    super.cleanup(value, result)
  }
}
