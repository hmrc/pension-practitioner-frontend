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
      case Some(Yourselfasindividual) => userAnswers
        .removeAllPages(PageConstants.pagesFullJourneyCompanyUK)
        .removeAllPages(PageConstants.pagesFullJourneyCompanyNonUK)
        .removeAllPages(PageConstants.pagesFullJourneyPartnershipUK)
        .removeAllPages(PageConstants.pagesFullJourneyPartnershipNonUK)
      case Some(_) => userAnswers
        .removeAllPages(PageConstants.pagesFullJourneyIndividualUK)
        .removeAllPages(PageConstants.pagesFullJourneyIndividualNonUK)
      case _ => userAnswers
    }
    super.cleanup(value, result)
  }
}
