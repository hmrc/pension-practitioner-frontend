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

package utils

import models.register.RegistrationCustomerType.{NonUK, UK}
import models.register.RegistrationLegalStatus.{Individual, LimitedCompany, Partnership}
import models.requests.DataRequest
import models.{KnownFact, KnownFacts}
import pages.RegistrationInfoPage
import pages.company.{ConfirmAddressPage => ConfirmCompanyAddressPage}
import pages.individual.IndividualAddressPage
import pages.partnership.{ConfirmAddressPage => ConfirmPartnershipAddressPage}
import play.api.mvc.AnyContent

class KnownFactsRetrieval {

  private val pspKey = "PSPID"
  private val ninoKey = "NINO"
  private val ctUtrKey = "CTUTR"
  private val saUtrKey = "SAUTR"
  private val countryKey = "CountryCode"

  def retrieve(pspId: String)(implicit request: DataRequest[AnyContent]): Option[KnownFacts] =
    request.userAnswers.get(RegistrationInfoPage).flatMap { registrationInfo =>

      (registrationInfo.legalStatus, registrationInfo.idNumber, registrationInfo.customerType) match {
        case (Individual, Some(idNumber), UK) =>
          Some(KnownFacts(Set(KnownFact(pspKey, pspId)), Set(KnownFact(ninoKey, idNumber))))
        case (LimitedCompany, Some(idNumber), UK) =>
          Some(KnownFacts(Set(KnownFact(pspKey, pspId)), Set(KnownFact(ctUtrKey, idNumber))))
        case (Partnership, Some(idNumber), UK) =>
          Some(KnownFacts(Set(KnownFact(pspKey, pspId)), Set(KnownFact(saUtrKey, idNumber))))
        case (legalStatus, _, NonUK) =>
          for {
            address <- legalStatus match {
              case LimitedCompany => request.userAnswers.get(ConfirmCompanyAddressPage)
              case Partnership => request.userAnswers.get(ConfirmPartnershipAddressPage)
              case Individual => request.userAnswers.get(IndividualAddressPage)
            }
            country <- address.country
          } yield {
            KnownFacts(Set(KnownFact(pspKey, pspId)), Set(KnownFact(countryKey, country)))
          }
        case _ => None
      }
    }
}
