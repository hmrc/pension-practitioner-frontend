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
import data.SampleData._
import forms.register.AreYouUKCompanyFormProvider
import matchers.JsonMatchers
import models.UserAnswers
import play.api.inject.bind
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.register.AreYouUKCompanyPage
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.register.AreYouUkCompanyView

import scala.concurrent.Future

class AreYouUKCompanyControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new AreYouUKCompanyFormProvider()
  private val form = formProvider()

  private def areYouUKCompanyRoute: String = routes.AreYouUKCompanyController.onPageLoad().url

  private val answers: UserAnswers = userAnswersWithCompanyName.set(AreYouUKCompanyPage, true).success.value

  "AreYouUKCompany Controller" must {

    "return OK and the correct view for a GET" in {
      val view = mock[AreYouUkCompanyView]

      when(view.apply(any(), any(), any())(any(), any())).thenReturn(Html(""))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName))
        .overrides(
          bind[AreYouUkCompanyView].toInstance(view)
        )
        .build()

      val request = FakeRequest(GET, areYouUKCompanyRoute)
      val result = route(application, request).value

      status(result) mustEqual OK

      verify(view, times(1)).apply(any(), any(), any())(any(), any())

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val view = mock[AreYouUkCompanyView]

      when(view.apply(any(), any(), any())(any(), any())).thenReturn(Html(""))

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[AreYouUkCompanyView].toInstance(view)
        )
        .build()

      val request = FakeRequest(GET, areYouUKCompanyRoute)
      val result = route(application, request).value

      status(result) mustEqual OK

      verify(view, times(1)).apply(any(), any(), any())(any(), any())

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName))
        .overrides(
        )
        .build()

      val request = FakeRequest(POST, areYouUKCompanyRoute).withFormUrlEncodedBody(("value", "true"))
      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val view = mock[AreYouUkCompanyView]

      when(view.apply(any(), any(), any())(any(), any())).thenReturn(Html(""))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName))
        .overrides(
          bind[AreYouUkCompanyView].toInstance(view)
        )
        .build()

      val request = FakeRequest(POST, areYouUKCompanyRoute).withFormUrlEncodedBody(("value", ""))
      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(view, times(1)).apply(any(), any(), any())(any(), any())

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, areYouUKCompanyRoute)
      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(POST, areYouUKCompanyRoute).withFormUrlEncodedBody(("value", "true"))
      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}