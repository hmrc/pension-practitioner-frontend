/*
 * Copyright 2020 HM Revenue & Customs
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
import forms.individual.IndividualNameFormProvider
import matchers.JsonMatchers
import models.UserAnswers
import models.register.TolerantIndividual
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.IndividualDetailsPage
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class IndividualNameControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val templateToBeRendered = "individual/name.njk"
  private val form = new IndividualNameFormProvider()()
  private val firstName = "testFirstName"
  private val lastName = "testLastName"
  private val name = TolerantIndividual(Some(firstName), None, Some(lastName))
  private val dummyCall: Call = Call("GET", "/foo")

  private def onPageLoadUrl: String = routes.IndividualNameController.onPageLoad().url

  private def submitUrl: String = routes.IndividualNameController.onSubmit().url

  private val valuesValid: Map[String, Seq[String]] = Map("firstName" -> Seq(firstName), "lastName" -> Seq(lastName))

  private val valuesInvalid: Map[String, Seq[String]] = Map("firstName" -> Seq(""), "lastName" -> Seq(""))

  private def jsonToPassToTemplate(form: Form[TolerantIndividual]): JsObject =
    Json.obj("form" -> form, "submitUrl" -> submitUrl)

  override def beforeEach: Unit = {
    super.beforeEach
    mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "IndividualName Controller" must {
    "return OK and the correct view for a GET" in {
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate(form))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj(IndividualDetailsPage.toString -> name)

      when(mockCompoundNavigator.nextPage(Matchers.eq(IndividualDetailsPage), any(), any())).thenReturn(dummyCall)

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
