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

package controllers.individual

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRequiredActionImpl, FakeAuthAction}
import controllers.base.ControllerSpecBase
import data.SampleData._
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import views.html.individual.WhatYouWillNeedView

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends ControllerSpecBase with MockitoSugar {
  private def onwardRoute = Call("GET", "/foo")

  override def modules: Seq[GuiceableModule] = Seq(
    bind[DataRequiredAction].to[DataRequiredActionImpl],
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveNoEnrolmentWithNoIV]).to[FakeAuthAction],
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator)
  )

  "WhatYouWillNeed Controller" must {

    "return OK and the correct view for a GET" in {

      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.WhatYouWillNeedController.onPageLoad().url)

      val view = application.injector.instanceOf[WhatYouWillNeedView]
        .apply(onwardRoute.url)(request, messages)

      val result = route(application, request).value

      status(result) mustEqual OK
      compareResultAndView(result, view)

      application.stop()
    }
  }
}
