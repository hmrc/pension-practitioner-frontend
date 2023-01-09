/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.RegistrationConnector
import controllers.base.ControllerSpecBase
import forms.individual.IsThisYouFormProvider
import matchers.JsonMatchers
import models.register._
import models.{Address, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.RegistrationInfoPage
import pages.individual.{IndividualAddressPage, IndividualDetailsPage, IsThisYouPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.countryOptions.CountryOptions

import scala.concurrent.Future

class IsThisYouControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport with JsonMatchers with OptionValues with TryValues {

  import IsThisYouControllerSpec._

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new IsThisYouFormProvider()
  private val form = formProvider()

  private def isThisYouGetRoute: String = routes.IsThisYouController.onPageLoad(NormalMode).url

  private def isThisYouPostRoute: String = routes.IsThisYouController.onSubmit(NormalMode).url

  private val countryOptions: CountryOptions = mock[CountryOptions]
  private val registrationConnector = mock[RegistrationConnector]
  private val templateCaptor = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

  private def expectedJson(form: Form[Boolean]): JsObject = Json.obj(
    "form" -> form,
    "submitUrl" -> isThisYouPostRoute,
    "radios" -> Radios.yesNo(form("value")),
    "name" -> individual.fullName,
    "address" -> address.lines(countryOptions)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(countryOptions.getCountryNameFromCode(eqTo(address))).thenReturn("United Kingdom")
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "IsThisYouController" when {

    "on a GET" must {

      "return OK with the correct view if individual details are in the cache" in {
        when(registrationConnector.registerWithIdIndividual(any())(any(), any())).
          thenReturn(Future.successful(IndividualRegistration(IndividualRegisterWithIdResponse(individual, address.toTolerantAddress), registrationInfo)))
        when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
        val application = applicationBuilder(userAnswers = Some(uaWithIndividualAddressRegInfo),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions),
            bind[RegistrationConnector].toInstance(registrationConnector))).overrides().build()
        val request = FakeRequest(GET, isThisYouGetRoute)
        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

        templateCaptor.getValue mustEqual "individual/isThisYou.njk"
        jsonCaptor.getValue must containJson(expectedJson(form))

        application.stop()
      }

      "return OK and populate the view correctly when the question has previously been answered" in {
        when(registrationConnector.registerWithIdIndividual(any())(any(), any())).
          thenReturn(Future.successful(IndividualRegistration(IndividualRegisterWithIdResponse(individual, address.toTolerantAddress), registrationInfo)))
        when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
        val application = applicationBuilder(userAnswers = Some(uaWithIndividualAddressRegInfo.set(IsThisYouPage, true).success.value),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions),
            bind[RegistrationConnector].toInstance(registrationConnector))).overrides().build()
        val request = FakeRequest(GET, isThisYouGetRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

        val filledForm = form.bind(Map("value" -> "true"))

        templateCaptor.getValue mustEqual "individual/isThisYou.njk"
        jsonCaptor.getValue must containJson(expectedJson(filledForm))

        application.stop()
      }

      "return OK with the correct view, call register with id and save the individual details in the cache if we have a valid nino" in {
        when(registrationConnector.registerWithIdIndividual(any())(any(), any())).
          thenReturn(Future.successful(IndividualRegistration(IndividualRegisterWithIdResponse(individual, address.toTolerantAddress), registrationInfo)))
        when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))

        val application = applicationBuilder(userAnswers = Some(UserAnswers()),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions),
            bind[RegistrationConnector].toInstance(registrationConnector))).overrides().build()
        val request = FakeRequest(GET, isThisYouGetRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
        verify(mockUserAnswersCacheConnector, times(1)).save(eqTo(uaWithIndividualAddressRegInfo.data))(any(), any())

        templateCaptor.getValue mustEqual "individual/isThisYou.njk"
        jsonCaptor.getValue must containJson(expectedJson(form))

        application.stop()
      }

      "redirect to Session Expired if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        val request = FakeRequest(GET, isThisYouGetRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

        application.stop()
      }
    }

    "on a POST" must {

      "redirect to the next page when valid data is submitted" in {
        when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
        when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)

        val application = applicationBuilder(userAnswers = Some(UserAnswers()),
          extraModules = Seq(bind[CountryOptions].toInstance(countryOptions),
            bind[RegistrationConnector].toInstance(registrationConnector))).overrides().build()

        val request = FakeRequest(POST, isThisYouPostRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual onwardRoute.url

        application.stop()
      }
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(uaWithIndividualAddressRegInfo),
        extraModules = Seq(bind[CountryOptions].toInstance(countryOptions),
          bind[RegistrationConnector].toInstance(registrationConnector))).overrides().build()

      val request = FakeRequest(POST, isThisYouPostRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))
      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "individual/isThisYou.njk"
      jsonCaptor.getValue must containJson(expectedJson(boundForm))

      application.stop()
    }

    "redirect to Session Expired if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, isThisYouPostRoute)
          .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}

object IsThisYouControllerSpec extends TryValues {
  private val individual = TolerantIndividual(Some("first"), None, Some("last"))
  private val address = Address("line1", "line2", Some("line3"), Some("line4"), Some("post code"), "GB")
  private val registrationInfo = RegistrationInfo(
    RegistrationLegalStatus.Individual,
    "test-sap",
    noIdentifier = false,
    RegistrationCustomerType.NonUK,
    None,
    None
  )
  private val uaWithIndividualAddressRegInfo = UserAnswers().set(IndividualDetailsPage, individual).flatMap(
    _.set(IndividualAddressPage, address)).flatMap(_.set(RegistrationInfoPage, registrationInfo)).success.value

}
