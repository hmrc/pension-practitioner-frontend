/*
 * Copyright 2023 HM Revenue & Customs
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

import models.register.RegistrationCustomerType.{UK, NonUK}
import models.register.RegistrationLegalStatus
import models.register.RegistrationLegalStatus.{Individual, LimitedCompany, Partnership}
import models.requests.DataRequest
import models.{KnownFacts, KnownFact, Address}
import pages.RegistrationInfoPage
import pages.company.CompanyRegisteredAddressPage
import pages.individual.IndividualAddressPage
import pages.partnership.PartnershipRegisteredAddressPage
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
          addOpt(legalStatus).fold[Option[KnownFacts]](None)(address =>
            Some(KnownFacts(Set(KnownFact(pspKey, pspId)), Set(KnownFact(countryKey, address.country)))))

        case _ => None
      }
    }

  private def addOpt(legalStatus: RegistrationLegalStatus)(implicit request: DataRequest[AnyContent]): Option[Address] =
    legalStatus match {
      case LimitedCompany => request.userAnswers.get(CompanyRegisteredAddressPage)
      case Partnership => request.userAnswers.get(PartnershipRegisteredAddressPage)
      case Individual => request.userAnswers.get(IndividualAddressPage)
    }
}
