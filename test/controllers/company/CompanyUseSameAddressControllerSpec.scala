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

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.address.UseAddressForContactFormProvider
import matchers.JsonMatchers
import models.{TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatestplus.mockito.MockitoSugar
import pages.company.{BusinessNamePage, CompanyUseSameAddressPage, ConfirmAddressPage}
import pages.register.AreYouUKCompanyPage
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.html.components
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.countryOptions.CountryOptions
import views.html.address.UseAddressForContactView

import scala.concurrent.Future

class CompanyUseSameAddressControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyName: String = "Company name"
  private val countryOptions: CountryOptions = mock[CountryOptions]
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))).build()
  private val form = new UseAddressForContactFormProvider()(messages("useAddressForContact.error.required", messages("company")))

  private val address: TolerantAddress = TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("UK"))

  val userAnswers: UserAnswers = UserAnswers()
    .setOrException(BusinessNamePage, companyName)
    .setOrException(ConfirmAddressPage, address)
    .setOrException(AreYouUKCompanyPage, true)

  private def onPageLoadUrl: String = routes.CompanyUseSameAddressController.onPageLoad().url
  private def submitCall: Call = routes.CompanyUseSameAddressController.onSubmit()
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

  "CompanyUseSameAddress Controller" must {
    "return OK and the correct view for a GET" in {
      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[UseAddressForContactView].apply(
        submitCall,
        form,
        Seq(
          components.RadioItem(content = Text(Messages("site.yes")), value = Some("true"), id = Some("value")),
          components.RadioItem(content = Text(Messages("site.no")), value = Some("false"), id = Some("value-no"))
        ),
        "company",
        companyName,
        Seq("addr1", "addr2", "addr3", "addr4", "postcode", "United Kingdom")
      )(request, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val prepopUA: UserAnswers = userAnswers.set(CompanyUseSameAddressPage, true).toOption.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(prepopUA))

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val filledForm = form.fill(true)

      val view = app.injector.instanceOf[UseAddressForContactView].apply(
        submitCall,
        filledForm,
        Seq(
          components.RadioItem(content = Text(Messages("site.yes")), value = Some("true"), id = Some("value")),
          components.RadioItem(content = Text(Messages("site.no")), value = Some("false"), id = Some("value-no"))
        ),
        "company",
        companyName,
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
        BusinessNamePage.toString -> companyName,
        ConfirmAddressPage.toString -> address,
        CompanyUseSameAddressPage.toString -> true)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(CompanyUseSameAddressPage), any(), any())).thenReturn(dummyCall)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(dummyCall.url)

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
