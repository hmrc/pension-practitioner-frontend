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

package generators

import models.WhatTypeBusiness
import models.register.BusinessType
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Arbitrary
import pages.WhatTypeBusinessPage
import pages.register.{AreYouUKCompanyPage, BusinessTypePage}
import play.api.libs.json.JsValue
import play.api.libs.json.Json


trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryBusinessTypeUserAnswersEntry: Arbitrary[(BusinessTypePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[BusinessTypePage.type]
        value <- arbitrary[BusinessType].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAreYouUKCompanyUserAnswersEntry: Arbitrary[(AreYouUKCompanyPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[AreYouUKCompanyPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryChargeTypeUserAnswersEntry: Arbitrary[(WhatTypeBusinessPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[WhatTypeBusinessPage.type]
        value <- arbitrary[WhatTypeBusiness].map(Json.toJson(_))
      } yield (page, value)
    }
}
