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

package navigators

import controllers.partnership.routes._
import data.SampleData
import models.Address
import models.{NormalMode, CheckMode, UserAnswers}
import org.scalatest.prop.TableFor3
import pages.partnership._
import pages.register.AreYouUKCompanyPage
import pages.{partnership, _}
import play.api.mvc.Call

class PartnershipNavigatorSpec extends NavigatorBehaviour {
  private val uaConfirmAddressYes = SampleData
    .emptyUserAnswers.setOrException(partnership.ConfirmAddressPage, SampleData.addressUK)

  private def uaConfirmName(v: Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(partnership.ConfirmNamePage, v)

  private def uaUseSameAddress(sameAddress:Boolean, uk:Boolean): UserAnswers =
    SampleData.emptyUserAnswers
      .setOrException(AreYouUKCompanyPage, uk)
      .setOrException(PartnershipUseSameAddressPage, sameAddress)

  private def uaAreYouUKCompany(v:Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(AreYouUKCompanyPage, v)

  private def uaIsPartnershipRegisteredInUkPage(v:Boolean): UserAnswers =
    SampleData.emptyUserAnswers.setOrException(IsPartnershipRegisteredInUkPage, v)

  private def uaNotInUKButCountryGB: UserAnswers =
    SampleData.emptyUserAnswers
      .setOrException(AreYouUKCompanyPage, false)
      .setOrException(PartnershipRegisteredAddressPage, Address("addr1", "addr2", None, None, None, "GB"))

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(BusinessUTRPage)(PartnershipNameController.onPageLoad(NormalMode)),
        row(BusinessNamePage)(ConfirmNameController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(BusinessNamePage)(PartnershipEnterRegisteredAddressController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(false))),
        row(ConfirmNamePage)(ConfirmAddressController.onPageLoad(), Some(uaConfirmName(true))),
        row(ConfirmNamePage)(TellHMRCController.onPageLoad(), Some(uaConfirmName(false))),
        row(ConfirmAddressPage)(TellHMRCController.onPageLoad()),
        row(ConfirmAddressPage)(PartnershipUseSameAddressController.onPageLoad(), Some(uaConfirmAddressYes)),
        row(PartnershipUseSameAddressPage)(PartnershipEmailController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=true, uk=true))),
        row(PartnershipUseSameAddressPage)(PartnershipPostcodeController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=false, uk=true))),
        row(PartnershipUseSameAddressPage)(PartnershipContactAddressController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=false, uk=false))),
        row(PartnershipPostcodePage)(PartnershipAddressListController.onPageLoad(NormalMode)),
        row(PartnershipAddressListPage)(PartnershipEmailController.onPageLoad(NormalMode)),
        row(PartnershipAddressPage)(PartnershipEmailController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(PartnershipEmailPage)(PartnershipPhoneController.onPageLoad(NormalMode)),
        row(PartnershipPhonePage)(CheckYourAnswersController.onPageLoad()),
        row(PartnershipRegisteredAddressPage)(PartnershipUseSameAddressController.onPageLoad()),
        row(PartnershipRegisteredAddressPage)(IsPartnershipRegisteredInUkController.onPageLoad(), Some(uaNotInUKButCountryGB)),
        row(IsPartnershipRegisteredInUkPage)(controllers.routes.WhatTypeBusinessController.onPageLoad(), Some(uaIsPartnershipRegisteredInUkPage(true))),
        row(IsPartnershipRegisteredInUkPage)(PartnershipEnterRegisteredAddressController
          .onPageLoad(NormalMode), Some(uaIsPartnershipRegisteredInUkPage(false))),
        row(DeclarationPage)(controllers.partnership.routes.ConfirmationController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }

  "CheckMode" must {
    def checkModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(PartnershipPostcodePage)(PartnershipAddressListController.onPageLoad(CheckMode)),
        row(PartnershipAddressListPage)(CheckYourAnswersController.onPageLoad()),
        row(PartnershipAddressPage)(CheckYourAnswersController.onPageLoad()),
        row(PartnershipEmailPage)(CheckYourAnswersController.onPageLoad()),
        row(PartnershipPhonePage)(CheckYourAnswersController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(CheckMode)(navigator, checkModeRoutes)
  }


}
