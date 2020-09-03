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
import models.register.BusinessRegistrationType
import pages.{PageConstants, WhatTypeBusinessPage}
import pages.behaviours.PageBehaviours

class BusinessRegistrationTypePageSpec extends PageBehaviours {

  "BusinessRegistrationTypePage" - {

    beRetrievable[BusinessRegistrationType](BusinessRegistrationTypePage)

    beSettable[BusinessRegistrationType](BusinessRegistrationTypePage)

    beRemovable[BusinessRegistrationType](BusinessRegistrationTypePage)

    "must clean up when set to company when have completed an individual UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Company)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualUK) must be (true)
    }

    "must clean up when set to company when have completed an individual non UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualNonUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Company)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualNonUK) must be (true)
    }

    "must clean up when set to partnership when have completed an individual non UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualNonUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Partnership)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualNonUK) must be (true)
    }

    "must clean up when set to partnership when have completed an individual UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Partnership)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualUK) must be (true)
    }

    "must clean up when set to partnership when have completed an company UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Partnership)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyUK) must be (true)
    }

    "must clean up when set to partnership when have completed an company Non UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyNonUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Partnership)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyNonUK - BusinessRegistrationTypePage) must be (true)
    }

    "must clean up when set to company when have completed an partnership UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Company)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipUK) must be (true)
    }

    "must clean up when set to company when have completed an partnership non UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipNonUK.setOrException(BusinessRegistrationTypePage ,BusinessRegistrationType.Company)
      result.get(WhatTypeBusinessPage).isDefined must be(true)
      result.get(BusinessRegistrationTypePage).isDefined must be(true)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipNonUK - BusinessRegistrationTypePage) must be (true)
    }
  }
}
