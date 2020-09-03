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

package data

import models.Address
import models.TolerantAddress
import models.UserAnswers
import models.WhatTypeBusiness.Companyorpartnership
import models.WhatTypeBusiness.Yourselfasindividual
import models.register.BusinessRegistrationType
import models.register.BusinessType
import models.register.RegistrationCustomerType
import models.register.RegistrationInfo
import models.register.RegistrationLegalStatus
import pages.RegistrationInfoPage
import pages.WhatTypeBusinessPage
import pages.company.CompanyRegisteredAddressPage
import pages.individual.AreYouUKResidentPage
import pages.register.AreYouUKCompanyPage
import pages.register.BusinessRegistrationTypePage
import pages.register.BusinessTypePage


object SampleData {
  //scalastyle.off: magic.number
  val userAnswersId = "id"
  val psaId = "A0000000"
  val pspName = "psp"

  def emptyUserAnswers: UserAnswers = UserAnswers()

  def userAnswersWithCompanyName: UserAnswers = UserAnswers().setOrException(pages.company.BusinessNamePage, pspName)

  def userAnswersWithPartnershipName: UserAnswers = UserAnswers().setOrException(pages.partnership.BusinessNamePage, pspName)

  val tolerantAddress = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))
  val addressUK = TolerantAddress(Some("addr1"), Some("addr2"), None, None, Some(""), Some(""))
  val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  def registrationInfo(registrationLegalStatus:RegistrationLegalStatus) =
    RegistrationInfo(registrationLegalStatus, "", noIdentifier = false, RegistrationCustomerType.UK, None, None)

  /*
/who-are-you-registering (company or partnership OR yourself as an individual) - WhatTypeBusinessPage
/are-you-in-the-uk-company (yes OR no) - AreYouUKCompanyPage
/business-type (5 items grouped into company OR partnership) - BusinessTypePage
/what-is-business-registered-as [NON UK] (company OR partnership) - BusinessRegistrationTypePage
/are-you-uk-resident [YOURSELF AS INDIVIDUAL] (yes OR no) - AreYouUKResidentPage

WhatTypeBusinessPage=Companyorpartnership
 AreYouUKCompanyPage=Yes
  BusinessTypePage=Limited company OR Unlimited company
   Business UTR
   Org name
   Confirm name
   Confirm address
   Can use as contact address
    No
     Contact address
   Email address
   Phone no
  BusinessTypePage=One of the partnership types
   <as for company above>
 AreYouUKCompanyPage=No
  BusinessRegistrationTypePage=Company
   Org name
   Enter registered address
   Can use as contact address
    No
     Contact address
   Email address
   Phone no
  BusinessRegistrationTypePage=Partnership
   <as above>
 WhatTypeBusinessPage=Yourself
  AreYouUKResidentPage=Yes
   Is this your name and address?
   Can hmrc use this address to contact you?
    No
     Address manual
   Email
   Phone no
  AreYouUKResidentPage=No
   What is your name?
   What is your address?
   Can hmrc use this address to contact you?
    No
     Address manual
   Email address
   Phone no
   */

  /*
    isCompany: None = yourself as individual, Some(false) = partnership, Some(true) = company
   */


  def userAnswersCompanyUK:UserAnswers = {
    UserAnswers()
    .setOrException(WhatTypeBusinessPage, value = Companyorpartnership)
    .setOrException(AreYouUKCompanyPage, true)
    .setOrException(BusinessTypePage, BusinessType.LimitedCompany)
    .setOrException(pages.company.BusinessUTRPage, "")
    .setOrException(pages.company.BusinessNamePage, "")
    .setOrException(pages.company.ConfirmNamePage, true)
    .setOrException(pages.company.ConfirmAddressPage, tolerantAddress)
    .setOrException(RegistrationInfoPage, registrationInfo(RegistrationLegalStatus.LimitedCompany))
    .setOrException(pages.company.CompanyUseSameAddressPage, false)
    .setOrException(pages.company.CompanyPostcodePage, Seq(tolerantAddress))
    .setOrException(pages.company.CompanyAddressListPage, 0)
    .setOrException(pages.company.CompanyAddressPage, address)
    .setOrException(pages.company.CompanyEmailPage, "")
    .setOrException(pages.company.CompanyPhonePage, "")
  }

  def userAnswersPartnershipUK:UserAnswers = {
    UserAnswers()
      .setOrException(WhatTypeBusinessPage, value = Companyorpartnership)
      .setOrException(AreYouUKCompanyPage, true)
      .setOrException(BusinessTypePage, BusinessType.LimitedCompany)
      .setOrException(pages.partnership.BusinessUTRPage, "")
      .setOrException(pages.partnership.BusinessNamePage, "")
      .setOrException(pages.partnership.ConfirmNamePage, true)
      .setOrException(pages.partnership.ConfirmAddressPage, tolerantAddress)
      .setOrException(RegistrationInfoPage, registrationInfo(RegistrationLegalStatus.LimitedCompany))
      .setOrException(pages.partnership.PartnershipUseSameAddressPage, false)
      .setOrException(pages.partnership.PartnershipPostcodePage, Seq(tolerantAddress))
      .setOrException(pages.partnership.PartnershipAddressListPage, 0)
      .setOrException(pages.partnership.PartnershipAddressPage, address)
      .setOrException(pages.partnership.PartnershipEmailPage, "")
      .setOrException(pages.partnership.PartnershipPhonePage, "")
  }

  def userAnswersCompanyNonUK:UserAnswers = {
    UserAnswers()
      .setOrException(WhatTypeBusinessPage, value = Companyorpartnership)
      .setOrException(AreYouUKCompanyPage, false)
      .setOrException(BusinessRegistrationTypePage, BusinessRegistrationType.Company)
      .setOrException(pages.company.BusinessNamePage, "")
      .setOrException(pages.company.CompanyRegisteredAddressPage, address)
      .setOrException(pages.company.CompanyUseSameAddressPage, false)
      .setOrException(pages.company.CompanyAddressPage, address)
      .setOrException(RegistrationInfoPage, registrationInfo(RegistrationLegalStatus.LimitedCompany))
      .setOrException(pages.company.CompanyEmailPage, "")
      .setOrException(pages.company.CompanyPhonePage, "")
  }

  def userAnswersPartnershipNonUK:UserAnswers = {
    UserAnswers()
      .setOrException(WhatTypeBusinessPage, value = Companyorpartnership)
      .setOrException(AreYouUKCompanyPage, false)
      .setOrException(BusinessRegistrationTypePage, BusinessRegistrationType.Partnership)
      .setOrException(pages.partnership.BusinessNamePage, "")
      .setOrException(pages.partnership.PartnershipRegisteredAddressPage, address)
      .setOrException(pages.partnership.PartnershipUseSameAddressPage, false)
      .setOrException(pages.partnership.PartnershipAddressPage, address)
      .setOrException(RegistrationInfoPage, registrationInfo(RegistrationLegalStatus.Partnership))
      .setOrException(pages.partnership.PartnershipEmailPage, "")
      .setOrException(pages.partnership.PartnershipPhonePage, "")
  }

  /*
    AreYouUKResidentPage=Yes
   Is this your name and address?
   Can hmrc use this address to contact you?
    No
     Address manual
   Email
   Phone no
  AreYouUKResidentPage=No
   What is your name?
   What is your address?
   Can hmrc use this address to contact you?
    No
     Address manual
   Email address
   Phone no
   */

  def userAnswersIndividualUK:UserAnswers = {
    UserAnswers()
      .setOrException(WhatTypeBusinessPage, value = Yourselfasindividual)
      .setOrException(AreYouUKResidentPage, true)


      //.setOrException(BusinessRegistrationTypePage, BusinessRegistrationType.Company)
      //.setOrException(pages.company.BusinessNamePage, "")
      //.setOrException(pages.company.CompanyRegisteredAddressPage, address)
      //.setOrException(pages.company.CompanyUseSameAddressPage, false)
      //.setOrException(pages.company.CompanyAddressPage, address)
      //.setOrException(RegistrationInfoPage, registrationInfo(RegistrationLegalStatus.LimitedCompany))
      //.setOrException(pages.company.CompanyEmailPage, "")
      //.setOrException(pages.company.CompanyPhonePage, "")
  }

  def userAnswersIndividualNonUK:UserAnswers = {
    UserAnswers()
  }

}
