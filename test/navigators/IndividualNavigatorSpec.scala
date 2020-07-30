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

import data.SampleData
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.prop.TableFor3
import pages._
import pages.individual._
import play.api.mvc.Call

class IndividualNavigatorSpec extends NavigatorBehaviour {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  private def uaIsThisYou(flag: Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(IsThisYouPage, flag)

  private def areYouUKResident(flag: Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(AreYouUKResidentPage, flag)

  private def uaUseAddressForContact(flag: Boolean): UserAnswers = SampleData
    .emptyUserAnswers.setOrException(UseAddressForContactPage, flag)

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(WhatYouWillNeedPage)(controllers.individual.routes.AreYouUKResidentController.onPageLoad()),
        row(AreYouUKResidentPage)(controllers.individual.routes.IsThisYouController.onPageLoad(NormalMode), Some(areYouUKResident(true))),
        row(AreYouUKResidentPage)(controllers.individual.routes.IndividualNameController.onPageLoad(), Some(areYouUKResident(false))),
        row(AreYouUKResidentPage)(controllers.routes.SessionExpiredController.onPageLoad(), None),
        row(IndividualDetailsPage)(controllers.individual.routes.IndividualNonUKAddressController.onPageLoad(NormalMode), None),
        row(IndividualAddressPage)(controllers.individual.routes.UseAddressForContactController.onPageLoad(NormalMode), None),
        row(IsThisYouPage)(controllers.individual.routes.YouNeedToTellHMRCController.onPageLoad(), Some(uaIsThisYou(false))),
        row(IsThisYouPage)(controllers.individual.routes.UseAddressForContactController.onPageLoad(NormalMode), Some(uaIsThisYou(true))),
        row(IsThisYouPage)(controllers.routes.SessionExpiredController.onPageLoad(), None),
        row(UseAddressForContactPage)(controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode), Some(uaUseAddressForContact(true))),
        row(UseAddressForContactPage)(controllers.individual.routes.IndividualPostcodeController.onPageLoad(NormalMode), Some(uaUseAddressForContact(false))),
        row(UseAddressForContactPage)(controllers.routes.SessionExpiredController.onPageLoad(), None),
        row(IndividualPostcodePage)(controllers.individual.routes.IndividualAddressListController.onPageLoad(NormalMode)),
        row(IndividualAddressListPage)(controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)),
        row(IndividualManualAddressPage)(controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)),
        row(IndividualEmailPage)(controllers.individual.routes.IndividualPhoneController.onPageLoad(NormalMode)),
        row(IndividualPhonePage)(controllers.individual.routes.CheckYourAnswersController.onPageLoad()),
        row(DeclarationPage)(controllers.individual.routes.ConfirmationController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }

  "CheckMode" must {
    def checkModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(IndividualPostcodePage)(controllers.individual.routes.IndividualAddressListController.onPageLoad(CheckMode)),
        row(IndividualAddressListPage)(controllers.individual.routes.CheckYourAnswersController.onPageLoad()),
        row(IndividualManualAddressPage)(controllers.individual.routes.CheckYourAnswersController.onPageLoad()),
        row(IndividualAddressPage)(controllers.individual.routes.CheckYourAnswersController.onPageLoad()),
        row(IndividualEmailPage)(controllers.individual.routes.CheckYourAnswersController.onPageLoad()),
        row(IndividualPhonePage)(controllers.individual.routes.CheckYourAnswersController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(CheckMode)(navigator, checkModeRoutes)
  }
}
