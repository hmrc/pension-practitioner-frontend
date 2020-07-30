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

import models.{CheckMode, NormalMode, UserAnswers}
import pages.Page
import pages.individual._
import play.api.mvc.Call

class IndividualNavigator
  extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case WhatYouWillNeedPage => controllers.individual.routes.AreYouUKResidentController.onPageLoad()
    case AreYouUKResidentPage =>
      ua.get(AreYouUKResidentPage) match {
        case Some(true) =>
          controllers.individual.routes.IsThisYouController.onPageLoad(NormalMode)
        case Some(false) =>
          controllers.individual.routes.IndividualNameController.onPageLoad()
        case _ =>
          controllers.routes.SessionExpiredController.onPageLoad()
      }
    case IndividualDetailsPage =>
      controllers.individual.routes.IndividualNonUKAddressController.onPageLoad(NormalMode)
    case IndividualAddressPage =>
      controllers.individual.routes.UseAddressForContactController.onPageLoad(NormalMode)
    case IsThisYouPage =>
      ua.get(IsThisYouPage) match {
        case Some(true) =>
          controllers.individual.routes.UseAddressForContactController.onPageLoad(NormalMode)
        case Some(false) =>
          controllers.individual.routes.YouNeedToTellHMRCController.onPageLoad()
        case _ =>
          controllers.routes.SessionExpiredController.onPageLoad()
      }
    case UseAddressForContactPage =>
      ua.get(UseAddressForContactPage) match {
        case Some(true) =>
          controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
        case Some(false) =>
          controllers.individual.routes.IndividualPostcodeController.onPageLoad(NormalMode)
        case _ =>
          controllers.routes.SessionExpiredController.onPageLoad()
      }
    case IndividualPostcodePage => controllers.individual.routes.IndividualAddressListController.onPageLoad(NormalMode)
    case IndividualAddressListPage => controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
    case IndividualManualAddressPage => controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
    case IndividualEmailPage => controllers.individual.routes.IndividualPhoneController.onPageLoad(NormalMode)
    case IndividualPhonePage => controllers.individual.routes.CheckYourAnswersController.onPageLoad()
    case DeclarationPage => controllers.individual.routes.ConfirmationController.onPageLoad()
  }
  //scalastyle:on cyclomatic.complexity
  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case IndividualPostcodePage => controllers.individual.routes.IndividualAddressListController.onPageLoad(CheckMode)
    case IndividualAddressListPage => controllers.individual.routes.CheckYourAnswersController.onPageLoad()
    case IndividualManualAddressPage => controllers.individual.routes.CheckYourAnswersController.onPageLoad()
    case IndividualAddressPage => controllers.individual.routes.CheckYourAnswersController.onPageLoad()
    case IndividualEmailPage => controllers.individual.routes.CheckYourAnswersController.onPageLoad()
    case IndividualPhonePage => controllers.individual.routes.CheckYourAnswersController.onPageLoad()
  }
}
