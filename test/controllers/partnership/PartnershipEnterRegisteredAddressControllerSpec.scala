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

package controllers.partnership

import connectors.RegistrationConnector
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import forms.address.AddressFormProvider
import matchers.JsonMatchers
import models.register.{RegistrationCustomerType, RegistrationInfo, RegistrationLegalStatus}
import models.{Address, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.company.BusinessNamePage
import pages.partnership.PartnershipRegisteredAddressPage
import pages.register.AreYouUKCompanyPage
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.{JsArray, JsBoolean, JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.InputOption
import utils.countryOptions.CountryOptions

import scala.concurrent.Future

class PartnershipEnterRegisteredAddressControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
                                with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val companyName: String = "Company name"

  val countryOptions: CountryOptions = mock[CountryOptions]

  private val mockRegistrationConnector = mock[RegistrationConnector]

  private val application: Application =
    applicationBuilderMutableRetrievalAction(
      mutableFakeDataRetrievalAction,
      extraModules = Seq(
        bind[CountryOptions].toInstance(countryOptions),
        bind[RegistrationConnector].to(mockRegistrationConnector)
      )
    ).build()
  private val templateToBeRendered = "address/manualAddress.njk"
  private val form = new AddressFormProvider(countryOptions)()

  val userAnswers: UserAnswers = UserAnswers().set(BusinessNamePage, companyName).toOption.value
    .setOrException(AreYouUKCompanyPage, true)

  private def onPageLoadUrl: String = routes.PartnershipEnterRegisteredAddressController.onPageLoad(NormalMode).url
  private def submitUrl: String = routes.PartnershipEnterRegisteredAddressController.onSubmit(NormalMode).url
  private val dummyCall: Call = Call("GET", "/foo")
  private val address: Address = Address("line1", "line2", Some("line3"), Some("line4"), None, "FR")

  private val valuesValid: Map[String, Seq[String]] = Map(
    "line1" -> Seq("line1"),
    "line2" -> Seq("line2"),
    "line3" -> Seq("line3"),
    "line4" -> Seq("line4"),
    "country" -> Seq("FR"),
    "postcode" -> Seq("")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map("value" -> Seq(""))

  private val jsonToPassToTemplate: Form[Address] => JsObject =
    form => Json.obj(
      "submitUrl" -> submitUrl,
      "form" -> form,
      "pageTitle" -> messages("address.title", messages("partnership")),
      "h1" -> messages("address.title", companyName)
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(countryOptions.options).thenReturn(Seq(InputOption("FR", "France")))
    when(mockAppConfig.validCountryCodes).thenReturn(Seq("FR"))
  }

  "Partnership Enter Registered Address Controller" must {
    "return OK and the correct view for a GET with countries but no postcode" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate.apply(form))
      (jsonCaptor.getValue \ "countries").asOpt[JsArray].isDefined mustBe true
      (jsonCaptor.getValue \ "postcodeEntry").asOpt[JsBoolean].isDefined mustBe false
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "Save data to user answers, redirect to next page when valid data is submitted and call register without id" in {
      val expectedJson = Json.obj(
          BusinessNamePage.toString -> companyName,
          PartnershipRegisteredAddressPage.toString -> address)

      val regInfo = RegistrationInfo(
        legalStatus = RegistrationLegalStatus.Partnership,
        sapNumber = "abc",
        noIdentifier = false,
        customerType = RegistrationCustomerType.NonUK,
        idType = None,
        idNumber = Some("pspId")
      )

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(PartnershipRegisteredAddressPage), any(), any())).thenReturn(dummyCall)
      when(mockRegistrationConnector.registerWithNoIdOrganisation(any(),any(),any())(any(),any()))
        .thenReturn(Future.successful(regInfo))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(submitUrl, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1))
        .save(jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
     redirectLocation(result) mustBe Some(dummyCall.url)
      verify(mockRegistrationConnector, times(1))
        .registerWithNoIdOrganisation(
          ArgumentMatchers.eq(companyName),
          ArgumentMatchers.eq(address),
          ArgumentMatchers.eq(RegistrationLegalStatus.Partnership))(any(),any())

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
