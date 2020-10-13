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

import com.google.inject.Inject
import connectors.cache.UserAnswersCacheConnector
import controllers.company.routes._
import models.CheckMode
import models.NormalMode
import models.SubscriptionType.Variation
import models.UserAnswers
import pages.{Page, SubscriptionTypePage}
import pages.company._
import pages.register.AreYouUKCompanyPage
import play.api.mvc.Call

import scala.util.Try

class CompanyNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessUTRPage => CompanyNameController.onPageLoad()
    case BusinessNamePage =>
      ua.get(AreYouUKCompanyPage) match {
        case Some(true) => ConfirmNameController.onPageLoad()
        case _ => CompanyEnterRegisteredAddressController.onPageLoad(NormalMode)
      }

    case ConfirmNamePage => ua.get(ConfirmNamePage) match {
        case Some(false) => TellHMRCController.onPageLoad()
        case _ => ConfirmAddressController.onPageLoad()
      }
    case ConfirmAddressPage => ua.get(ConfirmAddressPage) match {
        case None => TellHMRCController.onPageLoad()
        case _ => CompanyUseSameAddressController.onPageLoad()
      }
    case CompanyUseSameAddressPage =>
      (ua.get(AreYouUKCompanyPage), ua.get(CompanyUseSameAddressPage)) match {
      case (_, Some(true)) => CompanyEmailController.onPageLoad(NormalMode)
      case (Some(false), Some(false)) => CompanyContactAddressController.onPageLoad(NormalMode)
      case _ => CompanyPostcodeController.onPageLoad(NormalMode)
    }
    case CompanyPostcodePage => CompanyAddressListController.onPageLoad(NormalMode)
    case CompanyAddressListPage => CompanyEmailController.onPageLoad(NormalMode)
    case CompanyAddressPage => CompanyEmailController.onPageLoad(NormalMode)
    case CompanyRegisteredAddressPage =>
      (ua.get(AreYouUKCompanyPage), ua.get(CompanyRegisteredAddressPage)) match {
      case (Some(false), Some(addr)) if addr.country == "GB" => IsCompanyRegisteredInUkController.onPageLoad()
      case _ => CompanyUseSameAddressController.onPageLoad()
    }
    case IsCompanyRegisteredInUkPage =>
      ua.get(IsCompanyRegisteredInUkPage) match {
        case Some(true) => controllers.routes.WhatTypeBusinessController.onPageLoad()
        case _ => CompanyEnterRegisteredAddressController.onPageLoad(NormalMode)
      }
    case CompanyEmailPage => CompanyPhoneController.onPageLoad(NormalMode)
    case CompanyPhonePage => CheckYourAnswersController.onPageLoad()
    case DeclarationPage => controllers.company.routes.ConfirmationController.onPageLoad()
  }
  //scalastyle:off cyclomatic.complexity

  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessNamePage => variationNavigator(userAnswers)
    case CompanyPostcodePage => CompanyAddressListController.onPageLoad(CheckMode)
    case CompanyAddressListPage => variationNavigator(userAnswers)
    case CompanyAddressPage => variationNavigator(userAnswers)
    case CompanyEmailPage => variationNavigator(userAnswers)
    case CompanyPhonePage => variationNavigator(userAnswers)
  }

  def variationNavigator(ua: UserAnswers): Call = {
    ua.get(SubscriptionTypePage) match {
      case Some(Variation) => controllers.amend.routes.ViewDetailsController.onPageLoad()
      case _ => CheckYourAnswersController.onPageLoad()
    }
  }
}
