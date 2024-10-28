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

package generators

import models.WhatTypeBusiness
import models.register.{BusinessRegistrationType, BusinessType}
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages.WhatTypeBusinessPage
import pages.company._
import pages.register.{AreYouUKCompanyPage, BusinessRegistrationTypePage, BusinessTypePage}
import play.api.libs.json.{JsValue, Json}


trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryIsCompanyRegisteredInUkUserAnswersEntry: Arbitrary[(IsCompanyRegisteredInUkPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[IsCompanyRegisteredInUkPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryBusinessRegistrationTypeUserAnswersEntry: Arbitrary[(BusinessRegistrationTypePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[BusinessRegistrationTypePage.type]
        value <- arbitrary[BusinessRegistrationType].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryConfirmAddressUserAnswersEntry: Arbitrary[(ConfirmAddressPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ConfirmAddressPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryConfirmNameUserAnswersEntry: Arbitrary[(ConfirmNamePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ConfirmNamePage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCompanyNameUserAnswersEntry: Arbitrary[(BusinessNamePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[BusinessNamePage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryBusinessUTRUserAnswersEntry: Arbitrary[(BusinessUTRPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[BusinessUTRPage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

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
