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

package pages

import data.SampleData
import models.UserAnswers
import models.WhatTypeBusiness
import models.WhatTypeBusiness.Companyorpartnership
import models.WhatTypeBusiness.Yourselfasindividual
import pages.behaviours.PageBehaviours
import queries.Gettable

class WhatTypeBusinessPageSpec extends PageBehaviours {

  "WhatTypeBusinessPage" - {

    beRetrievable[WhatTypeBusiness](WhatTypeBusinessPage)

    beSettable[WhatTypeBusiness](WhatTypeBusinessPage)

    beRemovable[WhatTypeBusiness](WhatTypeBusinessPage)

    "must clean up when set to Companyorpartnership when have completed an individual UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualUK.setOrException(WhatTypeBusinessPage, Companyorpartnership)
      result.getOrException(WhatTypeBusinessPage) must be(models.WhatTypeBusiness.Companyorpartnership)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualUK) must be (true)
    }

    "must clean up when set to Companyorpartnership when have completed an individual non-UK journey" in {
      val result = SampleData.userAnswersFullJourneyIndividualNonUK.setOrException(WhatTypeBusinessPage, Companyorpartnership)
      result.getOrException(WhatTypeBusinessPage) must be(models.WhatTypeBusiness.Companyorpartnership)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyIndividualNonUK) must be (true)
    }

    "must clean up when set to Yourselfasindividual when have completed a company UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyUK.setOrException(WhatTypeBusinessPage, Yourselfasindividual)
      result.getOrException(WhatTypeBusinessPage) must be(models.WhatTypeBusiness.Yourselfasindividual)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyUK) must be (true)
    }

    "must clean up when set to Yourselfasindividual when have completed a company non-UK journey" in {
      val result = SampleData.userAnswersFullJourneyCompanyNonUK.setOrException(WhatTypeBusinessPage, Yourselfasindividual)
      result.getOrException(WhatTypeBusinessPage) must be(models.WhatTypeBusiness.Yourselfasindividual)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyCompanyNonUK) must be (true)
    }

    "must clean up when set to Yourselfasindividual when have completed a partnership UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipUK.setOrException(WhatTypeBusinessPage, Yourselfasindividual)
      result.getOrException(WhatTypeBusinessPage) must be(models.WhatTypeBusiness.Yourselfasindividual)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipUK) must be (true)
    }

    "must clean up when set to Yourselfasindividual when have completed a partnership non-UK journey" in {
      val result = SampleData.userAnswersFullJourneyPartnershipNonUK.setOrException(WhatTypeBusinessPage, Yourselfasindividual)
      result.getOrException(WhatTypeBusinessPage) must be(models.WhatTypeBusiness.Yourselfasindividual)
      areAllPagesEmpty(result, PageConstants.pagesFullJourneyPartnershipNonUK) must be (true)
    }

  }
}
