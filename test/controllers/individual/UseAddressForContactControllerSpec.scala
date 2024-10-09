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

import controllers.base.ControllerSpecBase
import forms.address.UseAddressForContactFormProvider
import matchers.JsonMatchers
import models.{Address, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.{AreYouUKResidentPage, IndividualAddressPage, IndividualManualAddressPage, UseAddressForContactPage}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import viewmodels.Radios
import utils.TwirlMigration
import utils.countryOptions.CountryOptions
import views.html.address.UseAddressForContactView

import scala.concurrent.Future

class UseAddressForContactControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new UseAddressForContactFormProvider()
  private val form = formProvider(messages("useAddressForContact.error.required", messages("individual.you")))

  private def useAddressForContactGetRoute: String = routes.UseAddressForContactController.onPageLoad(NormalMode).url

  private def useAddressForContactPostRoute: String = routes.UseAddressForContactController.onSubmit().url

  private val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  private val manualAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  private val countryOptions: CountryOptions = mock[CountryOptions]
  private val uaWithIndividualAddress = UserAnswers().set(IndividualAddressPage, address).success.value
  private val uaWithAddressAndUkResident = uaWithIndividualAddress.set(AreYouUKResidentPage, true).success.value

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(countryOptions.getCountryNameFromCode(eqTo(address))).thenReturn("United Kingdom")
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "UseAddressForContactController" when {

    "on a GET" must {

      "return OK with the correct view" in {
        val application = applicationBuilder(userAnswers = Some(uaWithAddressAndUkResident),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()
        val request = FakeRequest(GET, useAddressForContactGetRoute)

        val view = application.injector.instanceOf[UseAddressForContactView]
          .apply(routes.UseAddressForContactController.onSubmit(),
            form, Radios.yesNo(form("value")),
            "individual.you", "individual.you",
            address.lines(countryOptions))(request, messages)

        val result = route(application, request).value

        status(result) mustEqual OK
        compareResultAndView(result, view)

        application.stop()
      }

      "return OK and populate the view correctly when the question has previously been answered" in {
        val application = applicationBuilder(userAnswers = Some(uaWithAddressAndUkResident.set(UseAddressForContactPage, true).success.value),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()
        val request = FakeRequest(GET, useAddressForContactGetRoute)

        val filledForm = form.bind(Map("value" -> "true"))

        val view = application.injector.instanceOf[UseAddressForContactView]
          .apply(routes.UseAddressForContactController.onSubmit(),
            filledForm, Radios.yesNo(filledForm("value")),
            "individual.you", "individual.you",
            address.lines(countryOptions))(request, messages)

        val result = route(application, request).value

        status(result) mustEqual OK
        compareResultAndView(result, view)

        application.stop()
      }

      "redirect to Session Expired if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        val request = FakeRequest(GET, useAddressForContactGetRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

        application.stop()
      }
    }

    "on a POST" must {

      "save the manual address and redirect to the next page when valid data of true is submitted" in {
        when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
        when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

        val application = applicationBuilder(userAnswers = Some(uaWithIndividualAddress),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()

        val request = FakeRequest(POST, useAddressForContactPostRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockUserAnswersCacheConnector, times(1)).save(
          eqTo(uaWithIndividualAddress.setOrException(UseAddressForContactPage, true).
            setOrException(IndividualManualAddressPage, manualAddress).data))(any(), any())

        redirectLocation(result).value mustEqual onwardRoute.url

        application.stop()
      }

      "don't save the manual address and redirect to the next page when valid data of false is submitted" in {
        when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
        when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

        val application = applicationBuilder(userAnswers = Some(uaWithIndividualAddress),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()

        val request = FakeRequest(POST, useAddressForContactPostRoute)
          .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockUserAnswersCacheConnector, times(1)).save(
          eqTo(uaWithIndividualAddress.setOrException(UseAddressForContactPage, false).data))(any(), any())
        redirectLocation(result).value mustEqual onwardRoute.url

        application.stop()
      }
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(uaWithIndividualAddress),
        extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()

      val request = FakeRequest(POST, useAddressForContactPostRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val view = application.injector.instanceOf[UseAddressForContactView]
        .apply(routes.UseAddressForContactController.onSubmit(),
          boundForm, Radios.yesNo(boundForm("value")),
          "individual.you", "individual.you",
          address.lines(countryOptions))(request, messages)

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST
      compareResultAndView(result, view)

      application.stop()
    }

    "redirect to Session Expired if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, useAddressForContactPostRoute)
          .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}


