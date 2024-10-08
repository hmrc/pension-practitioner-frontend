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

import controllers.base.ControllerSpecBase
import data.SampleData._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.individual.YouNeedToTellHMRCView

class YouNeedToTellHMRCControllerSpec extends ControllerSpecBase {

  "YouNeedToTellHMRCController" must {

    "return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.YouNeedToTellHMRCController.onPageLoad().url)

      val view = application.injector.instanceOf[YouNeedToTellHMRCView]
        .apply("")(request, messages)

      val result = route(application, request).value

      status(result) mustEqual OK
      compareResultAndView(result, view)

      application.stop()
    }
  }
}
