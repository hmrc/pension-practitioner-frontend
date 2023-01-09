/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.RegistrationConnector
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.address.NonUKAddressFormProvider
import matchers.JsonMatchers
import models.register._
import models.{Address, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.{IndividualAddressPage, IndividualDetailsPage}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.InputOption
import utils.countryOptions.CountryOptions

import scala.concurrent.Future

class IndividualEnterRegisteredAddressControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val countryOptions: CountryOptions = mock[CountryOptions]
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  val regInfo: RegistrationInfo = RegistrationInfo(
    RegistrationLegalStatus.LimitedCompany,
    "sapNumber",
    noIdentifier = false,
    RegistrationCustomerType.UK,
    Some(RegistrationIdType.UTR),
    Some("utr")
  )

  private val application: Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(
        bind[CountryOptions].toInstance(countryOptions),
        bind[RegistrationConnector].toInstance(mockRegistrationConnector)
      )
    ).build()
  private val templateToBeRendered = "individual/nonUKAddress.njk"
  private val form = new NonUKAddressFormProvider(countryOptions)()

  private def onPageLoadUrl: String = routes.IndividualEnterRegisteredAddressController.onPageLoad(NormalMode).url

  private def submitUrl: String = routes.IndividualEnterRegisteredAddressController.onSubmit(NormalMode).url

  private val dummyCall: Call = Call("GET", "/foo")
  private val address: Address = Address("line1", "line2", Some("line3"), Some("line4"), None, "IN")
  private val ua: UserAnswers = UserAnswers().setOrException(IndividualDetailsPage, TolerantIndividual(Some("first"), None, Some("last")))

  private val valuesValid: Map[String, Seq[String]] = Map(
    "line1" -> Seq("line1"),
    "line2" -> Seq("line2"),
    "line3" -> Seq("line3"),
    "line4" -> Seq("line4"),
    "country" -> Seq("IN")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val jsonToPassToTemplate: Form[Address] => JsObject =
    form => Json.obj(
      "form" -> form,
      "submitUrl" -> submitUrl
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockRegistrationConnector.registerWithNoIdIndividual(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(regInfo))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(countryOptions.options).thenReturn(Seq(InputOption("IN", "India")))
    when(mockAppConfig.validCountryCodes).thenReturn(Seq("IN"))
  }

  "IndividualNonUKAddress Controller" must {
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
        IndividualAddressPage.toString -> address)

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(IndividualAddressPage), any(), any())).thenReturn(dummyCall)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(dummyCall.url)

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
