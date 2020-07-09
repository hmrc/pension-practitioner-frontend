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

package controllers.individual

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, FakeIdentifierAction, IdentifierAction}
import controllers.base.ControllerSpecBase
import forms.individual.AreYouUKResidentFormProvider
import matchers.JsonMatchers
import models.register.{RegistrationCustomerType, RegistrationInfo, RegistrationLegalStatus, TolerantIndividual}
import models.{NormalMode, TolerantAddress, UserAnswers}
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.RegistrationInfoPage
import pages.individual.{AreYouUKResidentPage, IndividualAddressPage, IndividualDetailsPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.annotations.AuthWithNoIV
import utils.countryOptions.CountryOptions
import org.mockito.Matchers.{any, eq => eqTo}

import scala.concurrent.Future

class IsThisYouControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new AreYouUKResidentFormProvider()
  private val form = formProvider()

  private def isThisYouGetRoute: String = routes.IsThisYouController.onPageLoad(NormalMode).url
  private def isThisYouPostRoute: String = routes.IsThisYouController.onSubmit(NormalMode).url

  private val answers: UserAnswers = UserAnswers().set(AreYouUKResidentPage, value = true).success.value

  private val individual = TolerantIndividual(Some("first"), None, Some("last"))
  private val address = TolerantAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("post code"), Some("GB"))
  private val registrationInfo = RegistrationInfo(
    RegistrationLegalStatus.Individual,
    "test-sap",
    noIdentifier = false,
    RegistrationCustomerType.NonUK,
    None,
    None
  )
  private val countryOptions: CountryOptions = mock[CountryOptions]

  "IsThisYouController" when {

    "on a GET" must {

      "return OK with the correct view if individual details are in the cache" in {
        when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
        when(countryOptions.getCountryNameFromCode(eqTo(address))).thenReturn(Some("United Kingdom"))
        val application = applicationBuilder(userAnswers = Some(UserAnswers().set(IndividualDetailsPage, individual).flatMap(
          _.set(IndividualAddressPage, address)).flatMap(_.set(RegistrationInfoPage, registrationInfo)).success.value),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()
        val request = FakeRequest(GET, isThisYouGetRoute)
        val templateCaptor = ArgumentCaptor.forClass(classOf[String])
        val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

        val expectedJson = Json.obj(
          "form"   -> form,
          "submitUrl" -> isThisYouPostRoute,
          "radios" -> Radios.yesNo(form("value")),
          "name" -> individual.fullName,
          "address" -> address.lines(countryOptions)
        )

        templateCaptor.getValue mustEqual "individual/isThisYou.njk"
        jsonCaptor.getValue must containJson(expectedJson)

        application.stop()
      }

      "return OK with the correct view, call register with id and save the individual details in the cache if we have a valid nino" in {
        when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
        when(countryOptions.getCountryNameFromCode(eqTo(address))).thenReturn(Some("United Kingdom"))
        when()
        val application = applicationBuilder(userAnswers = Some(UserAnswers()),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).overrides().build()
        val request = FakeRequest(GET, isThisYouGetRoute)
        val templateCaptor = ArgumentCaptor.forClass(classOf[String])
        val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

        val expectedJson = Json.obj(
          "form"   -> form,
          "submitUrl" -> isThisYouPostRoute,
          "radios" -> Radios.yesNo(form("value")),
          "name" -> individual.fullName,
          "address" -> address.lines(countryOptions)
        )

        templateCaptor.getValue mustEqual "individual/isThisYou.njk"
        jsonCaptor.getValue must containJson(expectedJson)

        application.stop()
      }
    }

/*    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(UserAnswers()))
        .overrides(
        )
        .build()
      val request = FakeRequest(GET, areYouUKResidentRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form"   -> form,
        "submitUrl" -> areYouUKResidentSubmitRoute,
        "radios" -> Radios.yesNo(form("value"))
      )

      templateCaptor.getValue mustEqual "individual/areYouUKResident.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(answers)).overrides().build()
      val request = FakeRequest(GET, areYouUKResidentRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val filledForm = form.bind(Map("value" -> "true"))

      val expectedJson = Json.obj(
        "form"   -> filledForm,
        "submitUrl" -> areYouUKResidentSubmitRoute,
        "radios" -> Radios.yesNo(filledForm("value"))
      )

      templateCaptor.getValue mustEqual "individual/areYouUKResident.njk"
      jsonCaptor.getValue must containJson(expectedJson)

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

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
        )
        .build()
      val request = FakeRequest(POST, areYouUKResidentRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form"   -> boundForm,
        "submitUrl" -> areYouUKResidentSubmitRoute,
        "radios" -> Radios.yesNo(boundForm("value"))
      )

      templateCaptor.getValue mustEqual "individual/areYouUKResident.njk"
      jsonCaptor.getValue must containJson(expectedJson)

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
    }*/
  }
}
