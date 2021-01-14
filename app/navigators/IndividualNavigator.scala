/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import models.{NormalMode, CheckMode, UserAnswers}
import pages.{SubscriptionTypePage, Page}
import pages.individual._
import play.api.mvc.Call
import controllers.individual.routes._
import models.SubscriptionType.Variation
import models.register.InternationalRegion.{RestOfTheWorld, EuEea, UK}
import utils.countryOptions.CountryOptions

class IndividualNavigator @Inject()(countryOptions: CountryOptions) extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case WhatYouWillNeedPage => AreYouUKResidentController.onPageLoad(NormalMode)
    case AreYouUKResidentPage =>
      ua.get(AreYouUKResidentPage) match {
        case Some(true) => IsThisYouController.onPageLoad(NormalMode)
        case Some(false) => IndividualNameController.onPageLoad(NormalMode)
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    case IndividualDetailsPage => IndividualEnterRegisteredAddressController.onPageLoad(NormalMode)
    case IndividualAddressPage => regionBasedNavigation(ua)
    case IsThisYouPage =>
      ua.get(IsThisYouPage) match {
        case Some(true) => UseAddressForContactController.onPageLoad(NormalMode)
        case Some(false) => YouNeedToTellHMRCController.onPageLoad()
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    case UseAddressForContactPage =>
      (ua.get(AreYouUKResidentPage), ua.get(UseAddressForContactPage)) match {
        case (_, Some(true)) => IndividualEmailController.onPageLoad(NormalMode)
        case (Some(true), Some(false)) => IndividualPostcodeController.onPageLoad(NormalMode)
        case (Some(false), Some(false)) => IndividualContactAddressController.onPageLoad(NormalMode)
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    case IndividualPostcodePage => IndividualAddressListController.onPageLoad(NormalMode)
    case IndividualAddressListPage => IndividualEmailController.onPageLoad(NormalMode)
    case IndividualManualAddressPage => IndividualEmailController.onPageLoad(NormalMode)
    case IndividualEmailPage => IndividualPhoneController.onPageLoad(NormalMode)
    case IndividualPhonePage => CheckYourAnswersController.onPageLoad()
    case DeclarationPage => ConfirmationController.onPageLoad()
  }
  //scalastyle:on cyclomatic.complexity
  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case AreYouUKResidentPage =>
      userAnswers.get(AreYouUKResidentPage) match {
        case Some(true) => IsThisYouController.onPageLoad(NormalMode)
        case Some(false) => IndividualNameController.onPageLoad(NormalMode)
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    case IndividualDetailsPage => variationNavigator(userAnswers)
    case IndividualPostcodePage => IndividualAddressListController.onPageLoad(CheckMode)
    case IndividualAddressListPage => variationNavigator(userAnswers)
    case IndividualManualAddressPage => variationNavigator(userAnswers)
    case IndividualAddressPage => CheckYourAnswersController.onPageLoad()
    case IndividualEmailPage => variationNavigator(userAnswers)
    case IndividualPhonePage => variationNavigator(userAnswers)
  }

  private def regionBasedNavigation(answers: UserAnswers): Call = {
    answers.get(IndividualAddressPage)
      .fold(controllers.routes.SessionExpiredController.onPageLoad())(address =>
      countryOptions.regions(address.country) match {
        case UK => AreYouUKResidentController.onPageLoad(CheckMode)
        case EuEea => UseAddressForContactController.onPageLoad(NormalMode)
        case RestOfTheWorld => OutsideEuEeaController.onPageLoad()
        case _ => controllers.routes.SessionExpiredController.onPageLoad()
      }
    )
  }

  def variationNavigator(ua: UserAnswers): Call = {
    ua.get(SubscriptionTypePage) match {
      case Some(Variation) => controllers.amend.routes.ViewDetailsController.onPageLoad()
      case _ => CheckYourAnswersController.onPageLoad()
    }
  }
}
