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

import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.PspNamePage
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Results.Ok

import scala.concurrent.Future

class SuccessControllerSpec
  extends ControllerSpecBase
    with MockitoSugar
    with JsonMatchers
    with OptionValues
    with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val pspName: String = "Psp name"
  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  val userAnswers: UserAnswers = UserAnswers().setOrException(PspNamePage, pspName)

  private def onPageLoadUrl: String = routes.SuccessController.onPageLoad().url

  private def submitUrl: String = controllers.routes.SignOutController.signOut().url

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockUserAnswersCacheConnector.removeAll(any(), any())).thenReturn(Future.successful(Ok))
  }

  "Success Controller" must {
    "return OK and the correct view for a GET" in {
      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val expectedView = application.injector.instanceOf[views.html.deregister.company.SuccessView]
      val fakeRequest = FakeRequest()
      val expectedHtml = expectedView(pspName, submitUrl)(fakeRequest, messages).toString

      contentAsString(result)
        .replaceAll("&amp;referrerUrl=%2F\\[.*?\\]", "&amp;referrerUrl=%2F[]")
        .removeAllNonces() contains expectedHtml
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}