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

package controllers.deregister.company

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{MinimalConnector, DeregistrationConnector}
import controllers.actions.FakeAuthActionNoEnrolment
import controllers.actions.{MutableFakeDataRetrievalAction, AuthAction, FakeAuthAction}
import controllers.base.ControllerSpecBase
import forms.deregister.ConfirmDeregistrationFormProvider
import matchers.JsonMatchers
import models.{UserAnswers, MinimalPSP}
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, when, verify}
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.PspNamePage
import pages.deregister.ConfirmDeregistrationCompanyPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.annotations.AuthWithIVEnrolmentRequired
import utils.annotations.AuthWithIVNoEnrolment

import scala.concurrent.Future

class ConfirmDeregistrationControllerSpec extends ControllerSpecBase with MockitoSugar with NunjucksSupport with JsonMatchers with OptionValues with TryValues {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ConfirmDeregistrationFormProvider()
  private val form = formProvider()
  private val pspName = "test-psp"
  private val mockMinimalConnector = mock[MinimalConnector]
  private val mockDeregistrationConnector = mock[DeregistrationConnector]
  private val minPsp = MinimalPSP("a@a.a", Some(pspName), None)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val userAnswers: UserAnswers = UserAnswers().set(PspNamePage, pspName).toOption.value
  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private def getRoute: String = routes.ConfirmDeregistrationController.onPageLoad().url
  private def postRoute: String = routes.ConfirmDeregistrationController.onSubmit().url

  override def modules: Seq[GuiceableModule] = Seq(
    bind[MinimalConnector].toInstance(mockMinimalConnector),
    bind[DeregistrationConnector].toInstance(mockDeregistrationConnector),
    bind[AuthAction].qualifiedWith(classOf[AuthWithIVEnrolmentRequired]).to[FakeAuthAction],
    bind[NunjucksRenderer].toInstance(mockRenderer),
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator)
  )

  override def beforeEach: Unit = {
    super.beforeEach
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any())).thenReturn(Future.successful(minPsp))
    when(mockDeregistrationConnector.canDeRegister(any())(any(), any())).thenReturn(Future.successful(true))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
  }
  private val answers: UserAnswers = UserAnswers().set(ConfirmDeregistrationCompanyPage, value = true).success.value

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
