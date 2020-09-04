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

package pages.register

import models.UserAnswers
import pages.PageConstants
import pages.QuestionPage
import play.api.libs.json.JsPath

import scala.util.Try

case object AreYouUKCompanyPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "areYouUKCompany"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    val result = value match {
      case Some(true) => userAnswers
        .removeAllPages(
          PageConstants.pagesFullJourneyCompanyUK ++
            PageConstants.pagesFullJourneyCompanyNonUK ++
            PageConstants.pagesFullJourneyPartnershipNonUK - AreYouUKCompanyPage
        )
      case Some(false) => userAnswers
        .removeAllPages(
          PageConstants.pagesFullJourneyIndividualUK ++
          PageConstants.pagesFullJourneyIndividualNonUK  - AreYouUKCompanyPage
        )
      case _ => userAnswers
    }
    super.cleanup(value, result)
  }
}
