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
import controllers.deregister.individual.routes
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
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.annotations.AuthMustHaveEnrolmentWithNoIV
import utils.TwirlMigration

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
  private val mockTwirlMigration = mock[TwirlMigration]
  private val mockFrontendErrorHandler = mock[FrontendErrorHandler]
  private val minPsp = MinimalPSP("a@a.a", Some(pspName), None, rlsFlag = false, deceasedFlag = false)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  val userAnswers: UserAnswers = UserAnswers().set(PspNamePage, pspName).toOption.value
  override lazy val app: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def postRoute: Call = routes.ConfirmDeregistrationController.onSubmit()

  override def modules: Seq[GuiceableModule] = Seq(
    bind[MinimalConnector].toInstance(mockMinimalConnector),
    bind[DeregistrationConnector].toInstance(mockDeregistrationConnector),
    bind[AuthAction].qualifiedWith(classOf[AuthMustHaveEnrolmentWithNoIV]).to[FakeAuthAction],
    bind[FrontendAppConfig].toInstance(mockAppConfig),
    bind[UserAnswersCacheConnector].toInstance(mockUserAnswersCacheConnector),
    bind[CompoundNavigator].toInstance(mockCompoundNavigator),
    bind[TwirlMigration].toInstance(mockTwirlMigration),
    bind[FrontendErrorHandler].toInstance(mockFrontendErrorHandler)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any())).thenReturn(Future.successful(minPsp))
    when(mockDeregistrationConnector.canDeRegister(any())(any(), any())).thenReturn(Future.successful(true))
    when(mockUserAnswersCacheConnector.save(any())(any(), any())) thenReturn Future.successful(Json.obj())
    when(mockTwirlMigration.duoTemplate(any(), any())).thenReturn(Future.successful(Html("")))
    when(mockFrontendErrorHandler.onClientError(any(), any(), any())).thenReturn(Future.successful(Results.BadRequest))
  }

  "ConfirmDeregistrationController" must {
    "return OK and the correct view for a GET" in {
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
            checked = form("value").value.contains("true")
          ),
          RadioItem(
            content = Text("No"),
            value = Some("false"),
            checked = form("value").value.contains("false")
          )
        ),
        mockAppConfig.returnToPspDashboardUrl
      )(request, messages).toString

      verify(mockTwirlMigration, times(1)).duoTemplate(any(), any())
      contentAsString(result) mustEqual expectedView
    }

    "return BAD_REQUEST and the correct view for a GET" in {
      val result = route(app, request).value
      status(result) mustEqual BAD_REQUEST
    }
  }
}
