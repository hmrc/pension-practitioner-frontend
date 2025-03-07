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

package controllers.deregister.individual

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import connectors.{DeregistrationConnector, MinimalConnector}
import controllers.actions.{AuthAction, FakeAuthAction, MutableFakeDataRetrievalAction}
import controllers.base.ControllerSpecBase
import forms.deregister.ConfirmDeregistrationFormProvider
import handlers.FrontendErrorHandler
import matchers.JsonMatchers
import models.{MinimalPSP, UserAnswers}
import navigators.CompoundNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.PspNamePage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.{Call, Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.annotations.AuthMustHaveEnrolmentWithNoIV

import scala.concurrent.Future

class ConfirmDeregistrationControllerSpec extends ControllerSpecBase with MockitoSugar with JsonMatchers with OptionValues with TryValues {

  val validFormData = Map("value" -> "true")
  private def getRoute: String = routes.ConfirmDeregistrationController.onPageLoad().url
  val request = FakeRequest(GET, getRoute).withFormUrlEncodedBody(validFormData.toSeq: _*)
  implicit val req: Request[_] = request

  private val formProvider = new ConfirmDeregistrationFormProvider()
  private val form = formProvider("individual")
  private val pspName = "test-psp"
  private val mockMinimalConnector = mock[MinimalConnector]
  private val mockDeregistrationConnector = mock[DeregistrationConnector]
  private val mockFrontendErrorHandler = mock[FrontendErrorHandler]
  private val minPsp = MinimalPSP("a@a.a", Some(pspName), None, rlsFlag = false, deceasedFlag = false)

  val userAnswers: UserAnswers = UserAnswers().set(PspNamePage, pspName).toOption.value

  private lazy val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def postRoute: Call = routes.ConfirmDeregistrationController.onSubmit()

  override def modules: Seq[GuiceableModule] = Seq(
    bind[MinimalConnector].toInstance(mockMinimalConnector),
    bind[DeregistrationConnector].toInstance(mockDeregistrationConnector),
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveEnrolmentWithNoIV]).to[FakeAuthAction],
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator),
    bind[FrontendErrorHandler].toInstance(mockFrontendErrorHandler)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockMinimalConnector.getMinimalPspDetails()(any(), any())).thenReturn(Future.successful(minPsp))
    when(mockDeregistrationConnector.canDeRegister(any(), any())).thenReturn(Future.successful(true))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
    when(mockFrontendErrorHandler.onClientError(any(), any(), any())).thenReturn(Future.successful(Results.BadRequest))
  }

  "ConfirmDeregistrationController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
      val request = FakeRequest(GET, getRoute)
      val result = route(app, request).value
      status(result) mustEqual OK
      val view = app.injector.instanceOf[views.html.deregister.individual.ConfirmDeregistrationView]
      val expectedView = view(
        postRoute,
        form,
        Seq(
          RadioItem(
            content = Text("Yes"),
            value = Some("true"),
            checked = form("value").value.contains("true"),
            id = Some("value")
          ),
          RadioItem(
            content = Text("No"),
            value = Some("false"),
            checked = form("value").value.contains("false"),
            id = Some("value-no")
          )
        ),
        mockAppConfig.returnToPspDashboardUrl
      )(request, messages)

      compareResultAndView(result, expectedView)
    }

    "return a Bad Request and errors when invalid data is submitted" in {

      val request = FakeRequest(POST, routes.ConfirmDeregistrationController.onSubmit().url).withFormUrlEncodedBody(("value", ""))
      val boundForm = form.bind(Map("value" -> ""))

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST

      val view = app.injector.instanceOf[views.html.deregister.individual.ConfirmDeregistrationView]

      val expectedView = view(
        routes.ConfirmDeregistrationController.onSubmit(),
        boundForm,
        Seq(
          uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
            content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("Yes"),
            value = Some("true"),
            checked = boundForm("value").value.contains("true"),
            id = Some("value")
          ),
          uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem(
            content = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text("No"),
            value = Some("false"),
            checked = boundForm("value").value.contains("false"),
            id = Some("value-no")
          )
        ),
        ""
      )(request, messages)

      compareResultAndView(result, expectedView)

    }

  }
}
