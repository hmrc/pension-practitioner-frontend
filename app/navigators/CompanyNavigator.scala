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
import models.NormalMode
import models.UserAnswers
import pages.Page
import pages.company._
import play.api.mvc.Call

class CompanyNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers): PartialFunction[Page, Call] = {
    case BusinessUTRPage => CompanyNameController.onPageLoad()
    case CompanyNamePage => ConfirmNameController.onPageLoad()
    case ConfirmNamePage => ua.get(ConfirmNamePage) match {
        case Some(false) => TellHMRCController.onPageLoad()
        case _ => ConfirmAddressController.onPageLoad()
      }
    case ConfirmAddressPage => ua.get(ConfirmAddressPage) match {
        case None => TellHMRCController.onPageLoad()
        case _ => CompanyUseSameAddressController.onPageLoad()
      }
    case CompanyUseSameAddressPage => ua.get(CompanyUseSameAddressPage) match {
      case Some(true) => CompanyEmailController.onPageLoad(NormalMode)
      case _ => CompanyPostcodeController.onPageLoad(NormalMode)
    }
    case CompanyPostcodePage => CompanyAddressListController.onPageLoad(NormalMode)
    case CompanyAddressListPage => CompanyEmailController.onPageLoad(NormalMode)
    case CompanyAddressPage => CompanyEmailController.onPageLoad(NormalMode)
    case CompanyEmailPage => CompanyPhoneController.onPageLoad(NormalMode)
    case CompanyPhonePage => CheckYourAnswersController.onPageLoad()
    case DeclarationPage => controllers.company.routes.ConfirmationController.onPageLoad()

  }
  //scalastyle:off cyclomatic.complexity

  override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = {

    case CompanyAddressPage => CheckYourAnswersController.onPageLoad()
    case CompanyEmailPage => CheckYourAnswersController.onPageLoad()
    case CompanyPhonePage => CheckYourAnswersController.onPageLoad()

  }
}
