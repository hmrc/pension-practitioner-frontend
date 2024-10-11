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

package controllers.partnership

import controllers.base.ControllerSpecBase
import forms.BusinessNameFormProvider
import matchers.JsonMatchers
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.BusinessNamePage
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.BusinessNameView

import scala.concurrent.Future

class BusinessNameControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {
  def onwardRoute: Call = Call("GET", "/foo")

  private val formProvider = new BusinessNameFormProvider()
  private val form = formProvider("partnershipName.error.required", "partnershipName.error.invalid", "partnershipName.error.length")

  private def partnershipNameRoute = routes.PartnershipNameController.onPageLoad(NormalMode).url
  private def partnershipNameSubmitCall = routes.PartnershipNameController.onSubmit(NormalMode)

  val answers: UserAnswers = UserAnswers().set(BusinessNamePage, "answer").success.value

  "PartnershipName Controller" must {

    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(UserAnswers()))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, partnershipNameRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[BusinessNameView].apply(
        "partnership",
        form,
        partnershipNameSubmitCall,
        None
      )(request, messages)

      compareResultAndView(result, view)
      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, partnershipNameRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> "answer"))

      val view = application.injector.instanceOf[BusinessNameView].apply(
        "partnership",
        filledForm,
        partnershipNameSubmitCall,
        None
      )(request, messages)
      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {

      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(UserAnswers()))
        .overrides(
        )
        .build()

      val request =
        FakeRequest(POST, partnershipNameSubmitCall.url)
      .withFormUrlEncodedBody(("value", "answer"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(UserAnswers()))
        .overrides(
        )
        .build()
      val request = FakeRequest(POST, partnershipNameSubmitCall.url).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      val view = application.injector.instanceOf[BusinessNameView].apply(
        "partnership",
        boundForm,
        partnershipNameSubmitCall,
        None
      )(request, messages)

      compareResultAndView(result, view)
      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, partnershipNameRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, partnershipNameRoute)
      .withFormUrlEncodedBody(("value", "answer"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
