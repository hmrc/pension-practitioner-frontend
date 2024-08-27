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

import base.SpecBase
import models.{NormalMode, UserAnswers}
import pages.Page
import play.api.libs.json.Json
import play.api.mvc.Call

import scala.jdk.CollectionConverters._

class CompoundNavigatorSpec extends SpecBase {

  case object PageOne extends Page
  case object PageTwo extends Page
  case object PageThree extends Page

  private def navigator(pp: PartialFunction[Page, Call]): Navigator = new Navigator {
    override protected def routeMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = pp

    override protected def editRouteMap(userAnswers: UserAnswers): PartialFunction[Page, Call] = pp
  }

  "CompoundNavigator" must {
    "redirect to the correct page if there is navigation for the page" in {
      val navigators = Set(
        navigator({case PageOne => Call("GET", "/page1")}),
        navigator({case PageTwo => Call("GET", "/page2")}),
        navigator({case PageThree => Call("GET", "/page3")})
      )
      val compoundNavigator = new CompoundNavigatorImpl(navigators.asJava)
      val result = compoundNavigator.nextPage(PageTwo, NormalMode, UserAnswers(Json.obj()))
      result mustEqual Call("GET", "/page2")
    }
  }
}
