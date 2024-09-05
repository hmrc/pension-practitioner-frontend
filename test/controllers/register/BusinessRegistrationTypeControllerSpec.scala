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
import forms.register.BusinessRegistrationTypeFormProvider
import matchers.JsonMatchers
import models.UserAnswers
import models.register.BusinessRegistrationType
import play.api.inject.bind
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.register.BusinessRegistrationTypePage
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.register.BusinessRegistrationTypeView

import scala.concurrent.Future

class BusinessRegistrationTypeControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {

  def onwardRoute = Call("GET", "/foo")

  def businessRegistrationTypeRoute: String = routes.BusinessRegistrationTypeController.onPageLoad().url
  def businessRegistrationTypeSubmitRoute: Call = routes.BusinessRegistrationTypeController.onSubmit()

  val formProvider = new BusinessRegistrationTypeFormProvider()
  val form = formProvider()

  val answers: UserAnswers = emptyUserAnswers.set(BusinessRegistrationTypePage, BusinessRegistrationType.values.head).success.value

  "BusinessRegistrationType Controller" must {

    "return OK and the correct view for a GET" in {
      val view = mock[BusinessRegistrationTypeView]

      when(view.apply(any(), any(), any())(any(), any())).thenReturn(Html(""))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[BusinessRegistrationTypeView].toInstance(view)
        )
        .build()

      val radios = Seq(
        uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
          content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("Yes"),
          value = Some("true"),
          checked = form("value").value.contains("true"),
          id = Some("value")
        ),
        uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
          content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("No"),
          value = Some("false"),
          checked = form("value").value.contains("false"),
          id = Some("value-no")
        )
      )

      val request = FakeRequest(GET, businessRegistrationTypeRoute)
      val result = route(application, request).value

      val expectedView = view.apply(businessRegistrationTypeSubmitRoute, form, radios )(request, messages)

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val view = mock[BusinessRegistrationTypeView]

      when(view.apply(any(), any(), any())(any(), any())).thenReturn(Html(""))

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[BusinessRegistrationTypeView].toInstance(view)
        )
        .build()

      val radios = Seq(
        uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
          content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("Yes"),
          value = Some("true"),
          checked = form("value").value.contains("true"),
          id = Some("value")
        ),
        uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
          content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("No"),
          value = Some("false"),
          checked = form("value").value.contains("false"),
          id = Some("value-no")
        )
      )

      val request = FakeRequest(GET, businessRegistrationTypeRoute)
      val result = route(application, request).value

      val expectedView = view.apply(businessRegistrationTypeSubmitRoute, form.fill(BusinessRegistrationType.values.head), radios)(request, messages)

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      val request = FakeRequest(POST, businessRegistrationTypeRoute)
        .withFormUrlEncodedBody(("value", BusinessRegistrationType.values.head.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val view = mock[BusinessRegistrationTypeView]

      when(view.apply(any(), any(), any())(any(), any())).thenReturn(Html(""))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[BusinessRegistrationTypeView].toInstance(view)
        )
        .build()

      val radios = Seq(
        uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
          content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("Yes"),
          value = Some("true"),
          checked = form("value").value.contains("true"),
          id = Some("value")
        ),
        uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
          content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("No"),
          value = Some("false"),
          checked = form("value").value.contains("false"),
          id = Some("value-no")
        )
      )

      val request = FakeRequest(POST, businessRegistrationTypeRoute).withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val expectedView = view.apply(businessRegistrationTypeSubmitRoute, boundForm, radios)(request, messages)

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST
      compareResultAndView(result, expectedView)

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, businessRegistrationTypeRoute)
      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(POST, businessRegistrationTypeRoute)
        .withFormUrlEncodedBody(("value", BusinessRegistrationType.values.head.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
