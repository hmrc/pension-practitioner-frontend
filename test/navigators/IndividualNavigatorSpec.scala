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
import models.{NormalMode, UserAnswers}
import org.scalatest.prop.TableFor3
import pages._
import pages.individual.{AreYouUKResidentPage, IsThisYouPage, WhatYouWillNeedPage}
import play.api.mvc.Call

class IndividualNavigatorSpec extends NavigatorBehaviour {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  private def uaIsThisYou(flag: Boolean) = SampleData
    .emptyUserAnswers.setOrException(IsThisYouPage, flag)

  "NormalMode" must {
    def normalModeRoutes: TableFor3[Page, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(WhatYouWillNeedPage)(controllers.individual.routes.AreYouUKResidentController.onPageLoad()),
        row(AreYouUKResidentPage)(controllers.individual.routes.IsThisYouController.onPageLoad(NormalMode)),
        row(IsThisYouPage)(controllers.individual.routes.YouNeedToTellHMRCController.onPageLoad(), Some(uaIsThisYou(false))),
        row(IsThisYouPage)(controllers.individual.routes.UseAddressForContactController.onPageLoad(NormalMode), Some(uaIsThisYou(true)))
      )

    behave like navigatorWithRoutesForMode(NormalMode)(navigator, normalModeRoutes)
  }
}
