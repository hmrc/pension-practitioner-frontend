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

import connectors.AddressLookupConnector
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.address.PostcodeFormProvider
import matchers.JsonMatchers
import models.{NormalMode, TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatestplus.mockito.MockitoSugar
import pages.company.{BusinessNamePage, CompanyPostcodePage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.address.PostcodeView

import scala.concurrent.Future

class CompanyPostcodeControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers {

  private val mockAddressLookupConnector = mock[AddressLookupConnector]
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyName: String = "Company name"
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[AddressLookupConnector].toInstance(mockAddressLookupConnector))
    ).build()
  private val form = new PostcodeFormProvider()(
    messages("postcode.error.required", messages("company")),
    messages("postcode.error.invalid", messages("company")))
  private val postcode = "ZZ1 1ZZ"
  private val seqAddresses: Seq[TolerantAddress] =
    Seq(TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("UK")))

  val userAnswers: UserAnswers = UserAnswers().set(BusinessNamePage, companyName).toOption.value

  private def onPageLoadUrl: String = routes.CompanyPostcodeController.onPageLoad(NormalMode).url
  private def enterManuallyCall: Call = routes.CompanyContactAddressController.onPageLoad(NormalMode)
  private def submitCall: Call = routes.CompanyPostcodeController.onSubmit(NormalMode)
  private def submitUrl: String = submitCall.url

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq(postcode))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
   }

  "CompanyPostcode Controller" must {
    "return OK and the correct view for a GET" in {
      when(mockAppConfig.betaFeedbackUnauthenticatedUrl).thenReturn("betaFeedbackUnauthenticatedUrl")
      val request = FakeRequest(GET, onPageLoadUrl)
      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[PostcodeView].apply(
        submitCall,
        enterManuallyCall.url,
        "company",
        companyName,
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
          BusinessNamePage.toString -> companyName,
          CompanyPostcodePage.toString -> seqAddresses)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(CompanyPostcodePage), any(), any())).thenReturn(enterManuallyCall)
      when(mockAddressLookupConnector.addressLookupByPostCode(any())(any(), any())).thenReturn(Future.successful(seqAddresses))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(enterManuallyCall.url)

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
