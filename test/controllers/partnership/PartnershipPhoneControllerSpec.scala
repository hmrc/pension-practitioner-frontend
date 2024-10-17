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
import forms.PhoneFormProvider
import matchers.JsonMatchers
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.{BusinessNamePage, PartnershipPhonePage}
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import viewmodels.CommonViewModelTwirl
import views.html.PhoneView

import scala.concurrent.Future

class PartnershipPhoneControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val PartnershipName: String = "Partnership name"
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val form = new PhoneFormProvider()(messages("phone.error.required", messages("Partnership")))
  private val phone = "11111111"
  private val dummyCall: Call = Call("GET", "/foo")

  val userAnswers: UserAnswers = UserAnswers().set(BusinessNamePage, PartnershipName).toOption.value

  private def onPageLoadUrl: String = routes.PartnershipPhoneController.onPageLoad(NormalMode).url
  private def submitCall: Call = routes.PartnershipPhoneController.onSubmit(NormalMode)
  private def submitUrl: String = submitCall.url

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq(phone))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val request = FakeRequest(GET, onPageLoadUrl)

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
   }

  "PartnershipPhone Controller" must {
    "return OK and the correct view for a GET" in {
      val view = app.injector.instanceOf[PhoneView].apply(
        CommonViewModelTwirl("partnership", PartnershipName, submitCall),
        form
      )(request, messages)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value
      status(result) mustEqual OK

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val prepopUA: UserAnswers = userAnswers.set(PartnershipPhonePage, phone).toOption.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(prepopUA))

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val filledForm = form.bind(Map("value" -> phone))
      val view = app.injector.instanceOf[PhoneView].apply(
        CommonViewModelTwirl("partnership", PartnershipName, submitCall),
        filledForm
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
        BusinessNamePage.toString -> PartnershipName,
        PartnershipPhonePage.toString -> phone)
      when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(expectedJson)
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(PartnershipPhonePage), any(), any())).thenReturn(dummyCall)

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
