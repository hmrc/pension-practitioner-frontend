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
import models.register.BusinessType
import models.register.BusinessType.BusinessPartnership
import models.register.BusinessType.LimitedCompany
import models.register.BusinessType.LimitedLiabilityPartnership
import models.register.BusinessType.LimitedPartnership
import models.register.BusinessType.UnlimitedCompany
import pages.PageConstants
import pages.QuestionPage
import play.api.libs.json.JsPath

import scala.util.Try

case object BusinessTypePage extends QuestionPage[BusinessType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "businessType"

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
                PageConstants.pagesFullJourneyIndividualNonUK - BusinessTypePage
            )
        case Some(
            LimitedPartnership | LimitedLiabilityPartnership |
            BusinessPartnership
            ) =>
          userAnswers
            .removeAllPages(
              PageConstants.pagesFullJourneyCompanyNonUK ++
              PageConstants.pagesFullJourneyCompanyUK - BusinessTypePage
            )
        case _ => userAnswers
      }
    }
    super.cleanup(value, result)
  }
}
