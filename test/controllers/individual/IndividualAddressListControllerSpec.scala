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

package controllers.individual

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.address.AddressListFormProvider
import matchers.JsonMatchers
import models.register.TolerantIndividual
import models.{NormalMode, TolerantAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.{IndividualAddressListPage, IndividualDetailsPage, IndividualManualAddressPage, IndividualPostcodePage}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.countryOptions.CountryOptions
import viewmodels.CommonViewModelTwirl
import views.html.individual.AddressListView

import scala.concurrent.Future

class IndividualAddressListControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val tolerantIndividualName: TolerantIndividual = TolerantIndividual(Some("individual"),None,Some("name"))

  private val countryOptions: CountryOptions = mock[CountryOptions]
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(bind[CountryOptions].toInstance(countryOptions))
    ).build()
  private val form: Form[Int] = new AddressListFormProvider()(messages("individual.addressList.error.required"))
  private val tolerantAddress = TolerantAddress(Some("addr1"), Some("addr2"), Some("addr3"), Some("addr4"), Some("postcode"), Some("GB"))

  private val userAnswers: UserAnswers = UserAnswers().set(IndividualDetailsPage, tolerantIndividualName).toOption.value
    .set(IndividualPostcodePage, Seq(tolerantAddress)).toOption.value

  private def onPageLoadUrl: String = routes.IndividualAddressListController.onPageLoad(NormalMode).url

  private def enterManuallyUrl: Call = routes.IndividualContactAddressController.onPageLoad(NormalMode)

  private def submitUrl: String = routes.IndividualAddressListController.onSubmit(NormalMode).url

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

  "IndividualAddressList Controller" must {
    "return OK and the correct view for a GET" in {

      val request = httpGETRequest(onPageLoadUrl)

      val view = app.injector.instanceOf[AddressListView].apply(
        CommonViewModelTwirl(
          "individual",
          tolerantIndividualName.fullName,
          routes.IndividualAddressListController.onSubmit(NormalMode),
          Some(enterManuallyUrl.url)
        ), form, address)(request, messages)
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
        IndividualDetailsPage.toString -> tolerantIndividualName,
        IndividualPostcodePage.toString -> Seq(tolerantAddress),
        IndividualManualAddressPage.toString -> tolerantAddress.toAddress)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(IndividualAddressListPage), any(), any())).thenReturn(enterManuallyUrl)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
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
