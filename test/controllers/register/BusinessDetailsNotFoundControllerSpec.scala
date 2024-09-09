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

package controllers.register

import controllers.base.ControllerSpecBase
import data.SampleData
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.register.BusinessDetailsNotFound

class BusinessDetailsNotFoundControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers {

  "BusinessDetailsNotFound Controller" must {

    "return OK and the correct view for a GET" in {

      val view = mock[BusinessDetailsNotFound]

      when(view.apply(any(), any(), any(), any())(any(), any()))
        .thenReturn(Html(""))

      val companiesHouseUrl = "companiesHouseURL"
      val hmrcChangesUrl = "hmrc"
      val taxHelplineUrl = "taxHelplineUrl"

      def onwardRoute = Call("GET", "/foo")

      when(mockAppConfig.companiesHouseFileChangesUrl).thenReturn(companiesHouseUrl)
      when(mockAppConfig.hmrcTaxHelplineUrl).thenReturn(taxHelplineUrl)
      when(mockAppConfig.hmrcChangesMustReportUrl).thenReturn(hmrcChangesUrl)

      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(SampleData.emptyUserAnswers))
        .overrides(
          bind[BusinessDetailsNotFound].toInstance(view)
        )
        .build()

      val request = FakeRequest(GET, routes.BusinessDetailsNotFoundController.onPageLoad().url)
      val result = route(application, request).value

      val expectedView = view.apply(companiesHouseUrl, hmrcChangesUrl, taxHelplineUrl, onwardRoute.toString)(request, messages)

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)

      application.stop()
    }
  }

}
