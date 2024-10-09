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
import data.SampleData
import forms.partnership.IsPartnershipRegisteredInUkFormProvider
import matchers.JsonMatchers
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.IsPartnershipRegisteredInUkPage
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import viewmodels.Radios
import utils.TwirlMigration
import views.html.partnership.IsPartnershipRegisteredInUkView

import scala.concurrent.Future

class IsPartnershipRegisteredInUkControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new IsPartnershipRegisteredInUkFormProvider()
  val form = formProvider()

  def isPartnershipRegisteredInUkRoute: String = routes.IsPartnershipRegisteredInUkController.onPageLoad().url
  def isPartnershipRegisteredInUkSubmitCall: Call = routes.IsPartnershipRegisteredInUkController.onSubmit()

  def isPartnershipRegisteredInUkSubmitRoute: String = isPartnershipRegisteredInUkSubmitCall.url


  val answers: UserAnswers = SampleData.userAnswersWithPartnershipName
    .set(IsPartnershipRegisteredInUkPage, true).success.value

  "IsPartnershipRegisteredInUk Controller" must {

    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithPartnershipName))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, isPartnershipRegisteredInUkRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[IsPartnershipRegisteredInUkView].apply(
        isPartnershipRegisteredInUkSubmitCall,
        form,
        Radios.yesNo (form("value"))
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
      val request = FakeRequest(GET, isPartnershipRegisteredInUkRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> "true"))

      val view = application.injector.instanceOf[IsPartnershipRegisteredInUkView].apply(
        isPartnershipRegisteredInUkSubmitCall,
        filledForm,
        Radios.yesNo(filledForm("value"))
      )(request, messages)

      compareResultAndView(result, view)
      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithPartnershipName))
        .overrides(
        )
        .build()

      val request =
        FakeRequest(POST, isPartnershipRegisteredInUkRoute)
      .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithPartnershipName))
        .overrides(
        )
        .build()
      val request = FakeRequest(POST, isPartnershipRegisteredInUkRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      val view = application.injector.instanceOf[IsPartnershipRegisteredInUkView].apply(
        isPartnershipRegisteredInUkSubmitCall,
        boundForm,
        Radios.yesNo(boundForm("value"))
      )(request, messages)

      compareResultAndView(result, view)
      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, isPartnershipRegisteredInUkRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, isPartnershipRegisteredInUkRoute)
      .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
