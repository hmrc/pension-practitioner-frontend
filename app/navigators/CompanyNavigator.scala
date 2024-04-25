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
import com.google.inject.Inject
import connectors.cache.UserAnswersCacheConnector
import models.SubscriptionType.Variation
import models.{CheckMode, NormalMode, UserAnswers}
import pages.{SubscriptionTypePage, Page}
import pages.company._
import pages.register.AreYouUKCompanyPage
import play.api.mvc.Call

class CompanyNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  private val nextPageOrNonUkRedirect: (UserAnswers, Call) => Call = (ua: UserAnswers, call: Call) =>
    ua.get(AreYouUKCompanyPage) match {
      case Some(true) => call
      case _ => controllers.register.routes.NonUKPractitionerController.onPageLoad()
  }

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {

    case BusinessUTRPage => nextPageOrNonUkRedirect(ua, CompanyNameController.onPageLoad(NormalMode))

    case BusinessNamePage => nextPageOrNonUkRedirect(ua, ConfirmNameController.onPageLoad())

    case ConfirmNamePage => nextPageOrNonUkRedirect(ua, ua.get(ConfirmNamePage) match {
      case Some(false) => TellHMRCController.onPageLoad()
      case _ => ConfirmAddressController.onPageLoad()
    })

    case ConfirmAddressPage => nextPageOrNonUkRedirect(ua, ua.get(ConfirmAddressPage) match {
      case None => TellHMRCController.onPageLoad()
      case _ => CompanyUseSameAddressController.onPageLoad()
    })

    case CompanyUseSameAddressPage => nextPageOrNonUkRedirect(ua, ua.get(CompanyUseSameAddressPage) match {
      case Some(true) => CompanyEmailController.onPageLoad(NormalMode)
      case Some(false) => CompanyContactAddressController.onPageLoad(NormalMode)
      case _ => CompanyPostcodeController.onPageLoad(NormalMode)
    })

    case CompanyPostcodePage => nextPageOrNonUkRedirect(ua, CompanyAddressListController.onPageLoad(NormalMode))

    case CompanyAddressListPage => nextPageOrNonUkRedirect(ua, CompanyEmailController.onPageLoad(NormalMode))

    case CompanyAddressPage => nextPageOrNonUkRedirect(ua, CompanyEmailController.onPageLoad(NormalMode))

    case CompanyRegisteredAddressPage => nextPageOrNonUkRedirect(ua, ua.get(CompanyRegisteredAddressPage) match {
      case Some(addr) if addr.country == "GB" => IsCompanyRegisteredInUkController.onPageLoad()
      case _ => CompanyUseSameAddressController.onPageLoad()
    })

    case IsCompanyRegisteredInUkPage => nextPageOrNonUkRedirect(ua, ua.get(IsCompanyRegisteredInUkPage) match {
        case Some(true) => controllers.routes.WhatTypeBusinessController.onPageLoad()
        case _ => CompanyEnterRegisteredAddressController.onPageLoad(NormalMode)
    })

    case CompanyEmailPage => nextPageOrNonUkRedirect(ua, CompanyPhoneController.onPageLoad(NormalMode))

    case CompanyPhonePage => nextPageOrNonUkRedirect(ua, CheckYourAnswersController.onPageLoad())

    case DeclarationPage => nextPageOrNonUkRedirect(ua, controllers.company.routes.ConfirmationController.onPageLoad())
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
