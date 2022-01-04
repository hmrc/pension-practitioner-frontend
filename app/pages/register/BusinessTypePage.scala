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

package pages.register

import models.UserAnswers
import models.register.BusinessType
import models.register.BusinessType.{LimitedCompany, BusinessPartnership, LimitedLiabilityPartnership, LimitedPartnership, UnlimitedCompany}
import pages.{PageConstants, QuestionPage}
import play.api.libs.json.JsPath
import queries.Gettable

import scala.util.Try

case object BusinessTypePage extends QuestionPage[BusinessType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "businessType"

  private val pagesNotToRemove =
    Set[Gettable[_]](AreYouUKCompanyPage, BusinessTypePage)

  override def cleanup(value: Option[BusinessType],
                       userAnswers: UserAnswers): Try[UserAnswers] = {
    val result = {
      value match {
        case Some(LimitedCompany | UnlimitedCompany) =>
          userAnswers
            .removeAllPages(
              PageConstants.pagesFullJourneyPartnershipUK ++
                PageConstants.pagesFullJourneyPartnershipNonUK ++
                PageConstants.pagesFullJourneyIndividualUK ++
                PageConstants.pagesFullJourneyIndividualNonUK --
                pagesNotToRemove
            )
        case Some(
            LimitedPartnership | LimitedLiabilityPartnership |
            BusinessPartnership
            ) =>
          userAnswers
            .removeAllPages(
              PageConstants.pagesFullJourneyCompanyNonUK ++
                PageConstants.pagesFullJourneyCompanyUK ++
                PageConstants.pagesFullJourneyIndividualUK ++
                PageConstants.pagesFullJourneyIndividualNonUK --
                pagesNotToRemove
            )
        case _ => userAnswers
      }
    }
    super.cleanup(value, result)
  }
}
