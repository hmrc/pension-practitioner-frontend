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

package navigators

import controllers.company.routes._
import controllers.register.routes._
import data.SampleData
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.prop.TableFor3
import pages.Page
import pages.company._
import pages.register.AreYouUKCompanyPage
import play.api.mvc.Call

class CompanyNavigatorSpec extends NavigatorBehaviour {
  private val uaConfirmAddressYes = SampleData
    .emptyUserAnswers.setOrException(ConfirmAddressPage, SampleData.addressUK)

  private def uaConfirmName(v:Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(ConfirmNamePage, v)

  private def uaUseSameAddress(sameAddress:Boolean, uk:Boolean): UserAnswers =
    SampleData.emptyUserAnswers
    .setOrException(AreYouUKCompanyPage, uk)
    .setOrException(CompanyUseSameAddressPage, sameAddress)

  private def uaAreYouUKCompany(v:Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(AreYouUKCompanyPage, v)

  private def uaIsCompanyRegisteredInUkPage(v:Boolean): UserAnswers =
    SampleData.emptyUserAnswers.setOrException(IsCompanyRegisteredInUkPage, v)

  private val navigator: CompoundNavigator = app.injector.instanceOf[CompoundNavigator]

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(BusinessUTRPage)(CompanyNameController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(BusinessNamePage)(ConfirmNameController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(BusinessNamePage)(NonUKPractitionerController.onPageLoad(), Some(uaAreYouUKCompany(false))),
        row(ConfirmNamePage)(ConfirmAddressController.onPageLoad(), Some(uaConfirmName(true).setOrException(AreYouUKCompanyPage, true))),
        row(ConfirmNamePage)(controllers.routes.TellHMRCController.onPageLoad("company"),Some(uaConfirmName(false).setOrException(AreYouUKCompanyPage, true))),
        row(ConfirmAddressPage)(controllers.routes.TellHMRCController.onPageLoad("company"), Some(uaAreYouUKCompany(true))),
        row(ConfirmAddressPage)(CompanyUseSameAddressController.onPageLoad(), Some(uaConfirmAddressYes.setOrException(AreYouUKCompanyPage, true))),
        row(CompanyUseSameAddressPage)(CompanyEmailController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=true, uk=true))),
        row(CompanyUseSameAddressPage)(CompanyContactAddressController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=false, uk=true))),
        row(CompanyUseSameAddressPage)(NonUKPractitionerController.onPageLoad(), Some(uaUseSameAddress(sameAddress=false, uk=false))),
        row(CompanyPostcodePage)(CompanyAddressListController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(CompanyAddressListPage)(CompanyEmailController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(CompanyAddressPage)(CompanyEmailController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(CompanyEmailPage)(CompanyPhoneController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(CompanyPhonePage)(CheckYourAnswersController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(CompanyRegisteredAddressPage)(CompanyUseSameAddressController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(IsCompanyRegisteredInUkPage)(controllers.routes.WhatTypeBusinessController.onPageLoad(),
          Some(uaIsCompanyRegisteredInUkPage(true).setOrException(AreYouUKCompanyPage, true))),
        row(IsCompanyRegisteredInUkPage)(NonUKPractitionerController.onPageLoad(), Some(uaIsCompanyRegisteredInUkPage(false))),
        row(DeclarationPage)(controllers.company.routes.ConfirmationController.onPageLoad(), Some(uaAreYouUKCompany(true)))
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }

  "CheckMode" must {
    def checkModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),

        row(CompanyAddressPage)(CheckYourAnswersController.onPageLoad()),
        row(CompanyEmailPage)(CheckYourAnswersController.onPageLoad()),
        row(CompanyPhonePage)(CheckYourAnswersController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(CheckMode)(navigator, checkModeRoutes)
  }


}
