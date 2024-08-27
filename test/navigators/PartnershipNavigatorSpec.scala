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

import controllers.partnership.routes._
import controllers.register.routes._
import data.SampleData
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

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(BusinessUTRPage)(PartnershipNameController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(BusinessNamePage)(ConfirmNameController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(BusinessNamePage)(NonUKPractitionerController.onPageLoad(), Some(uaAreYouUKCompany(false))),
        row(ConfirmNamePage)(ConfirmAddressController.onPageLoad(), Some(uaConfirmName(true).setOrException(AreYouUKCompanyPage, true))),
        row(ConfirmNamePage)(controllers.routes.TellHMRCController.onPageLoad("partnership"), Some(uaConfirmName(false).setOrException(AreYouUKCompanyPage, true))),
        row(ConfirmAddressPage)(NonUKPractitionerController.onPageLoad()),
        row(ConfirmAddressPage)(PartnershipUseSameAddressController.onPageLoad(), Some(uaConfirmAddressYes.setOrException(AreYouUKCompanyPage, true))),
        row(PartnershipUseSameAddressPage)(PartnershipEmailController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=true, uk=true))),
        row(PartnershipUseSameAddressPage)(PartnershipContactAddressController.onPageLoad(NormalMode), Some(uaUseSameAddress(sameAddress=false, uk=true))),
        row(PartnershipUseSameAddressPage)(NonUKPractitionerController.onPageLoad(), Some(uaUseSameAddress(sameAddress=false, uk=false))),
        row(PartnershipPostcodePage)(PartnershipAddressListController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(PartnershipAddressListPage)(PartnershipEmailController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(PartnershipAddressPage)(PartnershipEmailController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(PartnershipEmailPage)(PartnershipPhoneController.onPageLoad(NormalMode), Some(uaAreYouUKCompany(true))),
        row(PartnershipPhonePage)(CheckYourAnswersController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(PartnershipRegisteredAddressPage)(PartnershipUseSameAddressController.onPageLoad(), Some(uaAreYouUKCompany(true))),
        row(IsPartnershipRegisteredInUkPage)(controllers.routes.WhatTypeBusinessController.onPageLoad(),
          Some(uaIsPartnershipRegisteredInUkPage(true).setOrException(AreYouUKCompanyPage, true))),
        row(IsPartnershipRegisteredInUkPage)(NonUKPractitionerController.onPageLoad(), Some(uaIsPartnershipRegisteredInUkPage(false))),
        row(DeclarationPage)(controllers.partnership.routes.ConfirmationController.onPageLoad(), Some(uaAreYouUKCompany(true)))
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
