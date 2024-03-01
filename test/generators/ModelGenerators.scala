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

import java.time.ZoneOffset

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import java.time.LocalDate
import java.time.Instant

import models.WhatTypeBusiness
import models.register.BusinessRegistrationType
import models.register.BusinessType

trait ModelGenerators {

  implicit lazy val arbitraryBusinessRegistrationType: Arbitrary[BusinessRegistrationType] =
    Arbitrary {
      Gen.oneOf(BusinessRegistrationType.values.toSeq)
    }

  implicit lazy val arbitraryBusinessType: Arbitrary[BusinessType] =
    Arbitrary {
      Gen.oneOf(BusinessType.values.toSeq)
    }

  implicit lazy val arbitraryChargeType: Arbitrary[WhatTypeBusiness] =
    Arbitrary {
      Gen.oneOf(WhatTypeBusiness.values)
    }

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis =>
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }
}
