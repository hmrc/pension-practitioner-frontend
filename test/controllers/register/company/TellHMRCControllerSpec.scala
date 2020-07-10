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

package controllers.register.company

import controllers.base.ControllerSpecBase
import data.SampleData
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class TellHMRCControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers {

  "TellHMRC Controller" must {

    "return OK and the correct view for a GET" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(SampleData.emptyUserAnswers)).build()
      val request = FakeRequest(GET, controllers.register.company.routes.TellHMRCController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val hmrcUrl = "url1"
      val companiesHouseUrl = "url2"

      when(mockAppConfig.hmrcChangesMustReportUrl).thenReturn(hmrcUrl)
      when(mockAppConfig.companiesHouseFileChangesUrl).thenReturn(companiesHouseUrl)


      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "hmrcUrl" -> hmrcUrl,
        "companiesHouseUrl" -> companiesHouseUrl
      )

      templateCaptor.getValue mustEqual "register/company/tellHMRC.njk"
      jsonCaptor.getValue must containJson(expectedJson)
      application.stop()
    }
  }
}