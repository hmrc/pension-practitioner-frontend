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

import connectors.cache.UserAnswersCacheConnector
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.base.ControllerSpecBase
import matchers.JsonMatchers
import models.UserAnswers
import models.WhatTypeBusiness.{Companyorpartnership, Yourselfasindividual}
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Results.Ok
import org.mockito.Mockito.when
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.company.CompanyEmailPage
import pages.{PspIdPage, WhatTypeBusinessPage}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers.contentAsString
import views.html.deregister.individual.SuccessView
import play.api.inject.bind

import scala.concurrent.Future

class SuccessControllerSpec extends ControllerSpecBase with MockitoSugar
  with JsonMatchers with OptionValues with TryValues {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override lazy val app: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val pspId = "1234567890"
  private val email = "a@a.c"
  val userAnswers: UserAnswers = UserAnswers()
    .setOrException(WhatTypeBusinessPage, Yourselfasindividual)
    .setOrException(CompanyEmailPage, email)
    .setOrException(PspIdPage, pspId)

  private def onPageLoadUrl: String = routes.SuccessController.onPageLoad().url
  private def submitUrl: String = controllers.routes.SignOutController.signOut().url
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
    when(mockAppConfig.returnToPspDashboardUrl).thenReturn(submitUrl)
  }

  "Success Controller" must {
    "return OK and the correct view for a GET" in {
      when(mockUserAnswersCacheConnector.removeAll(any(), any())).thenReturn(Future.successful(Ok))

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[SuccessView]
      val fakeRequest = FakeRequest(GET, onPageLoadUrl)
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val messages: Messages = messagesApi.preferred(fakeRequest)
      contentAsString(result).removeAllNonces() mustEqual view(submitUrl)(fakeRequest, messages).toString
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(onPageLoadUrl)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
    }
  }
}