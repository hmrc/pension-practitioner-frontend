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

package pages.individual

import data.SampleData
import pages.behaviours.PageBehaviours
import pages.{PageConstants, WhatTypeBusinessPage}
import queries.Gettable

class AreYouUKResidentPageSpec extends PageBehaviours {

  private val pagesNotToRemove = Set[Gettable[_]](WhatTypeBusinessPage, AreYouUKResidentPage)

  "AreYouUKResidentPage" - {
    "must clean up when set to false when have completed an individual UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualUK
        .setOrException(AreYouUKResidentPage, true)
        .setOrException(AreYouUKResidentPage, false)
      areAllPagesNonEmpty(result, pagesNotToRemove)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualUK -- pagesNotToRemove) must be (true)
    }

    "must clean up when set to true when have completed an individual non UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualNonUK
        .setOrException(AreYouUKResidentPage, false)
        .setOrException(AreYouUKResidentPage, true)
      areAllPagesNonEmpty(result, pagesNotToRemove)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualNonUK -- pagesNotToRemove) must be (true)
    }
  }
}
