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

package controllers.company

import controllers.base.ControllerSpecBase
import data.SampleData
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.TellHMRCView

class TellHMRCControllerSpec extends ControllerSpecBase with MockitoSugar {

  "TellHMRC Controller" must {

    "return OK and the correct view for a GET" in {


      val application = applicationBuilder(userAnswers = Some(SampleData.emptyUserAnswers)).build()
      val request = FakeRequest(GET, controllers.routes.TellHMRCController.onPageLoad("company").url)
      val hmrcUrl = "url1"
      val companiesHouseUrl = "url2"

      when(mockAppConfig.hmrcChangesMustReportUrl).thenReturn(hmrcUrl)
      when(mockAppConfig.companiesHouseFileChangesUrl).thenReturn(companiesHouseUrl)


      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[TellHMRCView].apply("company", companiesHouseUrl, hmrcUrl)(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }
  }
}
