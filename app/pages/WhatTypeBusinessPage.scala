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

package pages

import models.WhatTypeBusiness.{Companyorpartnership, Yourselfasindividual}
import models.{UserAnswers, WhatTypeBusiness}
import play.api.libs.json.JsPath

import scala.util.Try
import pages.company.{CompanyAddressListPage, CompanyAddressPage, CompanyEmailPage, CompanyPhonePage, CompanyPostcodePage, CompanyUseSameAddressPage, BusinessNamePage => CompanyNamePage, BusinessUTRPage => CompanyUTRPage, ConfirmAddressPage => ConfirmCompanyAddressPage, ConfirmNamePage => ConfirmCompanyNamePage}
import pages.individual._
import pages.partnership.{BusinessNamePage => PartnershipNamePage, BusinessUTRPage => PartnershipUTRPage, ConfirmAddressPage => ConfirmPartnershipAddressPage, ConfirmNamePage => ConfirmPartnershipNamePage}
import pages.register.{AreYouUKCompanyPage, BusinessTypePage}

case object WhatTypeBusinessPage extends QuestionPage[WhatTypeBusiness] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "whatTypeBusiness"

  override def cleanup(value: Option[WhatTypeBusiness], userAnswers: UserAnswers): Try[UserAnswers] = {
    val result = value match {
      case Some(Yourselfasindividual) =>
        userAnswers
          .remove(AreYouUKCompanyPage).toOption.getOrElse(userAnswers)
          .remove(BusinessTypePage).toOption.getOrElse(userAnswers)
          .remove(PartnershipUTRPage).toOption.getOrElse(userAnswers)
          .remove(PartnershipNamePage).toOption.getOrElse(userAnswers)
          .remove(ConfirmPartnershipNamePage).toOption.getOrElse(userAnswers)
          .remove(ConfirmPartnershipAddressPage).toOption.getOrElse(userAnswers)
          .remove(CompanyUTRPage).toOption.getOrElse(userAnswers)
          .remove(CompanyNamePage).toOption.getOrElse(userAnswers)
          .remove(ConfirmCompanyNamePage).toOption.getOrElse(userAnswers)
          .remove(ConfirmCompanyAddressPage).toOption.getOrElse(userAnswers)
          .remove(CompanyAddressListPage).toOption.getOrElse(userAnswers)
          .remove(CompanyAddressPage).toOption.getOrElse(userAnswers)
          .remove(CompanyEmailPage).toOption.getOrElse(userAnswers)
          .remove(CompanyPhonePage).toOption.getOrElse(userAnswers)
          .remove(CompanyPostcodePage).toOption.getOrElse(userAnswers)
          .remove(CompanyUseSameAddressPage).toOption.getOrElse(userAnswers)
          .remove(RegistrationInfoPage).toOption
      case Some(companyorpartnership) =>
        userAnswers
          .remove(AreYouUKResidentPage).toOption.getOrElse(userAnswers)
          .remove(IndividualDetailsPage).toOption.getOrElse(userAnswers)
          .remove(IndividualAddressPage).toOption.getOrElse(userAnswers)
          .remove(IndividualPostcodePage).toOption.getOrElse(userAnswers)
          .remove(IndividualAddressListPage).toOption.getOrElse(userAnswers)
          .remove(IndividualManualAddressPage).toOption.getOrElse(userAnswers)
          .remove(IndividualEmailPage).toOption.getOrElse(userAnswers)
          .remove(IndividualPhonePage).toOption.getOrElse(userAnswers)
          .remove(IsThisYouPage).toOption.getOrElse(userAnswers)
          .remove(UseAddressForContactPage).toOption.getOrElse(userAnswers)
          .remove(RegistrationInfoPage).toOption
      case _ => None
    }
    super.cleanup(value, result.getOrElse(userAnswers))
  }
}
