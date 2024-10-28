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
import forms.address.AddressListFormProvider
import matchers.JsonMatchers
import models.{Address, NormalMode, TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatestplus.mockito.MockitoSugar
import pages.company.{BusinessNamePage, CompanyAddressListPage, CompanyAddressPage, CompanyPostcodePage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModelTwirl
import views.html.address.AddressListView

import scala.concurrent.Future

class CompanyAddressListControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyName: String = "Company name"
  val countryOptions: CountryOptions = mock[CountryOptions]
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))
    ).build()
  private val form = new AddressListFormProvider()(messages("addressList.error.invalid", messages("company")))
  private val tolerantAddress = TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("GB"))

  private val incompleteAddresses : TolerantAddress =
    TolerantAddress(
      Some("Address 1 Line 1"),
      None, None, None,
      Some("A1 1PC"),
      Some("GB")
    )

  private val incompleteFixableAddresses : TolerantAddress =
    TolerantAddress(
      Some("Address 2 Line 1"),
      None, None, Some("Address 2 Line 4"),
      Some("123"),
      Some("GB")
    )

  private val expectedFixableAddress : Address =
    Address(
      "Address 2 Line 1",
      "Address 2 Line 4",
      None,
      None,
      Some("123"),
      "GB"
    )

  var userAnswers: UserAnswers = UserAnswers().set(BusinessNamePage, companyName).toOption.value
                                  .set(CompanyPostcodePage, Seq(tolerantAddress, incompleteFixableAddresses, incompleteAddresses)).toOption.value

  private def onPageLoadUrl: String = routes.CompanyAddressListController.onPageLoad(NormalMode).url
  private def enterManuallyUrl: Call = routes.CompanyContactAddressController.onPageLoad(NormalMode)
  private def submitCall: Call = routes.CompanyAddressListController.onSubmit(NormalMode)
  private def submitUrl: String = submitCall.url

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("0"))
  private val valuesValid1: Map[String, Seq[String]] = Map("value" -> Seq("1"))
  private val valuesValid2: Map[String, Seq[String]] = Map("value" -> Seq("2"))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val radioItems = Seq(
    RadioItem(Text("addr1, addr2, addr3, addr4, postcode, United Kingdom"), value = Some("0")),
    RadioItem(Text("Address 2 Line 1, Address 2 Line 4, 123, United Kingdom"), value = Some("1")),
    RadioItem(Text("Address 1 Line 1, A1 1PC, United Kingdom"), value = Some("2"))
  )

  private val commonViewModelTwirl = CommonViewModelTwirl(
    "company", companyName, submitCall, Some(enterManuallyUrl.url))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
     when(countryOptions.getCountryNameFromCode(eqTo(tolerantAddress))).thenReturn(Some("United Kingdom"))
    when(countryOptions.getCountryNameFromCode(eqTo(incompleteAddresses))).thenReturn(Some("United Kingdom"))
    when(countryOptions.getCountryNameFromCode(eqTo(incompleteFixableAddresses))).thenReturn(Some("United Kingdom"))
  }

  private val request = FakeRequest(GET, onPageLoadUrl)

  "CompanyAddressList Controller" must {
    "return OK and the correct view for a GET" in {
      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[AddressListView].apply(form, radioItems, commonViewModelTwirl)(request, messages)

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
          CompanyPostcodePage.toString -> Seq(tolerantAddress, incompleteFixableAddresses, incompleteAddresses),
          CompanyAddressPage.toString -> tolerantAddress.copy(countryOpt = Some("GB")).toAddress)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(CompanyAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(enterManuallyUrl.url)

    }
    "Save data to user answers and redirect to next page when valid data is submitted when address is incomplete but fixable" in {

      val expectedJson = Json.obj(
        BusinessNamePage.toString -> companyName,
        CompanyPostcodePage.toString -> Seq(tolerantAddress, incompleteFixableAddresses, incompleteAddresses),
        CompanyAddressPage.toString -> expectedFixableAddress)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(CompanyAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid1)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(enterManuallyUrl.url)

    }

    "Save data to user answers and redirect to next page when valid data is submitted when address is incomplete and NonFixable" in {

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(CompanyAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val result = route(app, httpPOSTRequest(submitUrl, valuesValid2)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(enterManuallyUrl.url)

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
