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
import models.register._
import pages.PageConstants
import pages.WhatTypeBusinessPage
import pages.behaviours.PageBehaviours

class BusinessTypePageSpec extends PageBehaviours {
  "BusinessTypePage" - {

    beRetrievable[BusinessType](BusinessTypePage)

    beSettable[BusinessType](BusinessTypePage)

    beRemovable[BusinessType](BusinessTypePage)

    "must clean up when set to LimitedCompany when have completed an individual UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualUK.setOrException(BusinessTypePage, BusinessType.LimitedCompany)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualUK) must be (true)
    }

    "must clean up when set to LimitedCompany when have completed an individual non UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualNonUK.setOrException(BusinessTypePage, BusinessType.LimitedCompany)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualNonUK) must be (true)
    }

    "must clean up when set to LimitedCompany when have completed an partnership UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipUK.setOrException(BusinessTypePage, BusinessType.LimitedCompany)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipUK - BusinessTypePage) must be (true)
    }

    "must clean up when set to LimitedCompany when have completed an partnership non UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipNonUK.setOrException(BusinessTypePage, BusinessType.LimitedCompany)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipNonUK) must be (true)
    }

    "must clean up when set to LimitedPartnership when have completed an company UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyUK.setOrException(BusinessTypePage, BusinessType.LimitedPartnership)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyUK - BusinessTypePage) must be (true)
    }

    "must clean up when set to LimitedPartnership when have completed an company non UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyNonUK.setOrException(BusinessTypePage, BusinessType.LimitedPartnership)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyNonUK) must be (true)
    }
  }
}
