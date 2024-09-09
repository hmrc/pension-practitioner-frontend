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

package controllers

import controllers.base.ControllerSpecBase
import data.SampleData._
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CannotRegisterPractitionerView

class CannotRegisterPractitionerControllerSpec extends ControllerSpecBase {


  override def fakeApplication(): Application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName)).overrides().build()
  private def getRoute: String = routes.CannotRegisterPractitionerController.onPageLoad().url

  "CannotRegisterPractitionerController" must {

    "return OK and the correct view for a GET" in {
      val request = FakeRequest(GET, getRoute)

      val result = route(app, request).value

      val view = app.injector.instanceOf[CannotRegisterPractitionerView].apply()(request, messages)

      status(result) mustEqual OK
      compareResultAndView(result, view)
    }
  }
}
