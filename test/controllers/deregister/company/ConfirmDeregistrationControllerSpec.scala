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

package controllers.deregister.company

import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{DeregistrationConnector, MinimalConnector}
import controllers.actions.{AuthAction, FakeAuthAction, MutableFakeDataRetrievalAction}
import controllers.base.ControllerSpecBase
import forms.deregister.ConfirmDeregistrationFormProvider
import matchers.JsonMatchers
import models.{MinimalPSP, UserAnswers}
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.PspNamePage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.TestMetrics
import utils.annotations.AuthMustHaveEnrolment

import scala.concurrent.Future

class ConfirmDeregistrationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ConfirmDeregistrationFormProvider()
  private val form = formProvider()
  private val pspName = "test-psp"
  private val mockMinimalConnector = mock[MinimalConnector]
  private val mockDeregistrationConnector = mock[DeregistrationConnector]
  private val minPsp = MinimalPSP("a@a.a", Some(pspName), None, rlsFlag = false, deceasedFlag = false)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val userAnswers: UserAnswers = UserAnswers().set(PspNamePage, pspName).toOption.value
  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private def getRoute: String = routes.ConfirmDeregistrationController.onPageLoad().url
  private def postRoute: String = routes.ConfirmDeregistrationController.onSubmit().url

  override def modules: Seq[GuiceableModule] = Seq(
    bind[Metrics].toInstance(new TestMetrics),
    bind[MinimalConnector].toInstance(mockMinimalConnector),
    bind[DeregistrationConnector].toInstance(mockDeregistrationConnector),
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveEnrolment]).to[FakeAuthAction],
    bind[NunjucksRenderer].toInstance(mockRenderer),
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any())).thenReturn(Future.successful(minPsp))
    when(mockDeregistrationConnector.canDeRegister(any())(any(), any())).thenReturn(Future.successful(true))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
  }

  "ConfirmDeregistrationController" must {

    "return OK and the correct view for a GET" in {

      val request = FakeRequest(GET, getRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form"   -> form,
        "pspName" -> pspName,
        "submitUrl" -> postRoute,
        "radios" -> Radios.yesNo(form("value"))
      )

      templateCaptor.getValue mustEqual "deregister/company/confirmDeregistration.njk"
      jsonCaptor.getValue must containJson(expectedJson)

    }

    "redirect to the next page when valid data is submitted" in {
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
      val request = FakeRequest(POST, getRoute).withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockCompoundNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
      val request = FakeRequest(POST, getRoute).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form"   -> boundForm,
        "pspName" -> pspName,
        "submitUrl" -> postRoute,
        "radios" -> Radios.yesNo(boundForm("value"))
      )

      templateCaptor.getValue mustEqual "deregister/company/confirmDeregistration.njk"
      jsonCaptor.getValue must containJson(expectedJson)

    }
  }
}
