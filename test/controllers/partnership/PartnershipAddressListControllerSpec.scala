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
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModelTwirl
import views.html.address.AddressListView

import scala.concurrent.Future

class PartnershipAddressListControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val partnershipName: String = "Partnership name"
  val countryOptions: CountryOptions = mock[CountryOptions]
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))
    ).build()

  private val form = new AddressListFormProvider()(messages("addressList.error.invalid", messages("partnership")))

  private val tolerantAddress = TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("GB"))

  val userAnswers: UserAnswers = UserAnswers().set(BusinessNamePage, partnershipName).toOption.value
    .set(PartnershipPostcodePage, Seq(tolerantAddress)).toOption.value

  private def onPageLoadUrl: String = routes.PartnershipAddressListController.onPageLoad(NormalMode).url

  private def enterManuallyUrl: Call = routes.PartnershipContactAddressController.onPageLoad(NormalMode)

  private def submitUrl: String = routes.PartnershipAddressListController.onSubmit(NormalMode).url

  private val valuesValid: Map[String, Seq[String]] = Map("value" -> Seq("0"))

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val address = Seq(
    RadioItem(Text("addr1, addr2, addr3, addr4, postcode, United Kingdom"), value = Some("0")))

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
     when(countryOptions.getCountryNameFromCode(eqTo(tolerantAddress))).thenReturn(Some("United Kingdom"))
  }

  "PartnershipAddressList Controller" must {
    "return OK and the correct view for a GET" in {
      val request = httpGETRequest(onPageLoadUrl)

      val view = app.injector.instanceOf[AddressListView].apply(
        form,
        address,
        CommonViewModelTwirl(
          "partnership",
          partnershipName,
          routes.PartnershipAddressListController.onSubmit(NormalMode),
          Some(enterManuallyUrl.url)
        ))(request, messages)
      val result = route(app, request).value

      status(result) mustEqual OK
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
        PartnershipPostcodePage.toString -> Seq(tolerantAddress),
        PartnershipAddressPage.toString -> tolerantAddress.copy(countryOpt = Some("GB")).toAddress)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(PartnershipAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(app, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
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
