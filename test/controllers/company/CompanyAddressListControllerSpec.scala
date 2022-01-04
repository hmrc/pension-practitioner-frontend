/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{NormalMode, TolerantAddress, UserAnswers, Address}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.company.{BusinessNamePage, CompanyAddressListPage, CompanyAddressPage, CompanyPostcodePage}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModel

import scala.concurrent.Future

class CompanyAddressListControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
                                with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyName: String = "Company name"
  val countryOptions: CountryOptions = mock[CountryOptions]
  private val application: Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))
    ).build()
  private val templateToBeRendered = "address/addressList.njk"
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
  private def submitUrl: String = routes.CompanyAddressListController.onSubmit(NormalMode).url

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("0"))
  private val valuesValid1: Map[String, Seq[String]] = Map("value" -> Seq("1"))
  private val valuesValid2: Map[String, Seq[String]] = Map("value" -> Seq("2"))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val jsonToPassToTemplate: Form[Int] => JsObject =
    form => Json.obj(
        "form" -> form,
      "addresses" -> Json.arr(Json.obj("value" -> 0,"text" ->"addr1, addr2, addr3, addr4, postcode, United Kingdom"),
        Json.obj("value" -> 1, "text" -> "Address 2 Line 1, Address 2 Line 4, 123, United Kingdom"),
        Json.obj("value" -> 2,"text" -> "Address 1 Line 1, A1 1PC, United Kingdom")),
      "viewmodel" -> CommonViewModel("company", companyName, submitUrl, Some(enterManuallyUrl.url)))

  override def beforeEach: Unit = {
    super.beforeEach
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(countryOptions.getCountryNameFromCode(eqTo(tolerantAddress))).thenReturn(Some("United Kingdom"))
    when(countryOptions.getCountryNameFromCode(eqTo(incompleteAddresses))).thenReturn(Some("United Kingdom"))
    when(countryOptions.getCountryNameFromCode(eqTo(incompleteFixableAddresses))).thenReturn(Some("United Kingdom"))
  }

  "CompanyAddressList Controller" must {
    "return OK and the correct view for a GET" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate.apply(form))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

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
      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

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
      val result = route(application, httpPOSTRequest(submitUrl, valuesValid1)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(enterManuallyUrl.url)

    }

    "Save data to user answers and redirect to next page when valid data is submitted when address is incomplete and NonFixable" in {

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(CompanyAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val result = route(application, httpPOSTRequest(submitUrl, valuesValid2)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(enterManuallyUrl.url)

    }

    "return a BAD REQUEST when invalid data is submitted" in {

      val result = route(application, httpPOSTRequest(submitUrl, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}
