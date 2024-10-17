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

import connectors.RegistrationConnector
import controllers.base.ControllerSpecBase
import data.SampleData
import data.SampleData._
import forms.ConfirmAddressFormProvider
import matchers.JsonMatchers
import models.register._
import models.{TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.{BusinessNamePage, BusinessUTRPage, ConfirmAddressPage}
import pages.register.BusinessTypePage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import viewmodels.Radios
import utils.countryOptions.CountryOptions
import views.html.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends ControllerSpecBase with MockitoSugar
   with JsonMatchers with OptionValues with TryValues with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ConfirmAddressFormProvider()
  private val form = formProvider("confirmAddress.partnership.error.required")

  private def confirmAddressRoute: String = routes.ConfirmAddressController.onPageLoad().url
  private def confirmAddressSubmitCall: Call = routes.ConfirmAddressController.onSubmit()
  private def confirmAddressSubmitRoute: String = confirmAddressSubmitCall.url

  private val organisation = Organisation(pspName, BusinessType.LimitedPartnership)
  private val organisationRegistration = OrganisationRegistration(
    OrganisationRegisterWithIdResponse(
      organisation,
      addressUK
    ),
    RegistrationInfo(RegistrationLegalStatus.Partnership,
      "", false, RegistrationCustomerType.UK, None, None)
  )

  private val mockCountryOptions = mock[CountryOptions]

  private val mockRegistrationConnector = mock[RegistrationConnector]

  private val utr = "1234567890"

  private val userAnswersWithRegistrationValues = UserAnswers().setOrException(BusinessTypePage, BusinessType.LimitedPartnership)
    .setOrException(BusinessUTRPage, utr).setOrException(BusinessNamePage, "test-partnership")


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationConnector)
    reset(mockUserAnswersCacheConnector)
    reset(mockCountryOptions)
  }

  "ConfirmAddress Controller" must {
    "return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithRegistrationValues))
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector),
          bind[CountryOptions].toInstance(mockCountryOptions)

        ).build()

      when(mockRegistrationConnector.registerWithIdOrganisation(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(organisationRegistration))
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCountryOptions.getCountryNameFromCode(ArgumentMatchers.any[TolerantAddress])).thenReturn(Some("GB"))

      val request = FakeRequest(GET, confirmAddressRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRegistrationConnector, times(1))
        .registerWithIdOrganisation(any(), any(), any())(any(), any())
      verify(mockUserAnswersCacheConnector, times(1)).save(any())(any(), any())

      val view = application.injector.instanceOf[ConfirmAddressView].apply(
        "the partnership",
        form,
        confirmAddressSubmitCall,
        "test-partnership",
        Seq("addr1", "addr2", "", "GB"),
        Radios.yesNo(form("value"))
      )(request, messages)

      compareResultAndView(result, view)
      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

      val application = applicationBuilder(userAnswers = Some(UserAnswers().setOrException(BusinessNamePage, "test-partnership")))
        .build()

      val request =
        FakeRequest(POST, confirmAddressSubmitRoute)
          .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {

      when(mockCountryOptions.getCountryNameFromCode(ArgumentMatchers.any[TolerantAddress])).thenReturn(Some("GB"))

      val userAnswersWithAddress = userAnswersWithRegistrationValues
        .setOrException(ConfirmAddressPage, SampleData.addressUK)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithAddress))
        .overrides(
          bind[CountryOptions].toInstance(mockCountryOptions)
        )
        .build()
      val request = FakeRequest(POST, confirmAddressSubmitRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      val view = application.injector.instanceOf[ConfirmAddressView].apply(
        "the partnership",
        boundForm,
        confirmAddressSubmitCall,
        "test-partnership",
        Seq("addr1", "addr2", "", "GB"),
        Radios.yesNo(boundForm("value"))
      )(request, messages)
      compareResultAndView(result, view)
      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, confirmAddressRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, confirmAddressSubmitRoute)
          .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
