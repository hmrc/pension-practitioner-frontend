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
import forms.address.UseAddressForContactFormProvider
import matchers.JsonMatchers
import models.{TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.{BusinessNamePage, ConfirmAddressPage, PartnershipUseSameAddressPage}
import pages.register.AreYouUKCompanyPage
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import viewmodels.Radios
import utils.countryOptions.CountryOptions
import views.html.address.UseAddressForContactView

import scala.concurrent.Future

class PartnershipUseSameAddressControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val partnershipName: String = "Partnership name"
  private val countryOptions: CountryOptions = mock[CountryOptions]
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).build()
  private val form = new UseAddressForContactFormProvider()(messages("useAddressForContact.error.required", messages("partnership")))

  private val address: TolerantAddress = TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("UK"))

  val userAnswers: UserAnswers = UserAnswers()
    .setOrException(BusinessNamePage, partnershipName)
    .setOrException(ConfirmAddressPage, address)
    .setOrException(AreYouUKCompanyPage, true)

  private def onPageLoadUrl: String = routes.PartnershipUseSameAddressController.onPageLoad().url
  private def submitCall: Call = routes.PartnershipUseSameAddressController.onSubmit()
  private def submitUrl: String = submitCall.url

  private val dummyCall: Call = Call("GET", "/foo")

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("true"))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))


  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(countryOptions.getCountryNameFromCode(eqTo(address))).thenReturn(Some("United Kingdom"))
   }

  val request = FakeRequest(GET, onPageLoadUrl)

  "PartnershipUseSameAddress Controller" must {
    "return OK and the correct view for a GET" in {
      val view = app.injector.instanceOf[UseAddressForContactView].apply(
        submitCall,
        form,
        Radios.yesNo(form("value")),
        "partnership",
        partnershipName,
        Seq("addr1", "addr2", "addr3", "addr4", "postcode", "United Kingdom")
      )(request, messages)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK
      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val prepopUA: UserAnswers = userAnswers.set(PartnershipUseSameAddressPage, true).toOption.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(prepopUA))

      val result = route(app, httpGETRequest(onPageLoadUrl)).value
      status(result) mustEqual OK
      val filledForm = form.fill(true)

      val view = app.injector.instanceOf[UseAddressForContactView].apply(
        submitCall,
        filledForm,
        Radios.yesNo(form("value")),
        "partnership",
        partnershipName,
        Seq("addr1", "addr2", "addr3", "addr4", "postcode", "United Kingdom")
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
        ConfirmAddressPage.toString -> address,
        PartnershipUseSameAddressPage.toString -> true)

      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(expectedJson)
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(dummyCall)

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
