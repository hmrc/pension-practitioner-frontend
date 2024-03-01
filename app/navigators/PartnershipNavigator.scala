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

import com.google.inject.Inject
import connectors.cache.UserAnswersCacheConnector
import controllers.partnership.routes._
import models.SubscriptionType.Variation
import models.{NormalMode, CheckMode, UserAnswers}
import pages.partnership._
import pages.register.AreYouUKCompanyPage
import pages.{SubscriptionTypePage, Page}
import play.api.mvc.Call

class PartnershipNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessUTRPage => PartnershipNameController.onPageLoad(NormalMode)
    case BusinessNamePage =>
      ua.get(AreYouUKCompanyPage) match {
        case Some(true) => ConfirmNameController.onPageLoad()
        case _ =>
          PartnershipEnterRegisteredAddressController.onPageLoad(NormalMode)
      }

    case ConfirmNamePage => ua.get(ConfirmNamePage) match {
      case Some(false) => TellHMRCController.onPageLoad()
      case _ => ConfirmAddressController.onPageLoad()
    }
    case ConfirmAddressPage => ua.get(ConfirmAddressPage) match {
      case None => TellHMRCController.onPageLoad()
      case _ => PartnershipUseSameAddressController.onPageLoad()
    }
    case PartnershipUseSameAddressPage =>
      (ua.get(AreYouUKCompanyPage), ua.get(PartnershipUseSameAddressPage)) match {
        case (_, Some(true)) => PartnershipEmailController.onPageLoad(NormalMode)
        case (Some(false), Some(false)) => PartnershipContactAddressController.onPageLoad(NormalMode)
        case _ => PartnershipPostcodeController.onPageLoad(NormalMode)
      }

    case PartnershipPostcodePage => PartnershipAddressListController.onPageLoad(NormalMode)
    case PartnershipAddressListPage => PartnershipEmailController.onPageLoad(NormalMode)
    case PartnershipAddressPage => PartnershipEmailController.onPageLoad(NormalMode)
    case PartnershipRegisteredAddressPage =>
      (ua.get(AreYouUKCompanyPage), ua.get(PartnershipRegisteredAddressPage)) match {
        case (Some(false), Some(addr)) if addr.country == "GB" => IsPartnershipRegisteredInUkController.onPageLoad()
        case _ => PartnershipUseSameAddressController.onPageLoad()
      }

    case IsPartnershipRegisteredInUkPage =>
      ua.get(IsPartnershipRegisteredInUkPage) match {
        case Some(true) => controllers.routes.WhatTypeBusinessController.onPageLoad()
        case _ => PartnershipEnterRegisteredAddressController.onPageLoad(NormalMode)
      }

    case PartnershipEmailPage => PartnershipPhoneController.onPageLoad(NormalMode)
    case PartnershipPhonePage => CheckYourAnswersController.onPageLoad()
    case DeclarationPage => ConfirmationController.onPageLoad()
  }
  //scalastyle:on cyclomatic.complexity

  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessNamePage => variationNavigator(userAnswers)
    case PartnershipPostcodePage => PartnershipAddressListController.onPageLoad(CheckMode)
    case PartnershipAddressListPage => variationNavigator(userAnswers)
    case PartnershipAddressPage => variationNavigator(userAnswers)
    case PartnershipEmailPage => variationNavigator(userAnswers)
    case PartnershipPhonePage => variationNavigator(userAnswers)
  }

  def variationNavigator(ua: UserAnswers): Call = {
    ua.get(SubscriptionTypePage) match {
      case Some(Variation) => controllers.amend.routes.ViewDetailsController.onPageLoad()
      case _ => CheckYourAnswersController.onPageLoad()
    }
  }
}
