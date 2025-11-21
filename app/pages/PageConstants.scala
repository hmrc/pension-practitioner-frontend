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

package pages

import pages.individual._
import pages.register.{AreYouUKCompanyPage, BusinessRegistrationTypePage, BusinessTypePage}
import queries.Gettable

object PageConstants {
  def pagesFullJourneyIndividualUK: Set[Gettable[?]] = Set[Gettable[?]](
    AreYouUKResidentPage,
    IsThisYouPage,
    RegistrationInfoPage,
    IndividualDetailsPage,
    UseAddressForContactPage,
    IndividualPostcodePage,
    IndividualAddressListPage,
    IndividualManualAddressPage,
    pages.individual.IndividualEmailPage,
    pages.individual.IndividualPhonePage
  )

  def pagesFullJourneyIndividualNonUK: Set[Gettable[?]] = Set[Gettable[?]](
    AreYouUKResidentPage,
    IndividualDetailsPage,
    IndividualAddressPage,
    UseAddressForContactPage,
    IndividualPostcodePage,
    IndividualAddressListPage,
    IndividualManualAddressPage,
    pages.individual.IndividualEmailPage,
    pages.individual.IndividualPhonePage
  )

  def pagesFullJourneyCompanyUK: Set[Gettable[?]] = Set[Gettable[?]](
    AreYouUKCompanyPage,
    BusinessTypePage,
    pages.company.BusinessUTRPage,
    pages.company.BusinessNamePage,
    pages.company.ConfirmNamePage,
    pages.company.ConfirmAddressPage,
    RegistrationInfoPage,
    pages.company.CompanyUseSameAddressPage,
    pages.company.CompanyPostcodePage,
    pages.company.CompanyAddressListPage,
    pages.company.CompanyAddressPage,
    pages.company.CompanyEmailPage,
    pages.company.CompanyPhonePage
  )

  def pagesFullJourneyCompanyNonUK: Set[Gettable[?]] = Set[Gettable[?]](
    AreYouUKCompanyPage,
    BusinessRegistrationTypePage,
    pages.company.BusinessNamePage,
    pages.company.CompanyRegisteredAddressPage,
    pages.company.CompanyUseSameAddressPage,
    pages.company.CompanyAddressPage,
    RegistrationInfoPage,
    pages.company.CompanyEmailPage,
    pages.company.CompanyPhonePage
  )

  def pagesFullJourneyPartnershipUK: Set[Gettable[?]] = Set[Gettable[?]](
    AreYouUKCompanyPage,
    BusinessTypePage,
    pages.partnership.BusinessUTRPage,
    pages.partnership.BusinessNamePage,
    pages.partnership.ConfirmNamePage,
    pages.partnership.ConfirmAddressPage,
    RegistrationInfoPage,
    pages.partnership.PartnershipUseSameAddressPage,
    pages.partnership.PartnershipPostcodePage,
    pages.partnership.PartnershipAddressListPage,
    pages.partnership.PartnershipAddressPage,
    pages.partnership.PartnershipEmailPage,
    pages.partnership.PartnershipPhonePage
  )

  def pagesFullJourneyPartnershipNonUK: Set[Gettable[?]] = Set[Gettable[?]](
    AreYouUKCompanyPage,
    BusinessRegistrationTypePage,
    pages.partnership.BusinessNamePage,
    pages.partnership.PartnershipRegisteredAddressPage,
    pages.partnership.PartnershipUseSameAddressPage,
    pages.partnership.PartnershipAddressPage,
    RegistrationInfoPage,
    pages.partnership.PartnershipEmailPage,
    pages.partnership.PartnershipPhonePage
  )

  def pagesFullJourneyCompanyAndPartnership: Set[Gettable[?]] =
    pagesFullJourneyCompanyUK ++
      pagesFullJourneyCompanyNonUK ++
      pagesFullJourneyPartnershipUK ++
      pagesFullJourneyPartnershipNonUK

  def pagesFullJourneyIndividual: Set[Gettable[?]] =
    pagesFullJourneyIndividualUK ++
    pagesFullJourneyIndividualNonUK

  def pagesFullJourneyAll: Set[Gettable[?]] =
    pagesFullJourneyCompanyUK ++
    pagesFullJourneyCompanyNonUK ++
    pagesFullJourneyPartnershipUK ++
    pagesFullJourneyPartnershipNonUK ++
    pagesFullJourneyIndividualUK ++
    pagesFullJourneyIndividualNonUK
}
