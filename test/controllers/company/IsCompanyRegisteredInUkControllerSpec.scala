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
import forms.company.IsCompanyRegisteredInUkFormProvider
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import pages.company.IsCompanyRegisteredInUkPage
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.html.components
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import views.html.company.IsCompanyRegisteredInUkView

import scala.concurrent.Future

class IsCompanyRegisteredInUkControllerSpec extends ControllerSpecBase with MockitoSugar with TryValues {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new IsCompanyRegisteredInUkFormProvider()
  val form = formProvider()

  def isCompanyRegisteredInUkRoute = routes.IsCompanyRegisteredInUkController.onPageLoad().url
  def isCompanyRegisteredInUkSubmitCall = routes.IsCompanyRegisteredInUkController.onSubmit()
  def isCompanyRegisteredInUkSubmitRoute = isCompanyRegisteredInUkSubmitCall.url

  val answers: UserAnswers = SampleData.userAnswersWithCompanyName.set(IsCompanyRegisteredInUkPage, true).success.value

  "IsCompanyRegisteredInUk Controller" must {

    "return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithCompanyName))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, isCompanyRegisteredInUkRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[IsCompanyRegisteredInUkView].apply(
        isCompanyRegisteredInUkSubmitCall,
        form,
        Seq(
          components.RadioItem(content = Text(Messages("site.yes")), value = Some("true"), id = Some("value")),
          components.RadioItem(content = Text(Messages("site.no")), value = Some("false"), id = Some("value-no"))
        )
      )(request, messages)

      compareResultAndView(result, view)
      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, isCompanyRegisteredInUkRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> "true"))

      val view = application.injector.instanceOf[IsCompanyRegisteredInUkView].apply(
        isCompanyRegisteredInUkSubmitCall,
        filledForm,
        Seq(
          components.RadioItem(content = Text(Messages("site.yes")), value = Some("true"), id = Some("value"), checked = true),
          components.RadioItem(content = Text(Messages("site.no")), value = Some("false"), id = Some("value-no"))
        )
      )(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithCompanyName))
        .overrides(
        )
        .build()

      val request =
        FakeRequest(POST, isCompanyRegisteredInUkRoute)
      .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {


      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithCompanyName))
        .overrides(
        )
        .build()
      val request = FakeRequest(POST, isCompanyRegisteredInUkRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      val view = application.injector.instanceOf[IsCompanyRegisteredInUkView].apply(
        isCompanyRegisteredInUkSubmitCall,
        boundForm,
        Seq(
          components.RadioItem(content = Text(Messages("site.yes")), value = Some("true"), id = Some("value")),
          components.RadioItem(content = Text(Messages("site.no")), value = Some("false"), id = Some("value-no"))
        )
      )(request, messages)

      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, isCompanyRegisteredInUkRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, isCompanyRegisteredInUkRoute)
      .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
