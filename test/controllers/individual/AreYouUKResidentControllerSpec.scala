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

package controllers.individual

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRequiredActionImpl, FakeAuthAction}
import controllers.base.ControllerSpecBase
import forms.individual.AreYouUKResidentFormProvider
import matchers.JsonMatchers
import models.{NormalMode, UserAnswers}
import navigators.CompoundNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.AreYouUKResidentPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.AuthMustHaveNoEnrolmentWithNoIV
import viewmodels.Radios
import views.html.individual.AreYouUKResidentView

import scala.concurrent.Future

class AreYouUKResidentControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new AreYouUKResidentFormProvider()
  private val form = formProvider()

  private def areYouUKResidentRoute: String = routes.AreYouUKResidentController.onPageLoad(NormalMode).url

  private def areYouUKResidentSubmitRoute: Call = routes.AreYouUKResidentController.onSubmit(NormalMode)

  override def modules: Seq[GuiceableModule] = Seq(
    bind[DataRequiredAction].to[DataRequiredActionImpl],
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveNoEnrolmentWithNoIV]).to[FakeAuthAction],
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator)
  )

  private val answers: UserAnswers = UserAnswers().set(AreYouUKResidentPage, value = true).success.value

  "AreYouUKResident Controller" must {

    "return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(UserAnswers())).overrides().build()
      val request = FakeRequest(GET, areYouUKResidentRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[AreYouUKResidentView].apply(areYouUKResidentSubmitRoute, form, false,
        Radios.yesNo(form("value")))(request, messages)

      status(result) mustEqual OK
      compareResultAndView(result, view)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(answers)).overrides().build()
      val request = FakeRequest(GET, areYouUKResidentRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> "true"))

      val view = application.injector.instanceOf[AreYouUKResidentView].apply(areYouUKResidentSubmitRoute, filledForm, false,
        Radios.yesNo(filledForm("value")))(request, messages)

      status(result) mustEqual OK
      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
        )
        .build()

      val request = FakeRequest(POST, areYouUKResidentRoute)
        .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).overrides().build()
      val request = FakeRequest(POST, areYouUKResidentRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST


      val view = application.injector.instanceOf[AreYouUKResidentView].apply(areYouUKResidentSubmitRoute, boundForm, false,
        Radios.yesNo(boundForm("value")))(request, messages)

      status(result) mustEqual BAD_REQUEST
      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, areYouUKResidentRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, areYouUKResidentRoute)
          .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
