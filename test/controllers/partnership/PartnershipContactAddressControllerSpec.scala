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

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.address.AddressFormProvider
import matchers.JsonMatchers
import models.{Address, Country, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.{BusinessNamePage, PartnershipAddressPage}
import pages.register.AreYouUKCompanyPage
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.InputOption
import utils.countryOptions.CountryOptions
import views.html.address.ManualAddressView

import scala.concurrent.Future

class PartnershipContactAddressControllerSpec
  extends ControllerSpecBase
    with MockitoSugar
                                with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val partnershipName: String = "Partnership name"

  val countryOptions: CountryOptions = mock[CountryOptions]

  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))
    ).build()
  private val form = new AddressFormProvider(countryOptions)()

  val userAnswers: UserAnswers = UserAnswers()
    .set(BusinessNamePage, partnershipName).toOption.value
    .setOrException(AreYouUKCompanyPage, true)

  private def onPageLoadUrl: String = routes.PartnershipContactAddressController.onPageLoad(NormalMode).url
  private def submitCall: Call = routes.PartnershipContactAddressController.onSubmit(NormalMode)
  private def submitUrl: String = submitCall.url
  private val dummyCall: Call = Call("GET", "/foo")
  private val address: Address = Address("line1", "line2", Some("line3"), Some("line4"), Some("ZZ1 1ZZ"), "GB")

  private val valuesValid: Map[String, Seq[String]] = Map(
    "line1" -> Seq("line1"),
    "line2" -> Seq("line2"),
    "line3" -> Seq("line3"),
    "line4" -> Seq("line4"),
    "country" -> Seq("GB"),
    "postcode" -> Seq("ZZ1 1ZZ")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
     when(countryOptions.options).thenReturn(Seq(InputOption("GB", "United Kingdom")))
    when(mockAppConfig.validCountryCodes).thenReturn(Seq("GB"))
  }

  "PartnershipAddress Controller" must {
    "return OK and the correct view for a GET with countries and postcode" in {
      val request = FakeRequest(GET, onPageLoadUrl)
      val result = route(app, httpGETRequest(onPageLoadUrl)).value
      status(result) mustEqual OK

      val view = app.injector.instanceOf[ManualAddressView].apply(
        messages("address.title", messages("partnership")),
        messages("address.title", partnershipName),
        postcodeEntry = true,
        postcodeFirst = true,
        Array(Country("", ""), Country("GB", "United Kingdom")),
        submitCall,
        form
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj(
          BusinessNamePage.toString -> partnershipName,
          PartnershipAddressPage.toString -> address)

      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(expectedJson)
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(PartnershipAddressPage), any(), any())).thenReturn(dummyCall)

      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual dummyCall.url
    }

    "return a BAD REQUEST when invalid data is submitted" in {

      val result = route(app, httpPOSTRequest(submitUrl, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
