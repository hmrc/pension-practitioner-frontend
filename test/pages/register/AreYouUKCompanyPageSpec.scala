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

import data.SampleData
import pages.PageConstants
import pages.WhatTypeBusinessPage
import pages.behaviours.PageBehaviours


class AreYouUKCompanyPageSpec extends PageBehaviours {
  "AreYouUKCompanyPage" - {

    beRetrievable[Boolean](AreYouUKCompanyPage)

    beSettable[Boolean](AreYouUKCompanyPage)

    beRemovable[Boolean](AreYouUKCompanyPage)

    "must clean up when set to false when have completed an individual UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualUK.setOrException(AreYouUKCompanyPage, false)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.getOrException(AreYouUKCompanyPage) must be(false)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualUK) must be (true)
    }

    "must clean up when set to false when have completed an individual non-UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualNonUK.setOrException(AreYouUKCompanyPage, false)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.getOrException(AreYouUKCompanyPage) must be(false)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualNonUK) must be (true)
    }

    "must NOT clean up when set to true when have completed a company UK journey (because value not changed)" in {
      val result = SampleData.userAnswersFullJourneyCompanyUK.setOrException(AreYouUKCompanyPage, true)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.getOrException(AreYouUKCompanyPage) must be(true)
      areAllPagesNonEmpty(result, PageConstants.pagesFullJourneyCompanyUK) must be (true)
    }

    "must NOT clean up when set to true when have completed a partnership UK journey (because value not changed)" in {
      val result = SampleData.userAnswersFullJourneyPartnershipUK.setOrException(AreYouUKCompanyPage, true)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.getOrException(AreYouUKCompanyPage) must be(true)
      areAllPagesNonEmpty(result, PageConstants.pagesFullJourneyCompanyUK) must be (true)
    }

    "must clean up when set to true when have completed a company non-UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyNonUK.setOrException(AreYouUKCompanyPage, true)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.getOrException(AreYouUKCompanyPage) must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyNonUK - AreYouUKCompanyPage) must be (true)
    }

    "must clean up when set to true when have completed a partnership non-UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipNonUK.setOrException(AreYouUKCompanyPage, true)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.getOrException(AreYouUKCompanyPage) must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipNonUK - AreYouUKCompanyPage) must be (true)
    }
  }
}
