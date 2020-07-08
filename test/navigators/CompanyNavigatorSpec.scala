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

import models.NormalMode
import models.UserAnswers
import org.scalatest.prop.TableFor3
import pages._
import pages.register.company.BusinessUTRPage
import pages.register.company.CompanyNamePage
import pages.register.company.ConfirmNamePage
import play.api.mvc.Call

class CompanyNavigatorSpec extends NavigatorBehaviour {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(BusinessUTRPage)(controllers.register.company.routes.CompanyNameController.onPageLoad()),
        row(CompanyNamePage)(controllers.register.company.routes.ConfirmNameController.onPageLoad()),
        row(ConfirmNamePage)(controllers.register.company.routes.ConfirmAddressController.onPageLoad())
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }
}
