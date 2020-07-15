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

package controllers.register

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
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class BusinessDetailsNotFoundControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers {

  "BusinessDetailsNotFound Controller" must {

    "return OK and the correct view for a GET" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val companiesHouseUrl = "companiesHouseURL"
      val hmrcChangesUrl = "hmrc"
      val taxHelplineUrl = "taxHelplineUrl"

      def onwardRoute = Call("GET", "/foo")

      when(mockAppConfig.companiesHouseFileChangesUrl).thenReturn(companiesHouseUrl)
      when(mockAppConfig.hmrcTaxHelplineUrl).thenReturn(taxHelplineUrl)
      when(mockAppConfig.hmrcChangesMustReportUrl).thenReturn(hmrcChangesUrl)

      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(SampleData.emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.BusinessDetailsNotFoundController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      val jsonToPassToTemplate = Json.obj(
        "companiesHouseUrl" -> companiesHouseUrl,
        "hmrcUrl" -> hmrcChangesUrl,
        "hmrcTaxHelplineUrl" -> taxHelplineUrl,
        "enterDetailsAgainUrl" -> onwardRoute.url
      )

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "register/businessDetailsNotFound.njk"

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)

      application.stop()
    }
  }
}
