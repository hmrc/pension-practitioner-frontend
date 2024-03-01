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
import forms.address.AddressListFormProvider
import matchers.JsonMatchers
import models.{NormalMode, TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.partnership.{BusinessNamePage, PartnershipAddressListPage, PartnershipAddressPage, PartnershipPostcodePage}
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

class PartnershipAddressListControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val partnershipName: String = "Partnership name"
  val countryOptions: CountryOptions = mock[CountryOptions]
  private val application: Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))
    ).build()
  private val templateToBeRendered = "address/addressList.njk"
  private val form = new AddressListFormProvider()(messages("addressList.error.invalid", messages("partnership")))
  private val tolerantAddress = TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("GB"))

  val userAnswers: UserAnswers = UserAnswers().set(BusinessNamePage, partnershipName).toOption.value
    .set(PartnershipPostcodePage, Seq(tolerantAddress)).toOption.value

  private def onPageLoadUrl: String = routes.PartnershipAddressListController.onPageLoad(NormalMode).url

  private def enterManuallyUrl: Call = routes.PartnershipContactAddressController.onPageLoad(NormalMode)

  private def submitUrl: String = routes.PartnershipAddressListController.onSubmit(NormalMode).url

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("0"))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val jsonToPassToTemplate: Form[Int] => JsObject =
    form => Json.obj(
      "form" -> form,
      "addresses" -> Json.arr(Json.obj("value" -> 0, "text" -> "addr1, addr2, addr3, addr4, postcode, United Kingdom")),
      "viewmodel" -> CommonViewModel("partnership", partnershipName, submitUrl, Some(enterManuallyUrl.url)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(countryOptions.getCountryNameFromCode(eqTo(tolerantAddress))).thenReturn(Some("United Kingdom"))
  }

  "PartnershipAddressList Controller" must {
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
        BusinessNamePage.toString -> partnershipName,
        PartnershipPostcodePage.toString -> Seq(tolerantAddress),
        PartnershipAddressPage.toString -> tolerantAddress.copy(countryOpt = Some("GB")).toAddress)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(PartnershipAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
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
