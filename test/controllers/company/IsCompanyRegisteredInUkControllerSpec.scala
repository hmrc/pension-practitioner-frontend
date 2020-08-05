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

package controllers.company

import controllers.base.ControllerSpecBase
import data.SampleData
import forms.company.IsCompanyRegisteredInUkFormProvider
import matchers.JsonMatchers
import models.NormalMode
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import pages.company.IsCompanyRegisteredInUkPage
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios

import scala.concurrent.Future

class IsCompanyRegisteredInUkControllerSpec extends ControllerSpecBase with MockitoSugar with
  NunjucksSupport with JsonMatchers with OptionValues with TryValues {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new IsCompanyRegisteredInUkFormProvider()
  val form = formProvider()

  def isCompanyRegisteredInUkRoute = routes.IsCompanyRegisteredInUkController.onPageLoad(NormalMode).url
  def isCompanyRegisteredInUkSubmitRoute = routes.IsCompanyRegisteredInUkController.onSubmit(NormalMode).url

  val answers: UserAnswers = SampleData.userAnswersWithCompanyName.set(IsCompanyRegisteredInUkPage, true).success.value

  "IsCompanyRegisteredInUk Controller" must {

    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithCompanyName))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, isCompanyRegisteredInUkRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form"   -> form,
        "submitUrl" -> isCompanyRegisteredInUkSubmitRoute,
        "radios" -> Radios.yesNo(form("value"))
      )

      templateCaptor.getValue mustEqual "company/isCompanyRegisteredInUk.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, isCompanyRegisteredInUkRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val filledForm = form.bind(Map("value" -> "true"))

      val expectedJson = Json.obj(
        "form"   -> filledForm,
        "submitUrl" -> isCompanyRegisteredInUkSubmitRoute,
        "radios" -> Radios.yesNo(filledForm("value"))
      )

      templateCaptor.getValue mustEqual "company/isCompanyRegisteredInUk.njk"
      jsonCaptor.getValue must containJson(expectedJson)

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

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(SampleData.userAnswersWithCompanyName))
        .overrides(
        )
        .build()
      val request = FakeRequest(POST, isCompanyRegisteredInUkRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form"   -> boundForm,
        "submitUrl" -> isCompanyRegisteredInUkSubmitRoute,
        "radios" -> Radios.yesNo(boundForm("value"))
      )

      templateCaptor.getValue mustEqual "company/isCompanyRegisteredInUk.njk"
      jsonCaptor.getValue must containJson(expectedJson)

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
