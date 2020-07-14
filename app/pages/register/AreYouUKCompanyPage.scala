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

import models.UserAnswers
import pages.company.{CompanyAddressListPage, CompanyAddressPage, CompanyEmailPage, CompanyPhonePage, CompanyPostcodePage, CompanyUseSameAddressPage, BusinessNamePage => CompanyNamePage, BusinessUTRPage => CompanyUTRPage, ConfirmAddressPage => ConfirmCompanyAddressPage, ConfirmNamePage => ConfirmCompanyNamePage}
import pages.partnership.{BusinessNamePage => PartnershipNamePage, BusinessUTRPage => PartnershipUTRPage, ConfirmAddressPage => ConfirmPartnershipAddressPage, ConfirmNamePage => ConfirmPartnershipNamePage}
import pages.{QuestionPage, RegistrationInfoPage}
import play.api.libs.json.JsPath

import scala.util.Try

case object AreYouUKCompanyPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "areYouUKCompany"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    val result = value match {
      case Some(false) =>
        userAnswers
          .remove(BusinessTypePage).toOption.getOrElse(userAnswers)
          .remove(PartnershipNamePage).toOption.getOrElse(userAnswers)
          .remove(PartnershipUTRPage).toOption.getOrElse(userAnswers)
          .remove(ConfirmPartnershipNamePage).toOption.getOrElse(userAnswers)
          .remove(ConfirmPartnershipAddressPage).toOption.getOrElse(userAnswers)
          .remove(RegistrationInfoPage).toOption.getOrElse(userAnswers)
          .remove(CompanyNamePage).toOption.getOrElse(userAnswers)
          .remove(CompanyUTRPage).toOption.getOrElse(userAnswers)
          .remove(ConfirmCompanyNamePage).toOption.getOrElse(userAnswers)
          .remove(ConfirmCompanyAddressPage).toOption.getOrElse(userAnswers)
          .remove(CompanyAddressListPage).toOption.getOrElse(userAnswers)
          .remove(CompanyAddressPage).toOption.getOrElse(userAnswers)
          .remove(CompanyEmailPage).toOption.getOrElse(userAnswers)
          .remove(CompanyPhonePage).toOption.getOrElse(userAnswers)
          .remove(CompanyPostcodePage).toOption.getOrElse(userAnswers)
          .remove(CompanyUseSameAddressPage).toOption
      case _ => None
    }
    super.cleanup(value, result.getOrElse(userAnswers))
  }
}
