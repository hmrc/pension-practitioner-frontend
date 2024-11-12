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
import data.SampleData._
import forms.BusinessUTRFormProvider
import models.UserAnswers
import models.register.BusinessType
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages.company.BusinessUTRPage
import pages.register.BusinessTypePage
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.BusinessUTRView

import scala.concurrent.Future

class BusinessUTRControllerSpec extends ControllerSpecBase with MockitoSugar {
  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new BusinessUTRFormProvider
  private val form = formProvider.apply()

  private val validUTR = "1234567890"

  private def businessUTRRoute = routes.BusinessUTRController.onPageLoad().url

  private def businessUTRSubmitCall = routes.BusinessUTRController.onSubmit()

  private def businessUTRSubmitRoute = businessUTRSubmitCall.url

  private val businessType = BusinessType.LimitedCompany

  "BusinessUTR Controller" must {

    "return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(UserAnswers().setOrException(BusinessTypePage, businessType)))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, businessUTRRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[BusinessUTRView].apply(
        "limited company",
        form,
        businessUTRSubmitCall
      )(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {


      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName.setOrException(BusinessTypePage, businessType).
        setOrException(BusinessUTRPage, validUTR)))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, businessUTRRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> validUTR))

      val view = application.injector.instanceOf[BusinessUTRView].apply(
        "limited company",
        filledForm,
        businessUTRSubmitCall
      )(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {

      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName.setOrException(BusinessTypePage, businessType)))
        .overrides(
        )
        .build()

      val request =
        FakeRequest(POST, businessUTRSubmitRoute)
      .withFormUrlEncodedBody(("value", validUTR))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName.setOrException(BusinessTypePage, businessType)))
        .overrides(
        )
        .build()
      val request = FakeRequest(POST, businessUTRRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      val view = application.injector.instanceOf[BusinessUTRView].apply(
        "limited company",
        boundForm,
        businessUTRSubmitCall
      )(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, businessUTRRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, businessUTRRoute)
      .withFormUrlEncodedBody(("value", "answer"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
